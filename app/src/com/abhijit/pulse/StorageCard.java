package com.abhijit.pulse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

final class StorageCard extends Card {
    private float frac, sysFrac, sdFrac;
    private final Paint track = Ui.stroke(1, 0x14FFFFFF), arc = Ui.stroke(1, Ui.VIOLET), glow = Ui.stroke(1, Ui.VIOLET),
            knob = Ui.fill(0xFFFFFFFF), tickMin = Ui.stroke(1, 0x16FFFFFF), tickMaj = Ui.stroke(1.4f, 0x38FFFFFF),
            barTrack = Ui.fill(0x12FFFFFF), barFill = Ui.fill(0xFFFFFFFF);
    private final Paint big = Ui.text(Ui.LIGHT, 40, Ui.TEXT), unit = Ui.text(Ui.LIGHT, 18, Ui.DIM),
            sub = Ui.text(Ui.REG, 12.5f, Ui.DIM), capP = Ui.caps(9, Ui.DIM), statV = Ui.text(Ui.MED, 15, Ui.TEXT),
            lab = Ui.text(Ui.REG, 13, Ui.DIM), val = Ui.text(Ui.MED, 13, Ui.TEXT);

    StorageCard(Context c, Sys s) {
        super(c, s, "STORAGE", Ui.VIOLET);
        sub.setTextAlign(Paint.Align.CENTER);
        capP.setTextAlign(Paint.Align.CENTER);
        statV.setTextAlign(Paint.Align.CENTER);
        val.setTextAlign(Paint.Align.RIGHT);
        update();
    }

    @Override
    void update() {
        chip = sys.dataTotal > 0 ? sys.nominalStorageGb() + " GB" : "";
    }

    @Override
    void drawContent(Canvas c, RectF r) {
        Sys s = sys;
        frac = ease(frac, s.dataTotal > 0 ? 1f - s.dataFree / (float) s.dataTotal : 0);
        sysFrac = ease(sysFrac, s.sysTotal > 0 ? 1f - s.sysFree / (float) s.sysTotal : 0);
        sdFrac = ease(sdFrac, s.sdTotal > 0 ? 1f - s.sdFree / (float) s.sdTotal : 0);

        float block = Ui.dp(44);
        float statH = Ui.dp(50);
        float barsTop = r.bottom - block * 2;
        float statTop = barsTop - statH - Ui.dp(4);
        float arcArea = statTop - r.top - Ui.dp(6);

        float maxR = Math.min(arcArea, r.width() / 2f - Ui.dp(2));
        float st = maxR * 0.11f, m = Ui.dp(11);
        float R = maxR - st / 2f - m;
        float cx = r.centerX(), cy = r.top + maxR;
        Gfx.ticks(c, cx, cy, R + st / 2f + m - Ui.dp(2), Ui.dp(3.5f), 20, true, tickMin, tickMaj);
        int c2 = frac > 0.9f ? Ui.RED : Ui.PINK;
        Gfx.semi(c, cx, cy, R, st, frac, Ui.VIOLET, c2, track, arc, glow, knob);

        float sz = R * 0.40f;
        big.setTextSize(sz);
        unit.setTextSize(sz * 0.42f);
        String n = String.valueOf(Math.round(frac * 100));
        float nw = big.measureText(n), uw = unit.measureText("%");
        float base = cy - R * 0.2f;
        float bx = cx - (nw + uw + Ui.dp(2)) / 2f;
        c.drawText(n, bx, base, big);
        c.drawText("%", bx + nw + Ui.dp(2), base, unit);
        c.drawText(Ui.size(s.dataFree) + " free", cx, cy + Ui.dp(4), sub);

        // used / free / total row
        String[] cl = {"USED", "FREE", "TOTAL"};
        String[] cv = {Ui.size(s.dataTotal - s.dataFree), Ui.size(s.dataFree), Ui.size(s.dataTotal)};
        float colW = r.width() / 3f;
        for (int i = 0; i < 3; i++) {
            float x = r.left + colW * (i + 0.5f);
            c.drawText(cl[i], x, statTop + Ui.dp(16), capP);
            c.drawText(Ui.fit(cv[i], statV, colW - Ui.dp(6)), x, statTop + Ui.dp(38), statV);
        }

        // partitions
        drawBar(c, r, barsTop, "System", s.sysTotal > 0 ? Ui.size(s.sysTotal - s.sysFree) + " / " + Ui.size(s.sysTotal) : "—",
                sysFrac, Ui.AMBER, Ui.ORANGE, s.sysTotal > 0);
        drawBar(c, r, barsTop + block, "SD card", s.sdTotal > 0 ? Ui.size(s.sdTotal - s.sdFree) + " / " + Ui.size(s.sdTotal) : "Not inserted",
                sdFrac, Ui.MINT, Ui.CYAN, s.sdTotal > 0);
    }

    private void drawBar(Canvas c, RectF r, float top, String label, String value, float f, int c1, int c2, boolean on) {
        float lc = top + Ui.dp(12);
        val.setColor(on ? Ui.TEXT : Ui.FAINT);
        row(c, r.left, r.right, lc, label, value, 0, lab, val, barFill);
        Gfx.bar(c, r.left, top + Ui.dp(25), r.right, top + Ui.dp(32), on ? f : 0, c1, c2, barTrack, barFill);
    }
}
