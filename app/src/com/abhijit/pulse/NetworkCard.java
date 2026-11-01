package com.abhijit.pulse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

final class NetworkCard extends Card {
    private float scale = 64 * 1024f;
    private float dRx, dTx;
    private final RectF cr = new RectF();
    private final Chart rxChart = new Chart(Ui.CYAN), txChart = new Chart(Ui.PINK);
    private final Paint iconBg = Ui.fill(0), iconSt = Ui.stroke(2f, Ui.CYAN), iconFl = Ui.fill(Ui.CYAN);
    private final Paint capP = Ui.caps(9.5f, Ui.DIM), big = Ui.text(Ui.LIGHT, 28, Ui.TEXT), unit = Ui.text(Ui.REG, 12.5f, Ui.DIM),
            chartLab = Ui.caps(9, Ui.FAINT), chartInfo = Ui.caps(9, Ui.FAINT), footV = Ui.text(Ui.MED, 13, Ui.TEXT),
            sep = Ui.stroke(1, 0x0EFFFFFF);

    NetworkCard(Context c, Sys s) {
        super(c, s, "NETWORK", Ui.CYAN);
        chartInfo.setTextAlign(Paint.Align.RIGHT);
        sep.setStrokeCap(Paint.Cap.BUTT);
        update();
    }

    @Override
    void update() {
        chip = sys.online ? "ONLINE" : "OFFLINE";
        chipColor = sys.online ? Ui.MINT : Ui.RED;
    }

    @Override
    void drawContent(Canvas c, RectF r) {
        Sys s = sys;
        dRx = ease(dRx, (float) s.rxRate);
        dTx = ease(dTx, (float) s.txRate);
        float target = Math.max(32 * 1024f, Math.max(s.rxH.max(), s.txH.max()) * 1.2f);
        scale = ease(scale, target);

        float topH = Ui.dp(56);
        float half = r.width() / 2f;
        speed(c, r.left, r.top, half, "DOWNLOAD", dRx, Ui.CYAN, Gfx.IC_DOWN);
        speed(c, r.left + half, r.top, half, "UPLOAD", dTx, Ui.PINK, Gfx.IC_UP);

        float footH = Ui.dp(46);
        float footTop = r.bottom - footH;
        cr.set(r.left, r.top + topH + Ui.dp(26), r.right, footTop - Ui.dp(14));
        c.drawText("THROUGHPUT", cr.left, cr.top - Ui.dp(7), chartLab);
        c.drawText("SCALE " + Ui.rate(scale), cr.right, cr.top - Ui.dp(7), chartInfo);
        rxChart.draw(c, cr, s.rxH, 0, scale, true);
        txChart.draw(c, cr, s.txH, 0, scale, false);

        c.drawLine(r.left, footTop, r.right, footTop, sep);
        String[] fl = {"CONNECTION", "IP ADDRESS", "SINCE BOOT"};
        String[] fv = {s.netType, s.ip, s.rxTotal >= 0 ? "" : "—"};
        float[] w = {0.30f, 0.32f, 0.38f};
        float x = r.left;
        for (int i = 0; i < 3; i++) {
            float cw = r.width() * w[i];
            c.drawText(fl[i], x, footTop + Ui.dp(18), capP);
            if (i == 2 && s.rxTotal >= 0) {
                float vy = footTop + Ui.dp(38), ay = vy - Ui.dp(4.5f), sz = Ui.dp(12);
                float px = x;
                iconSt.setColor(Ui.CYAN);
                Gfx.icon(c, Gfx.IC_DOWN, px + sz / 2f, ay, sz, iconSt, iconFl);
                String d = Ui.size(s.rxTotal);
                c.drawText(d, px + sz + Ui.dp(4), vy, footV);
                px += sz + Ui.dp(4) + footV.measureText(d) + Ui.dp(10);
                iconSt.setColor(Ui.PINK);
                Gfx.icon(c, Gfx.IC_UP, px + sz / 2f, ay, sz, iconSt, iconFl);
                c.drawText(Ui.fit(Ui.size(s.txTotal), footV, x + cw - px - sz - Ui.dp(4)), px + sz + Ui.dp(4), vy, footV);
            } else {
                c.drawText(Ui.fit(fv[i], footV, cw - Ui.dp(10)), x, footTop + Ui.dp(38), footV);
            }
            x += cw;
        }
    }

    private void speed(Canvas c, float l, float t, float w, String label, float bps, int col, int icon) {
        float ic = Ui.dp(40);
        float cy = t + ic / 2f + Ui.dp(4);
        iconBg.setColor(Ui.a(col, 0.13f));
        c.drawCircle(l + ic / 2f, cy, ic / 2f, iconBg);
        iconSt.setColor(col);
        iconFl.setColor(col);
        Gfx.icon(c, icon, l + ic / 2f, cy, ic * 0.6f, iconSt, iconFl);
        float tx = l + ic + Ui.dp(12);
        c.drawText(label, tx, cy - Ui.dp(9), capP);
        String[] p = Ui.rateParts(bps);
        float maxW = w - ic - Ui.dp(20);
        big.setTextSize(Ui.dp(28));
        float need = big.measureText(p[0]) + Ui.dp(5) + unit.measureText(p[1]);
        if (need > maxW && need > 0) big.setTextSize(Math.max(Ui.dp(16), Ui.dp(28) * (maxW - unit.measureText(p[1]) - Ui.dp(5))
                / Math.max(1f, big.measureText(p[0]))));
        float base = cy + Ui.dp(19);
        c.drawText(p[0], tx, base, big);
        c.drawText(p[1], tx + big.measureText(p[0]) + Ui.dp(5), base, unit);
    }
}
