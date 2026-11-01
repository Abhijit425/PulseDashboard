package com.abhijit.pulse;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

/** Smooth area chart that scrolls in from the right. */
final class Chart {
    private final int color;
    private final Paint line, fill, grid, dot, halo;
    private final Path p = new Path(), f = new Path();
    private float cl = -1, ct = -1, cr = -1, cb = -1;

    Chart(int color) {
        this.color = color;
        line = Ui.stroke(2.2f, color);
        fill = Ui.fill(0xFFFFFFFF);
        grid = Ui.stroke(1f, 0x12FFFFFF);
        grid.setStrokeCap(Paint.Cap.BUTT);
        grid.setPathEffect(new DashPathEffect(new float[]{Ui.dp(3), Ui.dp(5)}, 0));
        dot = Ui.fill(color);
        halo = Ui.fill(Ui.a(color, 0.22f));
    }

    void draw(Canvas c, RectF r, History h, float min, float max, boolean drawGrid) {
        if (drawGrid) {
            for (int k = 0; k <= 3; k++) {
                float y = r.top + r.height() * k / 3f;
                c.drawLine(r.left, y, r.right, y, grid);
            }
        }
        int n = h.count;
        if (n < 2 || max <= min) return;
        if (cl != r.left || ct != r.top || cr != r.right || cb != r.bottom) {
            fill.setShader(new LinearGradient(0, r.top, 0, r.bottom, Ui.a(color, 0.30f), Ui.a(color, 0f), Shader.TileMode.CLAMP));
            line.setShader(new LinearGradient(r.left, 0, r.right, 0, Ui.a(color, 0.15f), color, Shader.TileMode.CLAMP));
            cl = r.left; ct = r.top; cr = r.right; cb = r.bottom;
        }
        float inset = Ui.dp(3);
        float top = r.top + inset, bottom = r.bottom - Ui.dp(1);
        float step = r.width() / (h.cap() - 1);
        float x0 = r.right - (n - 1) * step;
        p.reset();
        f.reset();
        float px = 0, py = 0;
        for (int i = 0; i < n; i++) {
            float x = x0 + i * step;
            float v = Ui.clamp((h.get(i) - min) / (max - min));
            float y = bottom - v * (bottom - top);
            if (i == 0) {
                p.moveTo(x, y);
                f.moveTo(x, r.bottom);
                f.lineTo(x, y);
            } else {
                float mx = (px + x) / 2f;
                p.cubicTo(mx, py, mx, y, x, y);
                f.cubicTo(mx, py, mx, y, x, y);
            }
            px = x;
            py = y;
        }
        f.lineTo(px, r.bottom);
        f.close();
        c.drawPath(f, fill);
        c.drawPath(p, line);
        c.drawCircle(px, py, Ui.dp(6.5f), halo);
        c.drawCircle(px, py, Ui.dp(3.2f), dot);
    }
}
