package com.abhijit.pulse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.os.SystemClock;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Top bar: brand mark, device name, live summary chips and a large clock. */
final class HeaderView extends View {
    private final Sys sys;
    private final String deviceName;
    private final boolean is24;
    private long lastTick;
    private final Paint ring = Ui.stroke(3.2f, Ui.CYAN), wave = Ui.stroke(2.2f, 0xFFFFFFFF), brand = Ui.text(Ui.MED, 24, Ui.TEXT),
            subP = Ui.text(Ui.REG, 13, Ui.DIM), clock = Ui.text(Ui.THIN, 50, Ui.TEXT), secP = Ui.text(Ui.LIGHT, 20, Ui.DIM),
            dateP = Ui.caps(10.5f, Ui.DIM), chipT = Ui.caps(10, Ui.TEXT), chipBg = Ui.fill(0), liveDot = Ui.fill(Ui.MINT),
            liveHalo = Ui.fill(0), chipBorder = Ui.stroke(1, 0x1AFFFFFF);
    private final Path wp = new Path();
    private final RectF o = new RectF();
    private final SimpleDateFormat hm, ss = new SimpleDateFormat("ss", Locale.US),
            date = new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()), ampm = new SimpleDateFormat("a", Locale.US);

    HeaderView(Context c, Sys sys, String deviceName) {
        super(c);
        this.sys = sys;
        this.deviceName = deviceName;
        is24 = android.text.format.DateFormat.is24HourFormat(c);
        hm = new SimpleDateFormat(is24 ? "HH:mm" : "h:mm", Locale.US);
        brand.setLetterSpacing(0.28f);
        clock.setTextAlign(Paint.Align.RIGHT);
        dateP.setTextAlign(Paint.Align.RIGHT);
    }

    void tick() {
        lastTick = SystemClock.uptimeMillis();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        brand.setShader(new LinearGradient(0, 0, Ui.dp(200), 0, Ui.CYAN, Ui.VIOLET, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float cy = h / 2f;
        boolean narrow = w < Ui.dp(600);

        // logo
        float lr = Ui.dp(24);
        float lx = Ui.dp(4) + lr;
        SweepGradient sg = new SweepGradient(lx, cy, new int[]{Ui.CYAN, Ui.VIOLET, Ui.PINK, Ui.CYAN}, null);
        ring.setShader(sg);
        c.drawCircle(lx, cy, lr - Ui.dp(2), ring);
        wp.reset();
        float s = lr * 0.62f;
        wp.moveTo(lx - s, cy);
        wp.lineTo(lx - s * 0.45f, cy);
        wp.lineTo(lx - s * 0.18f, cy - s * 0.62f);
        wp.lineTo(lx + s * 0.14f, cy + s * 0.62f);
        wp.lineTo(lx + s * 0.38f, cy - s * 0.2f);
        wp.lineTo(lx + s * 0.52f, cy);
        wp.lineTo(lx + s, cy);
        c.drawPath(wp, wave);

        // clock (right)
        Date now = new Date();
        float rx = w - Ui.dp(4);
        clock.setTextSize(Ui.dp(narrow ? 40 : 50));
        String sec = is24 ? ss.format(now) : ampm.format(now);
        float secW = secP.measureText(sec);
        float clockBase = cy + Ui.dp(narrow ? 6 : 8);
        c.drawText(sec, rx - secW, clockBase, secP);
        c.drawText(hm.format(now), rx - secW - Ui.dp(6), clockBase, clock);
        float clockW = clock.measureText(hm.format(now)) + secW + Ui.dp(6);
        c.drawText(date.format(now).toUpperCase(Locale.getDefault()), rx, clockBase + Ui.dp(22), dateP);
        float dateW = dateP.measureText(date.format(now).toUpperCase(Locale.getDefault()));
        float rightBlock = Math.max(clockW, dateW);

        // brand
        float tx = lx + lr + Ui.dp(16);
        c.drawText("PULSE", tx, cy - Ui.dp(2), brand);
        String sub = "System Dashboard  ·  " + deviceName;
        c.drawText(Ui.fit(sub, subP, w - tx - rightBlock - Ui.dp(24)), tx, cy + Ui.dp(19), subP);

        // live chips (only when there is room)
        float brandW = Math.max(brand.measureText("PULSE"), subP.measureText(sub));
        float chipsL = tx + Math.min(brandW, Ui.dp(330)) + Ui.dp(28);
        float chipsR = w - rightBlock - Ui.dp(28);
        String live = "LIVE · " + (sys.intervalMs < 1000 ? "0.5 S" : (sys.intervalMs / 1000) + " S");
        String[] chips = {live,
                "CPU " + (sys.cpuAvailable ? Math.round(sys.cpuLoad * 100) + "%" : "—"),
                "RAM " + (sys.memTotal > 0 ? Math.round(100f * (sys.memTotal - sys.memAvail) / sys.memTotal) + "%" : "—"),
                "BAT " + (sys.batLevel >= 0 ? sys.batLevel + "%" : "—")};
        float chH = Ui.dp(30);
        float total = 0;
        float[] cw = new float[chips.length];
        for (int i = 0; i < chips.length; i++) {
            cw[i] = chipT.measureText(chips[i]) + Ui.dp(i == 0 ? 42 : 28);
            total += cw[i] + (i > 0 ? Ui.dp(8) : 0);
        }
        if (!narrow && chipsR - chipsL >= cw[0]) {
            float x = Math.max(chipsL, (chipsL + chipsR - total) / 2f);
            if (x + total > chipsR) x = chipsL;
            for (int i = 0; i < chips.length; i++) {
                if (x + cw[i] > chipsR) break;
                o.set(x, cy - chH / 2f, x + cw[i], cy + chH / 2f);
                chipBg.setColor(0x0DFFFFFF);
                c.drawRoundRect(o, chH / 2f, chH / 2f, chipBg);
                c.drawRoundRect(o, chH / 2f, chH / 2f, chipBorder);
                float textX = x + Ui.dp(14);
                if (i == 0) {
                    float t = (SystemClock.uptimeMillis() - lastTick) / 700f;
                    float dx = x + Ui.dp(17);
                    if (t < 1f) {
                        liveHalo.setColor(Ui.a(Ui.MINT, 0.35f * (1f - t)));
                        c.drawCircle(dx, cy, Ui.dp(4) + Ui.dp(7) * t, liveHalo);
                        postInvalidateOnAnimation();
                    }
                    c.drawCircle(dx, cy, Ui.dp(4), liveDot);
                    textX = x + Ui.dp(28);
                }
                chipT.setColor(i == 0 ? Ui.MINT : Ui.TEXT);
                c.drawText(chips[i], textX, Ui.mid(cy, chipT), chipT);
                x += cw[i] + Ui.dp(8);
            }
        }
    }
}
