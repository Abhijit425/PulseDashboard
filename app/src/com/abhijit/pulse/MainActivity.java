package com.abhijit.pulse;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements ActionsCard.Listener {
    private static final int[] INTERVALS = {500, 1000, 2000};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Sys sys;
    private HeaderView header;
    private Card[] cards;
    private ActionsCard actions;
    private SharedPreferences prefs;
    private int interval;
    private boolean keepAwake, running;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            sys.intervalMs = interval;
            sys.sample();
            for (Card c : cards) {
                c.update();
                c.invalidate();
            }
            header.tick();
            handler.postDelayed(this, interval);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.init(this);
        prefs = getSharedPreferences("pulse", MODE_PRIVATE);
        interval = prefs.getInt("interval", 1000);
        keepAwake = prefs.getBoolean("awake", true);

        Window w = getWindow();
        w.setStatusBarColor(Ui.BG0);
        w.setNavigationBarColor(Ui.BG0);
        applyAwake();

        sys = new Sys(this);
        sys.intervalMs = interval;

        BgView root = new BgView(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        DashGrid grid = new DashGrid(this);

        String model = deviceName();
        header = new HeaderView(this, sys, model);
        RamCard ram = new RamCard(this, sys);
        StorageCard storage = new StorageCard(this, sys);
        BatteryCard battery = new BatteryCard(this, sys);
        CpuCard cpu = new CpuCard(this, sys);
        NetworkCard net = new NetworkCard(this, sys);
        DeviceCard device = new DeviceCard(this, sys, Build.VERSION.RELEASE);
        actions = new ActionsCard(this, sys);
        actions.listener = this;
        actions.keepAwake = keepAwake;
        actions.refreshLabel = label(interval);
        fillDevice(device, model);

        //                           spanW spanM  hW   hM   hN  orderW orderM
        grid.addView(header, new DashGrid.LP(12, 12, 96, 96, 92, 0, 0));
        grid.addView(ram, new DashGrid.LP(5, 6, 344, 344, 364, 1, 1));
        grid.addView(storage, new DashGrid.LP(4, 6, 344, 344, 364, 2, 2));
        grid.addView(battery, new DashGrid.LP(3, 6, 344, 330, 340, 3, 4));
        grid.addView(cpu, new DashGrid.LP(7, 12, 324, 316, 360, 4, 3));
        grid.addView(net, new DashGrid.LP(5, 6, 324, 330, 330, 5, 5));
        grid.addView(device, new DashGrid.LP(7, 12, 262, 252, 440, 6, 6));
        grid.addView(actions, new DashGrid.LP(5, 12, 262, 236, 330, 7, 7));
        cards = new Card[]{ram, storage, battery, cpu, net, device, actions};

        scroll.addView(grid, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);
        immersive();
    }

    @Override
    protected void onResume() {
        super.onResume();
        running = true;
        handler.removeCallbacks(tick);
        handler.post(tick);
        immersive();
    }

    @Override
    protected void onPause() {
        super.onPause();
        running = false;
        handler.removeCallbacks(tick);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) immersive();
    }

    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void applyAwake() {
        if (keepAwake) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private static String label(int ms) {
        return ms < 1000 ? "0.5 s" : (ms / 1000) + " s";
    }

    // ------------------------------------------------------------------ actions
    @Override
    public void onAction(int id) {
        switch (id) {
            case ActionsCard.BOOST:
                boost();
                break;
            case ActionsCard.AWAKE:
                keepAwake = !keepAwake;
                prefs.edit().putBoolean("awake", keepAwake).apply();
                applyAwake();
                actions.keepAwake = keepAwake;
                setStatus(keepAwake ? "Screen will stay on while the dashboard is open" : "Screen timeout restored", Ui.AMBER);
                break;
            case ActionsCard.REFRESH: {
                int idx = 0;
                for (int i = 0; i < INTERVALS.length; i++) if (INTERVALS[i] == interval) idx = i;
                interval = INTERVALS[(idx + 1) % INTERVALS.length];
                prefs.edit().putInt("interval", interval).apply();
                actions.refreshLabel = label(interval);
                sys.intervalMs = interval;
                setStatus("Sampling every " + label(interval), Ui.CYAN);
                break;
            }
            case ActionsCard.STORAGE:
                open(Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
                break;
            case ActionsCard.BATTERY:
                open(Intent.ACTION_POWER_USAGE_SUMMARY);
                break;
            case ActionsCard.APPS:
                open(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                break;
            default:
                break;
        }
        actions.invalidate();
        header.invalidate();
    }

    private void setStatus(String s, int color) {
        String t = new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
        actions.status = t + "  ·  " + s;
        actions.statusColor = color;
        actions.invalidate();
    }

    private void open(String action) {
        try {
            startActivity(new Intent(action));
        } catch (Throwable e) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Throwable ignored) {
                setStatus("Settings screen not available on this device", Ui.RED);
            }
        }
    }

    private void boost() {
        if (actions.boosting) return;
        actions.boosting = true;
        setStatus("Closing background processes…", Ui.PINK);
        final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final PackageManager pm = getPackageManager();
        final String self = getPackageName();
        final ActivityManager.MemoryInfo before = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(before);
        new Thread(new Runnable() {
            @Override
            public void run() {
                int n = 0;
                try {
                    List<ApplicationInfo> apps = pm.getInstalledApplications(0);
                    for (ApplicationInfo ai : apps) {
                        if (ai.packageName == null || ai.packageName.equals(self)) continue;
                        try {
                            am.killBackgroundProcesses(ai.packageName);
                            n++;
                        } catch (Throwable ignored) {
                        }
                    }
                } catch (Throwable ignored) {
                }
                Runtime.getRuntime().gc();
                try {
                    Thread.sleep(1400);
                } catch (InterruptedException ignored) {
                }
                final int count = n;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ActivityManager.MemoryInfo after = new ActivityManager.MemoryInfo();
                        am.getMemoryInfo(after);
                        long freed = after.availMem - before.availMem;
                        actions.boosting = false;
                        if (freed > 8L * 1024 * 1024) setStatus("Freed " + Ui.size(freed) + "  ·  " + count + " apps checked", Ui.MINT);
                        else setStatus("Memory already optimized  ·  " + count + " apps checked", Ui.MINT);
                    }
                });
            }
        }).start();
    }

    // ------------------------------------------------------------------ device info
    private static String deviceName() {
        String man = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER;
        String model = Build.MODEL == null ? "" : Build.MODEL;
        if (model.toLowerCase(Locale.US).startsWith(man.toLowerCase(Locale.US))) return Ui.cap(model);
        return Ui.cap(man) + " " + model;
    }

    private void fillDevice(DeviceCard d, String model) {
        ArrayList<String[]> l = new ArrayList<String[]>();
        l.add(new String[]{"MODEL", Build.MODEL == null ? model : Build.MODEL});
        l.add(new String[]{"ANDROID", Build.VERSION.RELEASE + "  ·  API " + Build.VERSION.SDK_INT});
        l.add(new String[]{"SECURITY PATCH", Build.VERSION.SECURITY_PATCH == null || Build.VERSION.SECURITY_PATCH.length() == 0 ? "—" : Build.VERSION.SECURITY_PATCH});
        l.add(new String[]{"CHIPSET", sys.hardware.length() == 0 ? "—" : sys.hardware});

        String res = "—", dens = "—", hz = "—";
        try {
            Display disp = getWindowManager().getDefaultDisplay();
            DisplayMetrics dm = new DisplayMetrics();
            disp.getRealMetrics(dm);
            res = Math.max(dm.widthPixels, dm.heightPixels) + " × " + Math.min(dm.widthPixels, dm.heightPixels);
            dens = dm.densityDpi + " dpi  ·  " + bucket(dm.densityDpi);
            float cur = disp.getRefreshRate(), max = cur;
            try {
                for (Display.Mode m : disp.getSupportedModes()) max = Math.max(max, m.getRefreshRate());
            } catch (Throwable ignored) {
            }
            hz = Math.round(cur) + " Hz" + (Math.round(max) > Math.round(cur) + 1 ? "  ·  max " + Math.round(max) : "");
        } catch (Throwable ignored) {
        }
        l.add(new String[]{"DISPLAY", res});
        l.add(new String[]{"DENSITY", dens});
        l.add(new String[]{"REFRESH RATE", hz});
        String k = System.getProperty("os.version", "—");
        int dash = k.indexOf('-');
        l.add(new String[]{"KERNEL", dash > 0 ? k.substring(0, dash) : k});
        d.uptimeIndex = l.size();
        l.add(new String[]{"UPTIME", Ui.duration(sys.uptime)});
        int sensors = 0;
        try {
            SensorManager sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            sensors = sm.getSensorList(android.hardware.Sensor.TYPE_ALL).size();
        } catch (Throwable ignored) {
        }
        l.add(new String[]{"SENSORS", sensors + " available"});
        l.add(new String[]{"ARCHITECTURE", sys.abi.length() == 0 ? "—" : sys.abi});
        l.add(new String[]{"BUILD", Build.DISPLAY == null ? "—" : Build.DISPLAY});
        d.items = l.toArray(new String[0][]);
    }

    private static String bucket(int dpi) {
        if (dpi <= 140) return "mdpi";
        if (dpi <= 200) return "hdpi";
        if (dpi <= 280) return "xhdpi";
        if (dpi <= 400) return "xxhdpi";
        return "xxxhdpi";
    }

    // ------------------------------------------------------------------ background
    static final class BgView extends FrameLayout {
        private final Paint base = new Paint(), b1 = new Paint(Paint.ANTI_ALIAS_FLAG), b2 = new Paint(Paint.ANTI_ALIAS_FLAG),
                b3 = new Paint(Paint.ANTI_ALIAS_FLAG), grid = new Paint();

        BgView(Context c) {
            super(c);
            setWillNotDraw(false);
            grid.setColor(0x07FFFFFF);
            grid.setStrokeWidth(Math.max(1f, Ui.dp(0.7f)));
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            base.setShader(new LinearGradient(0, 0, 0, h, Ui.BG0, Ui.BG1, Shader.TileMode.CLAMP));
            float m = Math.max(w, h);
            b1.setShader(new RadialGradient(w * 0.88f, h * 0.02f, m * 0.55f, Ui.a(Ui.VIOLET, 0.20f), 0, Shader.TileMode.CLAMP));
            b2.setShader(new RadialGradient(w * 0.04f, h * 0.55f, m * 0.5f, Ui.a(Ui.CYAN, 0.12f), 0, Shader.TileMode.CLAMP));
            b3.setShader(new RadialGradient(w * 0.62f, h * 1.02f, m * 0.5f, Ui.a(Ui.PINK, 0.10f), 0, Shader.TileMode.CLAMP));
        }

        @Override
        protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            c.drawRect(0, 0, w, h, base);
            float step = Ui.dp(44);
            for (float x = step; x < w; x += step) c.drawLine(x, 0, x, h, grid);
            for (float y = step; y < h; y += step) c.drawLine(0, y, w, y, grid);
            c.drawRect(0, 0, w, h, b1);
            c.drawRect(0, 0, w, h, b2);
            c.drawRect(0, 0, w, h, b3);
        }
    }
}
