package com.abhijit.pulse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/** Base glass card: background, accent glow, title row, status chip and easing helper. */
abstract class Card extends View {
    final String title;
    final int accent;
    String chip;
    int chipColor;
    protected final Sys sys;
    protected boolean animating;
    protected final float pad = Ui.dp(20);
    protected final RectF content = new RectF();

    private final Paint bgP = new Paint(Paint.ANTI_ALIAS_FLAG), glowP = new Paint(Paint.ANTI_ALIAS_FLAG),
            borderP = Ui.stroke(1f, 0x22FFFFFF), titleP = Ui.caps(11f, Ui.DIM), chipP = Ui.caps(9.5f, Ui.TEXT),
            chipBg = Ui.fill(0), dotP, haloP;
    private final RectF box = new RectF();

    Card(Context c, Sys sys, String title, int accent) {
        super(c);
        this.sys = sys;
        this.title = title;
        this.accent = accent;
        this.chipColor = accent;
        dotP = Ui.fill(accent);
        haloP = Ui.fill(Ui.a(accent, 0.22f));
    }

    /** Called once per sampling tick, before invalidate(). */
    void update() {
    }

    abstract void drawContent(Canvas c, RectF r);

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        bgP.setShader(new LinearGradient(0, 0, 0, h, 0xF2111A2E, 0xF20A1120, Shader.TileMode.CLAMP));
        glowP.setShader(new RadialGradient(w * 0.08f, 0, Math.max(w, h) * 0.8f,
                Ui.a(accent, 0.11f), Ui.a(accent, 0f), Shader.TileMode.CLAMP));
        borderP.setShader(new LinearGradient(0, 0, 0, h, 0x2EFFFFFF, 0x0AFFFFFF, Shader.TileMode.CLAMP));
        borderP.setStrokeWidth(Math.max(1f, Ui.dp(1f)));
    }

    @Override
    protected void onDraw(Canvas c) {
        animating = false;
        float w = getWidth(), h = getHeight(), rad = Ui.dp(22), half = Ui.dp(0.5f);
        box.set(half, half, w - half, h - half);
        c.drawRoundRect(box, rad, rad, bgP);
        c.drawRoundRect(box, rad, rad, glowP);
        c.drawRoundRect(box, rad, rad, borderP);

        float cy = pad + Ui.dp(7);
        c.drawCircle(pad + Ui.dp(4), cy, Ui.dp(8), haloP);
        c.drawCircle(pad + Ui.dp(4), cy, Ui.dp(3.6f), dotP);
        c.drawText(title, pad + Ui.dp(19), Ui.mid(cy, titleP), titleP);

        if (chip != null && chip.length() > 0) {
            float tw = chipP.measureText(chip);
            float ph = Ui.dp(24), right = w - pad + Ui.dp(4);
            chipP.setColor(chipColor);
            Gfx.pill(c, right - tw - Ui.dp(22), cy - ph / 2f, right, cy + ph / 2f, Ui.a(chipColor, 0.13f), chipBg, chip, chipP);
        }
        content.set(pad, pad + Ui.dp(36), w - pad, h - pad);
        drawContent(c, content);
        if (animating) postInvalidateOnAnimation();
    }

    protected float ease(float cur, float tgt) {
        float d = tgt - cur;
        if (Math.abs(d) <= 0.0004f + Math.abs(tgt) * 0.0005f) return tgt;
        animating = true;
        return cur + d * 0.16f;
    }

    /** Label (left) / value (right) row with optional colour dot. */
    protected void row(Canvas c, float l, float r, float cy, String label, String value, int dotColor,
                       Paint lab, Paint val, Paint dot) {
        float tx = l;
        if (dotColor != 0) {
            dot.setColor(dotColor);
            c.drawCircle(l + Ui.dp(3.5f), cy, Ui.dp(3.5f), dot);
            tx += Ui.dp(14);
        }
        float lw = lab.measureText(label);
        c.drawText(label, tx, Ui.mid(cy, lab), lab);
        Paint.Align al = val.getTextAlign();
        val.setTextAlign(Paint.Align.RIGHT);
        c.drawText(Ui.fit(value, val, r - tx - lw - Ui.dp(10)), r, Ui.mid(cy, val), val);
        val.setTextAlign(al);
    }
}
