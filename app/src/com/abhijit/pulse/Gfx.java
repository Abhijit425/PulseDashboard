package com.abhijit.pulse;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;

/** Vector drawing primitives shared by all cards. */
final class Gfx {
    private Gfx() {}

    private static final Matrix M = new Matrix();
    private static final RectF O = new RectF();
    private static final Path P = new Path();

    /** Full ring gauge starting at 12 o'clock. */
    static void ring(Canvas c, float cx, float cy, float r, float st, float frac, int c1, int c2,
                     Paint track, Paint arc, Paint glow, Paint knob) {
        track.setStrokeWidth(st);
        c.drawCircle(cx, cy, r, track);
        frac = Ui.clamp(frac);
        if (frac < 0.002f) return;
        O.set(cx - r, cy - r, cx + r, cy + r);
        float capDeg = (float) Math.toDegrees((st / 2f) / r);
        float sweep = 360f * frac;
        float end = Math.min(1f, Math.max(0.003f, (sweep + capDeg * 2f) / 360f));
        SweepGradient g = new SweepGradient(cx, cy, new int[]{c1, c2, c2}, new float[]{0f, end, 1f});
        M.setRotate(-90f - capDeg, cx, cy);
        g.setLocalMatrix(M);
        glow.setShader(g);
        glow.setStrokeWidth(st * 2.6f);
        glow.setAlpha(34);
        arc.setShader(g);
        arc.setStrokeWidth(st);
        c.drawArc(O, -90f, sweep, false, glow);
        c.drawArc(O, -90f, sweep, false, arc);
        double ang = Math.toRadians(-90 + sweep);
        c.drawCircle(cx + (float) (r * Math.cos(ang)), cy + (float) (r * Math.sin(ang)), st * 0.24f, knob);
    }

    /** Half gauge from 9 o'clock to 3 o'clock. */
    static void semi(Canvas c, float cx, float cy, float r, float st, float frac, int c1, int c2,
                     Paint track, Paint arc, Paint glow, Paint knob) {
        O.set(cx - r, cy - r, cx + r, cy + r);
        track.setStrokeWidth(st);
        c.drawArc(O, 180f, 180f, false, track);
        frac = Ui.clamp(frac);
        if (frac < 0.002f) return;
        LinearGradient g = new LinearGradient(cx - r, 0, cx + r, 0, c1, c2, Shader.TileMode.CLAMP);
        glow.setShader(g);
        glow.setStrokeWidth(st * 2.6f);
        glow.setAlpha(34);
        arc.setShader(g);
        arc.setStrokeWidth(st);
        float sweep = 180f * frac;
        c.drawArc(O, 180f, sweep, false, glow);
        c.drawArc(O, 180f, sweep, false, arc);
        double ang = Math.toRadians(180 + sweep);
        c.drawCircle(cx + (float) (r * Math.cos(ang)), cy + (float) (r * Math.sin(ang)), st * 0.24f, knob);
    }

    /** Radial tick marks, every `count`-th of a circle (or half circle if half=true). */
    static void ticks(Canvas c, float cx, float cy, float r, float len, int count, boolean half, Paint minor, Paint major) {
        float total = half ? 180f : 360f;
        float start = half ? 180f : -90f;
        int n = half ? count + 1 : count;
        for (int i = 0; i < n; i++) {
            boolean maj = i % (count / (half ? 2 : 4)) == 0;
            double a = Math.toRadians(start + total * i / count);
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            float l = maj ? len * 1.6f : len;
            c.drawLine(cx + cos * r, cy + sin * r, cx + cos * (r - l), cy + sin * (r - l), maj ? major : minor);
        }
    }

    /** Horizontal rounded progress bar with a gradient spanning the full track. */
    static void bar(Canvas c, float l, float t, float r, float b, float frac, int c1, int c2, Paint track, Paint fill) {
        float rad = (b - t) / 2f;
        O.set(l, t, r, b);
        c.drawRoundRect(O, rad, rad, track);
        frac = Ui.clamp(frac);
        if (frac < 0.002f) return;
        float w = Math.max(b - t, (r - l) * frac);
        fill.setShader(new LinearGradient(l, 0, r, 0, c1, c2, Shader.TileMode.CLAMP));
        O.set(l, t, l + w, b);
        c.drawRoundRect(O, rad, rad, fill);
    }

    /** Rounded pill with centred caption. */
    static void pill(Canvas c, float l, float t, float r, float b, int bg, Paint bgP, String text, Paint textP) {
        O.set(l, t, r, b);
        bgP.setColor(bg);
        float rad = (b - t) / 2f;
        c.drawRoundRect(O, rad, rad, bgP);
        Paint.Align al = textP.getTextAlign();
        textP.setTextAlign(Paint.Align.CENTER);
        c.drawText(text, (l + r) / 2f, Ui.mid((t + b) / 2f, textP), textP);
        textP.setTextAlign(al);
    }

    // ------------------------------------------------------------------ icons
    static final int IC_BOLT = 0, IC_SUN = 1, IC_REFRESH = 2, IC_STORAGE = 3, IC_BATTERY = 4, IC_APPS = 5,
            IC_DOWN = 6, IC_UP = 7;

    static void icon(Canvas c, int id, float cx, float cy, float s, Paint st, Paint fl) {
        switch (id) {
            case IC_BOLT:
                bolt(c, cx, cy, s, fl);
                break;
            case IC_SUN: {
                c.drawCircle(cx, cy, s * 0.2f, st);
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45);
                    float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
                    c.drawLine(cx + cos * s * 0.33f, cy + sin * s * 0.33f, cx + cos * s * 0.44f, cy + sin * s * 0.44f, st);
                }
                break;
            }
            case IC_REFRESH: {
                float r = s * 0.32f;
                O.set(cx - r, cy - r, cx + r, cy + r);
                c.drawArc(O, 40f, 290f, false, st);
                double a = Math.toRadians(330);
                float ex = cx + (float) (r * Math.cos(a)), ey = cy + (float) (r * Math.sin(a));
                float h = s * 0.16f;
                c.drawLine(ex, ey, ex - h, ey - h * 0.15f, st);
                c.drawLine(ex, ey, ex + h * 0.15f, ey - h, st);
                break;
            }
            case IC_STORAGE: {
                float w = s * 0.36f, h = s * 0.13f, gap = s * 0.05f;
                for (int i = -1; i <= 1; i++) {
                    float y = cy + i * (h * 2 + gap);
                    O.set(cx - w, y - h, cx + w, y + h);
                    c.drawRoundRect(O, h * 0.6f, h * 0.6f, st);
                    c.drawCircle(cx + w - h * 1.1f, y, s * 0.035f, fl);
                }
                break;
            }
            case IC_BATTERY: {
                float w = s * 0.24f, h = s * 0.40f;
                O.set(cx - w, cy - h + s * 0.06f, cx + w, cy + h);
                c.drawRoundRect(O, s * 0.08f, s * 0.08f, st);
                O.set(cx - w * 0.4f, cy - h - s * 0.02f, cx + w * 0.4f, cy - h + s * 0.06f);
                c.drawRect(O, fl);
                O.set(cx - w + s * 0.08f, cy, cx + w - s * 0.08f, cy + h - s * 0.08f);
                c.drawRoundRect(O, s * 0.03f, s * 0.03f, fl);
                break;
            }
            case IC_APPS: {
                float q = s * 0.15f, off = s * 0.19f;
                for (int i = 0; i < 4; i++) {
                    float x = cx + (i % 2 == 0 ? -off : off), y = cy + (i < 2 ? -off : off);
                    O.set(x - q, y - q, x + q, y + q);
                    c.drawRoundRect(O, q * 0.5f, q * 0.5f, st);
                }
                break;
            }
            case IC_DOWN:
            case IC_UP: {
                float dir = id == IC_DOWN ? 1 : -1;
                c.drawLine(cx, cy - dir * s * 0.3f, cx, cy + dir * s * 0.3f, st);
                c.drawLine(cx, cy + dir * s * 0.3f, cx - s * 0.22f, cy + dir * s * 0.08f, st);
                c.drawLine(cx, cy + dir * s * 0.3f, cx + s * 0.22f, cy + dir * s * 0.08f, st);
                break;
            }
            default:
                break;
        }
    }

    static void bolt(Canvas c, float cx, float cy, float s, Paint fl) {
        P.reset();
        P.moveTo(cx + 0.10f * s, cy - 0.46f * s);
        P.lineTo(cx - 0.28f * s, cy + 0.06f * s);
        P.lineTo(cx - 0.02f * s, cy + 0.06f * s);
        P.lineTo(cx - 0.10f * s, cy + 0.46f * s);
        P.lineTo(cx + 0.28f * s, cy - 0.08f * s);
        P.lineTo(cx + 0.02f * s, cy - 0.08f * s);
        P.close();
        c.drawPath(P, fl);
    }
}
