package com.abhijit.pulse;

/** Fixed-size ring buffer of samples, oldest first. */
final class History {
    private final float[] v;
    private int head;
    int count;

    History(int cap) {
        v = new float[cap];
    }

    void add(float x) {
        v[head] = x;
        head = (head + 1) % v.length;
        if (count < v.length) count++;
    }

    float get(int i) {
        int idx = (head - count + i) % v.length;
        if (idx < 0) idx += v.length;
        return v[idx];
    }

    int cap() {
        return v.length;
    }

    float max() {
        float m = 0;
        for (int i = 0; i < count; i++) m = Math.max(m, get(i));
        return m;
    }

    float last() {
        return count == 0 ? 0 : get(count - 1);
    }
}
