package com.abhijit.pulse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.Locale;

final class BatteryCard extends Card {
    private float lvl;
    private final RectF o = new RectF();
    private final Paint outline = Ui.stroke(2f, 0x66FFFFFF), nubP = Ui.fill(0x66FFFFFF), fillP = Ui.fill(0xFFFFFFFF),
            innerBg = Ui.fill(0x0DFFFFFF), boltP = Ui.fill(0xFFFFFFFF), glowP = Ui.fill(0);
    private final Paint big = Ui.text(Ui.LIGHT, 34, Ui.TEXT), unit = Ui.text(Ui.LIGHT, 16, Ui.DIM),
            status = Ui.text(Ui.MED, 13, Ui.MINT), power = Ui.text(Ui.REG, 12.5f, Ui.DIM),
            lab = Ui.text(Ui.REG, 13, Ui.DIM), val = Ui.text(Ui.MED, 13.5f, Ui.TEXT), dot = Ui.fill(0),
            sep = Ui.stroke(1, 0x0EFFFFFF);

    BatteryCard(Context c, Sys s) {
        super(c, s, "BATTERY", Ui.MINT);
        power.setTextAlign(Paint.Align.RIGHT);
        sep.setStrokeCap(Paint.Cap.BUTT);
        update();
    }

    @Override
    void update() {
        chip = sys.plugText();
        chipColor = sys.batPlug > 0 ? Ui.MINT : Ui.DIM;
    }

    private int levelColor(float l, boolean top) {
        if (l <= 0.2f) return top ? 0xFFFB7185 : Ui.RED;
        if (l <= 0.5f) return top ? Ui.AMBER : Ui.ORANGE;
        return top ? Ui.LIME : Ui.MINT;
    }

    @Override
    void drawContent(Canvas c, RectF r) {
        Sys s = sys;
        lvl = ease(lvl, s.batLevel < 0 ? 0 : s.batLevel / 100f);

        float bw = Math.min(r.width() * 0.5f, Ui.dp(150));
        float bh = bw * 0.5f, nub = Math.max(Ui.dp(4), bw * 0.05f);
        float bx = r.left + Ui.dp(1), by = r.top + Ui.dp(4);
        float rad = bh * 0.24f;

        o.set(bx, by, bx + bw, by + bh);
        c.drawRoundRect(o, rad, rad, outline);
        o.set(bx + bw + Ui.dp(3), by + bh * 0.32f, bx + bw + Ui.dp(3) + nub, by + bh * 0.68f);
        c.drawRoundRect(o, nub * 0.5f, nub * 0.5f, nubP);

        float ins = Ui.dp(5);
        float il = bx + ins, it = by + ins, ir = bx + bw - ins, ib = by + bh - ins;
        o.set(il, it, ir, ib);
        c.drawRoundRect(o, rad * 0.6f, rad * 0.6f, innerBg);
        if (lvl > 0.005f) {
            float fw = Math.max(rad, (ir - il) * lvl);
            int c1 = levelColor(lvl, false), c2 = levelColor(lvl, true);
            fillP.setShader(new LinearGradient(il, 0, ir, 0, c1, c2, Shader.TileMode.CLAMP));
            glowP.setColor(Ui.a(c1, 0.10f));
            o.set(bx - Ui.dp(6), by - Ui.dp(6), bx + bw + Ui.dp(6), by + bh + Ui.dp(6));
            c.drawRoundRect(o, rad * 1.5f, rad * 1.5f, glowP);
            o.set(il, it, il + fw, ib);
            c.drawRoundRect(o, rad * 0.6f, rad * 0.6f, fillP);
        }
        if (s.charging) {
            boltP.setColor(0xFFFFFFFF);
            Gfx.bolt(c, bx + bw / 2f, by + bh / 2f, bh * 0.62f, boltP);
        }

        // percentage
        float tx = bx + bw + nub + Ui.dp(18);
        float sz = Math.min(Ui.dp(36), bh * 0.62f);
        big.setTextSize(sz);
        unit.setTextSize(sz * 0.45f);
        String n = s.batLevel < 0 ? "—" : String.valueOf(Math.round(lvl * 100));
        float base = by + bh / 2f + sz * 0.36f;
        c.drawText(n, tx, base, big);
        c.drawText("%", tx + big.measureText(n) + Ui.dp(2), base, unit);

        // status line
        float sy = by + bh + Ui.dp(24);
        status.setColor(s.charging ? Ui.MINT : (s.batLevel >= 0 && s.batLevel <= 20 ? Ui.RED : Ui.TEXT));
        c.drawText(s.batStatusText(), r.left, Ui.mid(sy, status), status);
        if (s.batCurrentMa != Integer.MIN_VALUE && s.batVoltage > 0) {
            double w = Math.abs(s.batCurrentMa) / 1000.0 * s.batVoltage / 1000.0;
            c.drawText(String.format(Locale.US, "%.1f W", w), r.right, Ui.mid(sy, power), power);
        }

        // detail rows
        float ry = sy + Ui.dp(18);
        String temp = Float.isNaN(s.batTemp) ? "—" : String.format(Locale.US, "%.1f °C", s.batTemp);
        String volt = s.batVoltage > 0 ? String.format(Locale.US, "%.2f V", s.batVoltage / 1000f) : "—";
        String cur = s.batCurrentMa == Integer.MIN_VALUE ? "—"
                : (s.charging ? "+" : "−") + String.format(Locale.US, "%,d mA", Math.abs(s.batCurrentMa));
        String[] labels = {"Temperature", "Voltage", "Current", "Health", "Technology"};
        String[] vals = {temp, volt, cur, s.healthText(), s.batTech.length() == 0 ? "—" : s.batTech};
        int tColor = Float.isNaN(s.batTemp) ? 0 : s.batTemp >= 45 ? Ui.RED : s.batTemp >= 38 ? Ui.AMBER : Ui.MINT;
        int[] dots = {tColor, 0, 0, 0, 0};
        float rowH = (r.bottom - ry) / labels.length;
        for (int i = 0; i < labels.length; i++) {
            float rc = ry + rowH * (i + 0.5f);
            row(c, r.left, r.right, rc, labels[i], vals[i], dots[i], lab, val, dot);
            c.drawLine(r.left, ry + rowH * i, r.right, ry + rowH * i, sep);
        }
    }
}
