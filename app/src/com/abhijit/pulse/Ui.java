package com.abhijit.pulse;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;

import java.util.Locale;

/** Palette, typography, paint factories and number formatting. */
final class Ui {
    private Ui() {}

    static float d = 1f;

    static void init(Context c) {
        d = c.getResources().getDisplayMetrics().density;
    }

    static float dp(float v) {
        return v * d;
    }

    // ---- palette -------------------------------------------------------------------------
    static final int BG0 = 0xFF04070E, BG1 = 0xFF0A1122;
    static final int TEXT = 0xFFF3F6FC, DIM = 0xFF8E98AF, FAINT = 0xFF56617A;
    static final int CYAN = 0xFF22D3EE, BLUE = 0xFF3B82F6, VIOLET = 0xFF8B5CF6, PINK = 0xFFEC4899,
            MINT = 0xFF34D399, LIME = 0xFFA3E635, AMBER = 0xFFFBBF24, ORANGE = 0xFFFB923C, RED = 0xFFF43F5E;

    // ---- type ----------------------------------------------------------------------------
    static final Typeface THIN = Typeface.create("sans-serif-thin", Typeface.NORMAL);
    static final Typeface LIGHT = Typeface.create("sans-serif-light", Typeface.NORMAL);
    static final Typeface REG = Typeface.create("sans-serif", Typeface.NORMAL);
    static final Typeface MED = Typeface.create("sans-serif-medium", Typeface.NORMAL);

    static Paint text(Typeface tf, float sizeDp, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG | Paint.LINEAR_TEXT_FLAG);
        p.setTypeface(tf);
        p.setTextSize(dp(sizeDp));
        p.setColor(color);
        try {
            p.setFontFeatureSettings("'tnum'");
        } catch (Throwable ignored) {
        }
        return p;
    }

    static Paint caps(float sizeDp, int color) {
        Paint p = text(MED, sizeDp, color);
        p.setLetterSpacing(0.14f);
        return p;
    }

    static Paint fill(int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        return p;
    }

    static Paint stroke(float wDp, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(wDp));
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setColor(color);
        return p;
    }

    static int a(int color, float alpha) {
        int al = Math.round(clamp(alpha) * 255f);
        return (color & 0x00FFFFFF) | (al << 24);
    }

    static int mix(int c1, int c2, float t) {
        t = clamp(t);
        int a1 = c1 >>> 24, r1 = (c1 >> 16) & 255, g1 = (c1 >> 8) & 255, b1 = c1 & 255;
        int a2 = c2 >>> 24, r2 = (c2 >> 16) & 255, g2 = (c2 >> 8) & 255, b2 = c2 & 255;
        return (Math.round(a1 + (a2 - a1) * t) << 24) | (Math.round(r1 + (r2 - r1) * t) << 16)
                | (Math.round(g1 + (g2 - g1) * t) << 8) | Math.round(b1 + (b2 - b1) * t);
    }

    static float clamp(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    // ---- formatting ----------------------------------------------------------------------
    private static final String[] SIZE_U = {"B", "KB", "MB", "GB", "TB"};
    private static final String[] RATE_U = {"B/s", "KB/s", "MB/s", "GB/s"};

    static String num(double v) {
        if (v >= 100) return String.format(Locale.US, "%.0f", v);
        if (v >= 10) return String.format(Locale.US, "%.1f", v);
        return String.format(Locale.US, "%.2f", v);
    }

    static String[] sizeParts(long b) {
        if (b < 0) return new String[]{"—", ""};
        double v = b;
        int i = 0;
        while (v >= 1000 && i < SIZE_U.length - 1) {
            v /= 1024.0;
            i++;
        }
        return new String[]{i == 0 ? String.valueOf((long) v) : num(v), SIZE_U[i]};
    }

    static String size(long b) {
        String[] p = sizeParts(b);
        return p[1].length() == 0 ? p[0] : p[0] + " " + p[1];
    }

    static String[] rateParts(double bps) {
        if (bps < 0) bps = 0;
        double v = bps;
        int i = 0;
        while (v >= 1000 && i < RATE_U.length - 1) {
            v /= 1024.0;
            i++;
        }
        return new String[]{i == 0 ? String.valueOf((long) v) : num(v), RATE_U[i]};
    }

    static String rate(double bps) {
        String[] p = rateParts(bps);
        return p[0] + " " + p[1];
    }

    static String duration(long ms) {
        long s = ms / 1000, m = s / 60, h = m / 60, dd = h / 24;
        if (dd > 0) return String.format(Locale.US, "%dd %02dh %02dm", dd, h % 24, m % 60);
        if (h > 0) return String.format(Locale.US, "%dh %02dm", h, m % 60);
        return String.format(Locale.US, "%dm %02ds", m, s % 60);
    }

    /** Ellipsize a string so it fits into maxW pixels with the given paint. */
    static String fit(String s, Paint p, float maxW) {
        if (s == null) return "";
        if (maxW <= 0) return "";
        if (p.measureText(s) <= maxW) return s;
        int lo = 0, hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (p.measureText(s, 0, mid) + p.measureText("…") <= maxW) lo = mid;
            else hi = mid - 1;
        }
        return lo <= 0 ? "…" : s.substring(0, lo).trim() + "…";
    }

    private static final Paint.FontMetrics FM = new Paint.FontMetrics();

    /** Baseline that vertically centres the paint's text on cy. */
    static float mid(float cy, Paint p) {
        p.getFontMetrics(FM);
        return cy - (FM.ascent + FM.descent) / 2f;
    }

    static String cap(String s) {
        if (s == null || s.length() == 0) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
