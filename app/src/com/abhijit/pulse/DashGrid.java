package com.abhijit.pulse;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * 12-column responsive grid. Three modes: wide (landscape tablet), medium (portrait tablet),
 * narrow (phone). Children declare span, height and order per mode.
 */
final class DashGrid extends ViewGroup {
    static final class LP extends ViewGroup.LayoutParams {
        final int spanW, spanM, hW, hM, hN, orderW, orderM;
        int x, y, w, h;

        LP(int spanW, int spanM, int hW, int hM, int hN, int orderW, int orderM) {
            super(MATCH_PARENT, WRAP_CONTENT);
            this.spanW = spanW;
            this.spanM = spanM;
            this.hW = hW;
            this.hM = hM;
            this.hN = hN;
            this.orderW = orderW;
            this.orderM = orderM;
        }
    }

    DashGrid(Context c) {
        super(c);
    }

    static int modeFor(int widthPx) {
        float dpW = widthPx / Ui.d;
        return dpW >= 960 ? 2 : dpW >= 600 ? 1 : 0;
    }

    @Override
    protected void onMeasure(int wms, int hms) {
        int W = MeasureSpec.getSize(wms);
        int mode = modeFor(W);
        float side = Ui.dp(mode == 0 ? 14 : 24), gap = Ui.dp(mode == 0 ? 12 : 18);
        float top = Ui.dp(mode == 0 ? 14 : 20), bottom = Ui.dp(32);
        float unit = (W - 2 * side - 11 * gap) / 12f;

        int n = getChildCount();
        int[] ord = new int[n];
        for (int i = 0; i < n; i++) ord[i] = i;
        for (int i = 1; i < n; i++) { // insertion sort by order key
            int k = ord[i], j = i - 1;
            int key = order(getChildAt(k), mode);
            while (j >= 0 && order(getChildAt(ord[j]), mode) > key) {
                ord[j + 1] = ord[j];
                j--;
            }
            ord[j + 1] = k;
        }

        float y = top, x = side, rowH = 0;
        int used = 0, rc = 0;
        int[] row = new int[n];
        for (int k = 0; k < n; k++) {
            View v = getChildAt(ord[k]);
            if (v.getVisibility() == GONE) continue;
            LP lp = (LP) v.getLayoutParams();
            int span = mode == 2 ? lp.spanW : mode == 1 ? lp.spanM : 12;
            float h = Ui.dp(mode == 2 ? lp.hW : mode == 1 ? lp.hM : lp.hN);
            if (used + span > 12 && rc > 0) {
                finishRow(row, rc, y, rowH);
                y += rowH + gap;
                x = side;
                used = 0;
                rc = 0;
                rowH = 0;
            }
            float cw = unit * span + gap * (span - 1);
            lp.x = Math.round(x);
            lp.w = Math.round(x + cw) - lp.x;
            x += cw + gap;
            used += span;
            rowH = Math.max(rowH, h);
            row[rc++] = ord[k];
        }
        if (rc > 0) {
            finishRow(row, rc, y, rowH);
            y += rowH;
        }
        y += bottom;
        int H = Math.round(y);
        if (MeasureSpec.getMode(hms) == MeasureSpec.EXACTLY) H = Math.max(H, MeasureSpec.getSize(hms));
        setMeasuredDimension(W, H);
    }

    private static int order(View v, int mode) {
        LP lp = (LP) v.getLayoutParams();
        return mode == 2 ? lp.orderW : lp.orderM;
    }

    private void finishRow(int[] row, int rc, float y, float rowH) {
        for (int i = 0; i < rc; i++) {
            View v = getChildAt(row[i]);
            LP lp = (LP) v.getLayoutParams();
            lp.y = Math.round(y);
            lp.h = Math.round(rowH);
            v.measure(MeasureSpec.makeMeasureSpec(lp.w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(lp.h, MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (v.getVisibility() == GONE) continue;
            LP lp = (LP) v.getLayoutParams();
            v.layout(lp.x, lp.y, lp.x + lp.w, lp.y + lp.h);
        }
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LP;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LP(12, 12, 200, 200, 200, 99, 99);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(android.util.AttributeSet attrs) {
        return generateDefaultLayoutParams();
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return generateDefaultLayoutParams();
    }
}
