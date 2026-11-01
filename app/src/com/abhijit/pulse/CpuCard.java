package com.abhijit.pulse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.Locale;

final class CpuCard extends Card {
    private float load;
    private float[] disp = new float[0];
    private final RectF o = new RectF(), cr = new RectF();
    private final Paint track = Ui.stroke(1, 0x14FFFFFF), arc = Ui.stroke(1, Ui.AMBER), glow = Ui.stroke(1, Ui.AMBER),
            knob = Ui.fill(0xFFFFFFFF), tickMin = Ui.stroke(1, 0x14FFFFFF), tickMaj = Ui.stroke(1.4f, 0x33FFFFFF),
            barBg = Ui.fill(0x0CFFFFFF), barFill = Ui.fill(0xFFFFFFFF), legendDot = Ui.fill(0);
    private final Paint big = Ui.text(Ui.LIGHT, 40, Ui.TEXT), unit = Ui.text(Ui.LIGHT, 18, Ui.DIM),
            capP = Ui.caps(9.5f, Ui.DIM), tempP = Ui.text(Ui.MED, 12, Ui.AMBER), idx = Ui.caps(8.5f, Ui.FAINT),
            freq = Ui.text(Ui.MED, 11, Ui.TEXT), legend = Ui.text(Ui.REG, 11.5f, Ui.DIM),
            chartLab = Ui.caps(9, Ui.FAINT), chartInfo = Ui.caps(9, Ui.FAINT);
    private final Chart chart = new Chart(Ui.AMBER);

    private static final int[] CLUSTER = {Ui.MINT, Ui.AMBER, Ui.PINK, Ui.CYAN, Ui.VIOLET};

    CpuCard(Context c, Sys s) {
        super(c, s, "PROCESSOR", Ui.AMBER);
        capP.setTextAlign(Paint.Align.CENTER);
        tempP.setTextAlign(Paint.Align.CENTER);
        idx.setTextAlign(Paint.Align.CENTER);
        freq.setTextAlign(Paint.Align.CENTER);
        chartInfo.setTextAlign(Paint.Align.RIGHT);
        update();
    }

    @Override
    void update() {
        String a = sys.abi.contains("arm64") ? "ARM64" : sys.abi.contains("armeabi") ? "ARM32"
                : sys.abi.contains("x86_64") ? "X86-64" : sys.abi.toUpperCase(Locale.US);
        chip = sys.cores + " CORES" + (a.length() > 0 ? " · " + a : "");
    }

    private int clusterColor(int i) {
        int n = sys.clusterCount;
        int c = sys.cluster[i];
        if (n == 1) return Ui.AMBER;
        if (n == 2) return c == 0 ? Ui.MINT : Ui.AMBER;
        if (n == 3) return c == 0 ? Ui.MINT : c == 1 ? Ui.AMBER : Ui.PINK;
        return CLUSTER[c % CLUSTER.length];
    }

    @Override
    void drawContent(Canvas c, RectF r) {
        Sys s = sys;
        int n = s.cores;
        if (disp.length != n) disp = new float[n];
        load = ease(load, s.cpuLoad);

        float chartH = Math.min(Ui.dp(76), r.height() * 0.3f);
        float upperB = r.bottom - chartH - Ui.dp(20);
        float upperH = upperB - r.top;
        float dia = Math.min(upperH, Math.min(Ui.dp(184), r.width() * 0.36f));
        float cx = r.left + dia / 2f, cy = r.top + upperH / 2f;
        float st = dia * 0.075f;
        float rad = dia / 2f - st * 1.3f;
        Gfx.ticks(c, cx, cy, rad - st * 0.5f - Ui.dp(7), Ui.dp(3.5f), 48, false, tickMin, tickMaj);
        Gfx.ring(c, cx, cy, rad, st, load, Ui.AMBER, load > 0.8f ? Ui.RED : Ui.PINK, track, arc, glow, knob);

        float sz = dia * 0.25f;
        big.setTextSize(sz);
        unit.setTextSize(sz * 0.42f);
        String num = s.cpuAvailable ? String.valueOf(Math.round(load * 100)) : "—";
        float nw = big.measureText(num), uw = unit.measureText("%");
        float g = Ui.dp(16);
        boolean hasTemp = !Float.isNaN(s.cpuTemp);
        float block = 0.72f * sz + g + (hasTemp ? Ui.dp(17) : 0);
        float base = cy - block / 2f + 0.72f * sz;
        float bx = cx - (nw + uw + Ui.dp(2)) / 2f;
        c.drawText(num, bx, base, big);
        c.drawText("%", bx + nw + Ui.dp(2), base, unit);
        c.drawText(s.cpuReal ? "CPU LOAD" : "FREQ LOAD", cx, base + g, capP);
        if (hasTemp) {
            tempP.setColor(s.cpuTemp >= 70 ? Ui.RED : s.cpuTemp >= 50 ? Ui.AMBER : Ui.MINT);
            c.drawText(String.format(Locale.US, "%.0f °C", s.cpuTemp), cx, base + g + Ui.dp(17), tempP);
        }

        // per-core bars
        float ax = r.left + dia + Ui.dp(30), aw = r.right - ax;
        float legendY = upperB - Ui.dp(4);
        float barsTop = r.top + Ui.dp(16), barsBottom = upperB - Ui.dp(44);
        float gap = Math.max(Ui.dp(5), Math.min(Ui.dp(14), aw / n * 0.32f));
        float bw = (aw - gap * (n - 1)) / n;
        float startX = ax;
        if (bw > Ui.dp(34)) {
            bw = Ui.dp(34);
            gap = n > 1 ? Math.min(Ui.dp(26), (aw - bw * n) / (n - 1)) : 0;
            startX = ax + (aw - (bw * n + gap * (n - 1))) / 2f;
        }
        boolean showFreq = bw + gap >= Ui.dp(28);
        float brad = Math.min(bw / 2f, Ui.dp(8));
        for (int i = 0; i < n; i++) {
            disp[i] = ease(disp[i], s.coreLoad[i]);
            float x = startX + i * (bw + gap);
            o.set(x, barsTop, x + bw, barsBottom);
            c.drawRoundRect(o, brad, brad, barBg);
            int col = clusterColor(i);
            if (s.coreOn[i]) {
                float h = Math.max(bw * 0.6f, (barsBottom - barsTop) * disp[i]);
                h = Math.min(h, barsBottom - barsTop);
                barFill.setShader(new LinearGradient(0, barsBottom, 0, barsBottom - h, Ui.a(col, 0.35f), col, Shader.TileMode.CLAMP));
                o.set(x, barsBottom - h, x + bw, barsBottom);
                c.drawRoundRect(o, brad, brad, barFill);
            }
            if (showFreq) {
                c.drawText(String.valueOf(i), x + bw / 2f, barsTop - Ui.dp(5), idx);
                freq.setColor(s.coreOn[i] ? Ui.TEXT : Ui.FAINT);
                String f = s.coreOn[i] ? String.format(Locale.US, "%.2f", s.fCur[i] / 1e6) : "OFF";
                c.drawText(f, x + bw / 2f, barsBottom + Ui.dp(16), freq);
            }
        }

        // legend
        String[] names;
        int cc = s.clusterCount;
        if (cc == 1) names = new String[]{"All cores"};
        else if (cc == 2) names = new String[]{"Efficiency", "Performance"};
        else if (cc == 3) names = new String[]{"Efficiency", "Performance", "Prime"};
        else {
            names = new String[cc];
            for (int k = 0; k < cc; k++) names[k] = "Cluster " + (k + 1);
        }
        float lx = ax;
        float ly = Ui.mid(legendY - Ui.dp(6), legend);
        for (int k = 0; k < names.length; k++) {
            int col = cc == 1 ? Ui.AMBER : cc == 2 ? (k == 0 ? Ui.MINT : Ui.AMBER)
                    : cc == 3 ? (k == 0 ? Ui.MINT : k == 1 ? Ui.AMBER : Ui.PINK) : CLUSTER[k % CLUSTER.length];
            float wNeed = Ui.dp(14) + legend.measureText(names[k]);
            if (lx + wNeed > r.right) break;
            legendDot.setColor(col);
            c.drawCircle(lx + Ui.dp(4), legendY - Ui.dp(6), Ui.dp(4), legendDot);
            c.drawText(names[k], lx + Ui.dp(13), ly, legend);
            lx += wNeed + Ui.dp(16);
        }
        if (showFreq) {
            legend.setTextAlign(Paint.Align.RIGHT);
            if (lx + legend.measureText("GHz") < r.right) c.drawText("GHz", r.right, ly, legend);
            legend.setTextAlign(Paint.Align.LEFT);
        }

        // history
        cr.set(r.left, r.bottom - chartH, r.right, r.bottom);
        c.drawText("LOAD HISTORY", cr.left, cr.top - Ui.dp(7), chartLab);
        String info = (s.hardware.length() > 0 ? s.hardware : "") + (s.governor.length() > 0 ? "  ·  " + s.governor : "");
        c.drawText(Ui.fit(info.toUpperCase(Locale.US), chartInfo, cr.width() - Ui.dp(120)), cr.right, cr.top - Ui.dp(7), chartInfo);
        chart.draw(c, cr, s.cpuH, 0, 100, true);
    }
}
