package com.awell.ctrlview;

import java.util.Arrays;

public class FFT {
    private final int n;
    private final double[] real;
    private final double[] imag;

    public FFT(int size) {
        int n1;
        n1 = 1;
        while (n1 < size) n1 <<= 1; // 找到最小的 2^n >= size
        n = n1;
        real = new double[n];
        imag = new double[n];
    }

    public void forward(double[] buffer) {
        if (buffer.length != n) {
            throw new IllegalArgumentException("Buffer length must equal FFT size (" + n + ")");
        }
        System.arraycopy(buffer, 0, real, 0, n);
        Arrays.fill(imag, 0);
        fft(0, n - 1, 1);
    }

    private void fft(int l, int r, int sign) {
        if (l >= r) return;

        int m = (l + r) >> 1;
        fft(l, m, -sign);
        fft(m + 1, r, -sign);

        double angle = Math.toRadians(2 * Math.PI * sign * (r - l + 1) / n);
        double wlen = Math.cos(angle);
        double wimp = Math.sin(angle);

        for (int k = l; k <= m; k++) {
            double u = real[k];
            double v = imag[k] * wlen - real[k] * wimp;
            real[k] = u + v;
            imag[k] = u - v;

            // 优化：更新 wlen 和 wimp（避免重复计算）
            double wlenNext = wlen * wlen - wimp * wimp;
            double wimpNext = 2 * wlen * wimp;
            wlen = wlenNext;
            wimp = wimpNext;
        }
    }
}
