package com.abhijit.pulse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;

final class ActionsCard extends Card {
    interface Listener {
        void onAction(int id);
    }

    static final int BOOST = 0, AWAKE = 1, REFRESH = 2, STORAGE = 3, BATTERY = 4, APPS = 5;

    private static final String[] TITLES = {"Boost RAM", "Keep Awake", "Refresh", "Storage", "Battery", "Apps"};
    private static final int[] ICONS = {Gfx.IC_BOLT, Gfx.IC_SUN, Gfx.IC_REFRESH, Gfx.IC_STORAGE, Gfx.IC_BATTERY, Gfx.IC_APPS};
    private static final int[] COLORS = {Ui.PINK, Ui.AMBER, Ui.CYAN, Ui.VIOLET, Ui.MINT, Ui.BLUE};

    Listener listener;
    boolean boosting, keepAwake;
    String refreshLabel = "1 s";
    String status = "Ready · tap a tile";
    int statusColor = Ui.DIM;

    private final RectF[] rects = new RectF[6];
    private int pressed = -1;
    private final RectF o = new RectF();
    private final Paint tile = Ui.fill(0x0AFFFFFF), tileBorder = Ui.stroke(1, 0x14FFFFFF), primary = Ui.fill(0xFFFFFFFF),
            iconBg = Ui.fill(0), iconSt = Ui.stroke(2f, 0xFFFFFFFF), iconFl = Ui.fill(0xFFFFFFFF),
            spin = Ui.stroke(2.4f, 0xFFFFFFFF), statusDot = Ui.fill(0);
    private final Paint titleP = Ui.text(Ui.MED, 14, Ui.TEXT), subP = Ui.text(Ui.REG, 11.5f, Ui.DIM),
            statusP = Ui.text(Ui.REG, 12.5f, Ui.DIM);

    ActionsCard(Context c, Sys s) {
        super(c, s, "QUICK ACTIONS", Ui.VIOLET);
        for (int i = 0; i < rects.length; i++) rects[i] = new RectF();
        setClickable(true);
        setHapticFeedbackEnabled(true);
        chip = "TOOLS";
    }

    private String sub(int i) {
        switch (i) {
            case BOOST: return boosting ? "Optimizing…" : "Clean memory";
            case AWAKE: return keepAwake ? "On" : "Off";
            case REFRESH: return "Every " + refreshLabel;
            case STORAGE: return "Manage space";
            case BATTERY: return "Usage & saver";
            default: return "Installed apps";
        }
    }

    @Override
    void drawContent(Canvas c, RectF r) {
        float statusH = Ui.dp(30);
        int cols = r.width() > Ui.dp(400) ? 3 : 2;
        int rows = (6 + cols - 1) / cols;
        float gap = Ui.dp(10);
        float gh = r.height() - statusH - Ui.dp(4);
        float tw = (r.width() - gap * (cols - 1)) / cols;
        float th = (gh - gap * (rows - 1)) / rows;
        float rad = Ui.dp(16);
        for (int i = 0; i < 6; i++) {
            int cx = i % cols, cy = i / cols;
            float l = r.left + cx * (tw + gap), t = r.top + cy * (th + gap);
            RectF rr = rects[i];
            rr.set(l, t, l + tw, t + th);
            boolean on = i == AWAKE && keepAwake;
            if (i == BOOST) {
                primary.setShader(new LinearGradient(rr.left, rr.top, rr.right, rr.bottom, Ui.VIOLET, Ui.PINK, Shader.TileMode.CLAMP));
                primary.setAlpha(pressed == i ? 200 : 255);
                c.drawRoundRect(rr, rad, rad, primary);
            } else {
                tile.setColor(on ? Ui.a(COLORS[i], 0.12f) : (pressed == i ? 0x1CFFFFFF : 0x0AFFFFFF));
                c.drawRoundRect(rr, rad, rad, tile);
                tileBorder.setColor(on ? Ui.a(COLORS[i], 0.45f) : 0x14FFFFFF);
                c.drawRoundRect(rr, rad, rad, tileBorder);
            }

            float isz = Math.min(Ui.dp(tw > Ui.dp(170) ? 38 : 32), th - Ui.dp(16));
            float icx = rr.left + Ui.dp(tw > Ui.dp(170) ? 12 : 10) + isz / 2f, icy = rr.centerY();
            int col = i == BOOST ? 0xFFFFFFFF : COLORS[i];
            iconBg.setColor(i == BOOST ? 0x33FFFFFF : Ui.a(col, 0.14f));
            c.drawCircle(icx, icy, isz / 2f, iconBg);
            iconSt.setColor(col);
            iconFl.setColor(col);
            Gfx.icon(c, ICONS[i], icx, icy, isz * 0.62f, iconSt, iconFl);
            if (i == BOOST && boosting) {
                float a = (SystemClock.uptimeMillis() % 900) / 900f * 360f;
                o.set(icx - isz / 2f + Ui.dp(1), icy - isz / 2f + Ui.dp(1), icx + isz / 2f - Ui.dp(1), icy + isz / 2f - Ui.dp(1));
                c.drawArc(o, a, 100, false, spin);
                animating = true;
            }

            float tx = icx + isz / 2f + Ui.dp(10);
            float maxW = rr.right - tx - Ui.dp(8);
            titleP.setColor(Ui.TEXT);
            subP.setColor(i == BOOST ? 0xDDFFFFFF : on ? COLORS[i] : Ui.DIM);
            c.drawText(Ui.fit(TITLES[i], titleP, maxW), tx, icy - Ui.dp(2), titleP);
            c.drawText(Ui.fit(sub(i), subP, maxW), tx, icy + Ui.dp(15), subP);
        }

        float sy = r.bottom - statusH / 2f + Ui.dp(2);
        statusDot.setColor(statusColor);
        c.drawCircle(r.left + Ui.dp(4), sy, Ui.dp(3.5f), statusDot);
        statusP.setColor(statusColor == Ui.DIM ? Ui.DIM : Ui.TEXT);
        c.drawText(Ui.fit(status, statusP, r.width() - Ui.dp(16)), r.left + Ui.dp(15), Ui.mid(sy, statusP), statusP);
    }

    private int hit(float x, float y) {
        for (int i = 0; i < rects.length; i++) if (rects[i].contains(x, y)) return i;
        return -1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = hit(e.getX(), e.getY());
                invalidate();
                return pressed >= 0;
            case MotionEvent.ACTION_MOVE:
                if (pressed >= 0 && !rects[pressed].contains(e.getX(), e.getY())) {
                    pressed = -1;
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP: {
                int p = pressed;
                pressed = -1;
                invalidate();
                if (p >= 0 && rects[p].contains(e.getX(), e.getY())) {
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    playSoundEffect(android.view.SoundEffectConstants.CLICK);
                    if (listener != null) listener.onAction(p);
                }
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                pressed = -1;
                invalidate();
                return true;
            default:
                return true;
        }
    }
}
