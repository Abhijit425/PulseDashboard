package com.abhijit.pulse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

final class RamCard extends Card {
    private float pct;
    private final Paint track = Ui.stroke(1, 0x14FFFFFF), arc = Ui.stroke(1, Ui.CYAN), glow = Ui.stroke(1, Ui.CYAN),
            knob = Ui.fill(0xFFFFFFFF), tickMin = Ui.stroke(1, 0x14FFFFFF), tickMaj = Ui.stroke(1.4f, 0x33FFFFFF);
    private final Paint big = Ui.text(Ui.LIGHT, 40, Ui.TEXT), unit = Ui.text(Ui.LIGHT, 18, Ui.DIM),
            capP = Ui.caps(9.5f, Ui.DIM), lab = Ui.text(Ui.REG, 13, Ui.DIM), val = Ui.text(Ui.MED, 14, Ui.TEXT),
            dot = Ui.fill(0), sep = Ui.stroke(1, 0x0EFFFFFF), chartLab = Ui.caps(9, Ui.FAINT), chartVal = Ui.caps(9.5f, Ui.CYAN);
    private final Chart chart = new Chart(Ui.CYAN);
    private final RectF cr = new RectF();

    RamCard(Context c, Sys s) {
        super(c, s, "MEMORY", Ui.CYAN);
        capP.setTextAlign(Paint.Align.CENTER);
        chartVal.setTextAlign(Paint.Align.RIGHT);
        sep.setStrokeCap(Paint.Cap.BUTT);
        update();
    }

    @Override
    void update() {
        chip = sys.lowMem ? "LOW MEMORY" : sys.nominalRamGb() + " GB RAM";
        chipColor = sys.lowMem ? Ui.RED : Ui.CYAN;
    }

    @Override
    void drawContent(Canvas c, RectF r) {
        Sys s = sys;
        float target = s.memTotal > 0 ? (s.memTotal - s.memAvail) / (float) s.memTotal : 0;
        pct = ease(pct, target);

        float chartH = Math.min(Ui.dp(76), r.height() * 0.3f);
        float upperB = r.bottom - chartH - Ui.dp(20);
        float upperH = upperB - r.top;
        float dia = Math.min(upperH, r.width() * 0.46f);
        float cx = r.left + dia / 2f, cy = r.top + upperH / 2f;
        float st = dia * 0.075f;
        float rad = dia / 2f - st * 1.3f;
        int c2 = pct > 0.85f ? Ui.RED : Ui.VIOLET;
        Gfx.ticks(c, cx, cy, rad - st * 0.5f - Ui.dp(7), Ui.dp(3.5f), 48, false, tickMin, tickMaj);
        Gfx.ring(c, cx, cy, rad, st, pct, Ui.CYAN, c2, track, arc, glow, knob);

        float sz = dia * 0.25f;
        big.setTextSize(sz);
        unit.setTextSize(sz * 0.42f);
        String n = String.valueOf(Math.round(pct * 100));
        float nw = big.measureText(n), uw = unit.measureText("%");
        float g = Ui.dp(16);
        float base = cy - (0.72f * sz + g) / 2f + 0.72f * sz;
        float bx = cx - (nw + uw + Ui.dp(2)) / 2f;
        c.drawText(n, bx, base, big);
        c.drawText("%", bx + nw + Ui.dp(2), base, unit);
        c.drawText("IN USE", cx, base + g, capP);

        // right-hand stats
        float lx = r.left + dia + Ui.dp(24), rx = r.right;
        long used = s.memTotal - s.memAvail;
        String swap = s.swapTotal > 0 ? Ui.size(s.swapTotal - Math.max(0, s.swapFree)) + " / " + Ui.size(s.swapTotal) : "Off";
        String[] labels = {"Used", "Available", "Total", "Cached", "Swap"};
        String[] vals = {Ui.size(used), Ui.size(s.memAvail), Ui.size(s.memTotal), Ui.size(s.memCached), swap};
        int[] dots = {Ui.CYAN, Ui.MINT, 0, 0, 0};
        float rowH = upperH / labels.length;
        for (int i = 0; i < labels.length; i++) {
            float rc = r.top + rowH * (i + 0.5f);
            row(c, lx, rx, rc, labels[i], vals[i], dots[i], lab, val, dot);
            if (i < labels.length - 1) c.drawLine(lx, r.top + rowH * (i + 1), rx, r.top + rowH * (i + 1), sep);
        }

        cr.set(r.left, r.bottom - chartH, r.right, r.bottom);
        c.drawText("USAGE HISTORY", cr.left, cr.top - Ui.dp(7), chartLab);
        c.drawText(String.format(java.util.Locale.US, "%.1f%%", s.ramH.last()), cr.right, cr.top - Ui.dp(7), chartVal);
        chart.draw(c, cr, s.ramH, 0, 100, true);
    }
}
