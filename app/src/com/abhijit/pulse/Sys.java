package com.abhijit.pulse;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;

/** Live system sampler. All sampling happens on the UI thread (each pass takes a few ms). */
final class Sys {
    // memory
    long memTotal, memAvail, memThreshold, memCached = -1, swapTotal = -1, swapFree = -1;
    boolean lowMem;
    // storage
    long dataTotal, dataFree, sysTotal = -1, sysFree, sdTotal = -1, sdFree;
    // cpu
    int cores;
    long[] fMax, fMin, fCur;
    float[] coreLoad;
    boolean[] coreOn;
    int[] cluster;
    int clusterCount = 1;
    float cpuLoad;
    boolean cpuReal, cpuAvailable;
    float cpuTemp = Float.NaN;
    String hardware = "", governor = "", abi = "";
    // battery
    int batLevel = -1, batStatus, batPlug, batHealth, batVoltage;
    float batTemp = Float.NaN;
    String batTech = "";
    int batCurrentMa = Integer.MIN_VALUE;
    boolean charging;
    // network
    double rxRate, txRate;
    long rxTotal = -1, txTotal = -1;
    String netType = "—", ip = "—";
    boolean online;
    // misc
    long uptime;
    int intervalMs = 1000;

    final History ramH = new History(90), cpuH = new History(90), rxH = new History(90), txH = new History(90);

    private final Context ctx;
    private final ActivityManager am;
    private final ConnectivityManager cm;
    private final BatteryManager bm;
    private final IntentFilter batFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    private long lastRx = -1, lastTx = -1, lastNetT;
    private long lastCpuTotal = -1, lastCpuIdle;
    private boolean procStatOk = true, memInfoOk = true;
    private String[] thermal = new String[0];
    private int tick;

    Sys(Context c) {
        ctx = c.getApplicationContext();
        am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
        try { initCpu(); } catch (Throwable t) { if (cores <= 0) fallbackCpu(); }
        try { initThermal(); } catch (Throwable ignored) { }
        sample();
    }

    void sample() {
        try { mem(); } catch (Throwable ignored) { }
        if (tick % 5 == 0) try { storage(); } catch (Throwable ignored) { }
        try { cpu(); } catch (Throwable ignored) { }
        if (tick % 2 == 0) try { temp(); } catch (Throwable ignored) { }
        try { battery(); } catch (Throwable ignored) { }
        try { net(); } catch (Throwable ignored) { }
        if (tick % 10 == 0) try { ip(); } catch (Throwable ignored) { }
        uptime = SystemClock.elapsedRealtime();
        tick++;
    }

    // ------------------------------------------------------------------ memory
    private void mem() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        memTotal = mi.totalMem;
        memAvail = mi.availMem;
        memThreshold = mi.threshold;
        lowMem = mi.lowMemory;
        if (memInfoOk) {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader("/proc/meminfo"), 2048);
                String l;
                long cached = -1, buffers = 0, st = -1, sf = -1;
                while ((l = br.readLine()) != null) {
                    if (l.startsWith("Cached:")) cached = kb(l);
                    else if (l.startsWith("Buffers:")) buffers = kb(l);
                    else if (l.startsWith("SwapTotal:")) st = kb(l);
                    else if (l.startsWith("SwapFree:")) sf = kb(l);
                }
                memCached = cached >= 0 ? cached + Math.max(0, buffers) : -1;
                swapTotal = st;
                swapFree = sf;
            } catch (Throwable t) {
                memInfoOk = false;
            } finally {
                close(br);
            }
        }
        float used = memTotal > 0 ? 100f * (memTotal - memAvail) / memTotal : 0;
        ramH.add(used);
    }

    private static long kb(String line) {
        long v = 0;
        boolean any = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch >= '0' && ch <= '9') {
                v = v * 10 + (ch - '0');
                any = true;
            } else if (any) break;
        }
        return any ? v * 1024L : -1;
    }

    // ------------------------------------------------------------------ storage
    private void storage() {
        StatFs d = new StatFs(Environment.getDataDirectory().getPath());
        dataTotal = d.getTotalBytes();
        dataFree = d.getAvailableBytes();
        try {
            StatFs s = new StatFs(Environment.getRootDirectory().getPath());
            sysTotal = s.getTotalBytes();
            sysFree = s.getAvailableBytes();
        } catch (Throwable t) {
            sysTotal = -1;
        }
        long sdT = -1, sdF = 0;
        try {
            File[] dirs = ctx.getExternalFilesDirs(null);
            for (int k = 1; dirs != null && k < dirs.length; k++) {
                File f = dirs[k];
                if (f == null) continue;
                try {
                    if (Environment.isExternalStorageRemovable(f)) {
                        StatFs st = new StatFs(f.getPath());
                        sdT = st.getTotalBytes();
                        sdF = st.getAvailableBytes();
                        break;
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        sdTotal = sdT;
        sdFree = sdF;
    }

    /** Marketing capacity (8, 16, 32 ... GB) estimated from partition sizes. */
    int nominalStorageGb() {
        double gb = (dataTotal + Math.max(0, sysTotal)) / 1e9;
        int n = 8;
        while (n < gb && n < 8192) n *= 2;
        return n;
    }

    int nominalRamGb() {
        return (int) Math.max(1, Math.ceil(memTotal / 1073741824.0));
    }

    // ------------------------------------------------------------------ cpu
    private static final String CPU = "/sys/devices/system/cpu/cpu";

    private void fallbackCpu() {
        cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        allocCpu();
    }

    private void allocCpu() {
        fMax = new long[cores];
        fMin = new long[cores];
        fCur = new long[cores];
        coreLoad = new float[cores];
        coreOn = new boolean[cores];
        cluster = new int[cores];
    }

    private void initCpu() {
        int n = 0;
        String poss = read("/sys/devices/system/cpu/possible");
        if (poss != null) {
            try {
                int cut = Math.max(poss.lastIndexOf('-'), poss.lastIndexOf(','));
                n = Integer.parseInt(poss.substring(cut + 1).trim()) + 1;
            } catch (Exception ignored) {
            }
        }
        if (n <= 0 || n > 64) n = Runtime.getRuntime().availableProcessors();
        cores = Math.max(1, n);
        allocCpu();
        for (int i = 0; i < cores; i++) {
            fMax[i] = readLong(CPU + i + "/cpufreq/cpuinfo_max_freq");
            fMin[i] = readLong(CPU + i + "/cpufreq/cpuinfo_min_freq");
        }
        computeClusters();

        String[] abis = Build.SUPPORTED_ABIS;
        abi = abis != null && abis.length > 0 ? abis[0] : "";

        String hw = "";
        try {
            Object soc = Build.class.getField("SOC_MODEL").get(null);
            Object man = Build.class.getField("SOC_MANUFACTURER").get(null);
            if (soc != null && !"unknown".equalsIgnoreCase(soc.toString()) && soc.toString().length() > 0) {
                hw = (man != null && !"unknown".equalsIgnoreCase(man.toString()) ? man + " " : "") + soc;
            }
        } catch (Throwable ignored) {
        }
        if (hw.length() == 0) {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader("/proc/cpuinfo"), 4096);
                String l;
                while ((l = br.readLine()) != null) {
                    if (l.startsWith("Hardware")) {
                        int c = l.indexOf(':');
                        if (c > 0) hw = l.substring(c + 1).trim();
                        break;
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                close(br);
            }
        }
        if (hw.length() == 0) hw = Build.HARDWARE;
        hardware = hw;
        String g = read(CPU + "0/cpufreq/scaling_governor");
        governor = g == null ? "" : g;
    }

    private void computeClusters() {
        ArrayList<Long> uniq = new ArrayList<Long>();
        for (int i = 0; i < cores; i++) if (fMax[i] > 0 && !uniq.contains(fMax[i])) uniq.add(fMax[i]);
        Long[] u = uniq.toArray(new Long[0]);
        Arrays.sort(u);
        clusterCount = Math.max(1, u.length);
        for (int i = 0; i < cores; i++) {
            int c = 0;
            for (int k = 0; k < u.length; k++) if (u[k] == fMax[i]) c = k;
            cluster[i] = c;
        }
    }

    private void cpu() {
        float sum = 0;
        boolean any = false, refetch = false;
        for (int i = 0; i < cores; i++) {
            long cur = readLong(CPU + i + "/cpufreq/scaling_cur_freq");
            if (fMax[i] <= 0 && cur > 0) {
                fMax[i] = readLong(CPU + i + "/cpufreq/cpuinfo_max_freq");
                fMin[i] = readLong(CPU + i + "/cpufreq/cpuinfo_min_freq");
                refetch = true;
            }
            fCur[i] = Math.max(0, cur);
            coreOn[i] = cur > 0;
            float l = 0;
            if (cur > 0) {
                any = true;
                if (fMax[i] > 0 && fMin[i] >= 0 && fMax[i] > fMin[i]) l = (cur - fMin[i]) / (float) (fMax[i] - fMin[i]);
                else if (fMax[i] > 0) l = cur / (float) fMax[i];
                l = 0.04f + 0.96f * Ui.clamp(l);
            }
            coreLoad[i] = Ui.clamp(l);
            sum += coreLoad[i];
        }
        if (refetch) computeClusters();
        float est = sum / cores;

        float real = -1;
        if (procStatOk) {
            String l = read("/proc/stat");
            try {
                if (l == null || !l.startsWith("cpu")) throw new IllegalStateException();
                String[] t = l.trim().split("\\s+");
                long total = 0, idle = 0;
                for (int k = 1; k < t.length && k <= 8; k++) {
                    long v = Long.parseLong(t[k]);
                    total += v;
                    if (k == 4 || k == 5) idle += v;
                }
                if (lastCpuTotal >= 0 && total > lastCpuTotal) {
                    real = 1f - (idle - lastCpuIdle) / (float) (total - lastCpuTotal);
                }
                lastCpuTotal = total;
                lastCpuIdle = idle;
            } catch (Throwable e) {
                procStatOk = false;
            }
        }
        cpuReal = real >= 0;
        cpuAvailable = cpuReal || any;
        cpuLoad = Ui.clamp(cpuReal ? real : est);
        cpuH.add(cpuLoad * 100f);
    }

    private void initThermal() {
        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < 80 && list.size() < 12; i++) {
            String type = read("/sys/class/thermal/thermal_zone" + i + "/type");
            if (type == null) continue;
            String t = type.toLowerCase(Locale.US);
            if (t.contains("cpu") || t.contains("soc") || t.contains("tsens") || t.contains("cluster")
                    || t.contains("mtktscpu") || t.equals("ap") || t.contains("apss") || t.contains("big")
                    || t.contains("little") || t.contains("core")) {
                list.add("/sys/class/thermal/thermal_zone" + i + "/temp");
            }
        }
        thermal = list.toArray(new String[0]);
    }

    private void temp() {
        float best = Float.NaN;
        for (String p : thermal) {
            long v = readLong(p);
            if (v == -1) continue;
            float c = v > 1000 ? v / 1000f : (v > 200 ? v / 10f : v);
            if (c > 5 && c < 130 && (Float.isNaN(best) || c > best)) best = c;
        }
        cpuTemp = best;
    }

    // ------------------------------------------------------------------ battery
    private void battery() {
        Intent i = ctx.registerReceiver(null, batFilter);
        if (i == null) return;
        int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        batLevel = level >= 0 && scale > 0 ? Math.round(level * 100f / scale) : -1;
        batStatus = i.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        batPlug = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        charging = batStatus == BatteryManager.BATTERY_STATUS_CHARGING
                || (batStatus == BatteryManager.BATTERY_STATUS_FULL && batPlug > 0);
        int t = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
        batTemp = t == Integer.MIN_VALUE ? Float.NaN : t / 10f;
        int v = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
        batVoltage = v > 0 && v < 100 ? v * 1000 : v;
        batHealth = i.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN);
        String tech = i.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
        batTech = tech == null ? "" : tech;
        if (bm != null) {
            int cur = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
            if (cur == Integer.MIN_VALUE || cur == 0 || cur == Integer.MAX_VALUE) batCurrentMa = Integer.MIN_VALUE;
            else batCurrentMa = Math.abs(cur) > 10000 ? cur / 1000 : cur;
        }
    }

    String batStatusText() {
        switch (batStatus) {
            case BatteryManager.BATTERY_STATUS_CHARGING: return "Charging";
            case BatteryManager.BATTERY_STATUS_FULL: return batPlug > 0 ? "Fully charged" : "Full";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING: return "Not charging";
            case BatteryManager.BATTERY_STATUS_DISCHARGING: return "On battery";
            default: return "Unknown";
        }
    }

    String plugText() {
        switch (batPlug) {
            case BatteryManager.BATTERY_PLUGGED_AC: return "AC";
            case BatteryManager.BATTERY_PLUGGED_USB: return "USB";
            case BatteryManager.BATTERY_PLUGGED_WIRELESS: return "WIRELESS";
            default: return batPlug > 0 ? "DOCK" : "UNPLUGGED";
        }
    }

    String healthText() {
        switch (batHealth) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return "Good";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "Overheat";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "Dead";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "Over voltage";
            case BatteryManager.BATTERY_HEALTH_COLD: return "Cold";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE: return "Failure";
            default: return "Unknown";
        }
    }

    // ------------------------------------------------------------------ network
    private void net() {
        long rx = TrafficStats.getTotalRxBytes(), tx = TrafficStats.getTotalTxBytes();
        long now = SystemClock.elapsedRealtime();
        if (rx != TrafficStats.UNSUPPORTED && tx != TrafficStats.UNSUPPORTED) {
            if (lastRx >= 0 && now > lastNetT) {
                double dt = (now - lastNetT) / 1000.0;
                rxRate = Math.max(0, (rx - lastRx) / dt);
                txRate = Math.max(0, (tx - lastTx) / dt);
            }
            lastRx = rx;
            lastTx = tx;
            lastNetT = now;
            rxTotal = rx;
            txTotal = tx;
        }
        rxH.add((float) rxRate);
        txH.add((float) txRate);

        NetworkInfo ni = cm == null ? null : cm.getActiveNetworkInfo();
        online = ni != null && ni.isConnected();
        if (!online) netType = "Offline";
        else {
            switch (ni.getType()) {
                case ConnectivityManager.TYPE_WIFI: netType = "Wi-Fi"; break;
                case ConnectivityManager.TYPE_ETHERNET: netType = "Ethernet"; break;
                case ConnectivityManager.TYPE_VPN: netType = "VPN"; break;
                case ConnectivityManager.TYPE_BLUETOOTH: netType = "Bluetooth"; break;
                case ConnectivityManager.TYPE_MOBILE:
                    String sub = ni.getSubtypeName();
                    netType = sub != null && sub.length() > 0 ? "Mobile · " + sub : "Mobile";
                    break;
                default: netType = ni.getTypeName();
            }
        }
    }

    private void ip() throws Exception {
        String best = null;
        int bestRank = 99;
        Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
        while (en != null && en.hasMoreElements()) {
            NetworkInterface ni = en.nextElement();
            try {
                if (!ni.isUp() || ni.isLoopback()) continue;
            } catch (Throwable t) {
                continue;
            }
            String name = ni.getName() == null ? "" : ni.getName();
            int rank = name.startsWith("wlan") ? 0 : name.startsWith("eth") ? 1 : name.startsWith("rmnet") || name.startsWith("ccmni") ? 2 : 3;
            Enumeration<InetAddress> ads = ni.getInetAddresses();
            while (ads.hasMoreElements()) {
                InetAddress a = ads.nextElement();
                if (a instanceof Inet4Address && !a.isLoopbackAddress() && rank < bestRank) {
                    best = a.getHostAddress();
                    bestRank = rank;
                }
            }
        }
        ip = best == null ? "—" : best;
    }

    // ------------------------------------------------------------------ io helpers
    static String read(String path) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(path), 256);
            String l = br.readLine();
            return l == null ? null : l.trim();
        } catch (Throwable e) {
            return null;
        } finally {
            close(br);
        }
    }

    static long readLong(String path) {
        String s = read(path);
        if (s == null || s.length() == 0) return -1;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void close(BufferedReader br) {
        if (br != null) try { br.close(); } catch (Throwable ignored) { }
    }
}
