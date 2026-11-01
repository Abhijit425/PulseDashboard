package com.abhijit.pulse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

final class DeviceCard extends Card {
    String[][] items = new String[0][];
    int uptimeIndex = -1;
    private final RectF o = new RectF();
    private final Paint tileBg = Ui.fill(0x08FFFFFF), tileBorder = Ui.stroke(1, 0x10FFFFFF),
            capP = Ui.caps(9, Ui.DIM), val = Ui.text(Ui.MED, 14.5f, Ui.TEXT), accentBar = Ui.fill(Ui.BLUE);

    DeviceCard(Context c, Sys s, String android) {
        super(c, s, "DEVICE", Ui.BLUE);
        chip = "ANDROID " + android;
    }

    @Override
    void update() {
        if (uptimeIndex >= 0 && uptimeIndex < items.length) items[uptimeIndex][1] = Ui.duration(sys.uptime);
    }

    @Override
    void drawContent(Canvas c, RectF r) {
        int n = items.length;
        if (n == 0) return;
        int cols = r.width() > Ui.dp(540) ? 4 : r.width() > Ui.dp(380) ? 3 : 2;
        int rows = (n + cols - 1) / cols;
        float gap = Ui.dp(10);
        float tw = (r.width() - gap * (cols - 1)) / cols;
        float th = (r.height() - gap * (rows - 1)) / rows;
        float rad = Ui.dp(14);
        for (int i = 0; i < n; i++) {
            int cx = i % cols, cy = i / cols;
            float l = r.left + cx * (tw + gap), t = r.top + cy * (th + gap);
            o.set(l, t, l + tw, t + th);
            c.drawRoundRect(o, rad, rad, tileBg);
            c.drawRoundRect(o, rad, rad, tileBorder);
            float mid = t + th / 2f;
            o.set(l + Ui.dp(10), mid - Ui.dp(11), l + Ui.dp(12.5f), mid + Ui.dp(11));
            accentBar.setColor(Ui.a(Ui.BLUE, i == uptimeIndex ? 1f : 0.55f));
            c.drawRoundRect(o, Ui.dp(2), Ui.dp(2), accentBar);
            float tx = l + Ui.dp(22);
            c.drawText(items[i][0], tx, mid - Ui.dp(4), capP);
            c.drawText(Ui.fit(items[i][1], val, tw - Ui.dp(30)), tx, mid + Ui.dp(15), val);
        }
    }
}
