package com.awell.ctrlview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.awell.launcher.R;
import com.awell.ctrlview.SpetrumView;
/**
 * @author xiayiye5
 * @date 2022/7/11 10:03
 */
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioRecorder {

    private static final int SAMPLE_RATE = 44100;    // 44.1kHz
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO; // 单声道
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT; // 16位
    private static final int BUFFER_SIZE = 2048;     // 缓冲区大小（需为2的幂）
    private static final long UPDATE_INTERVAL = 300; // 300毫秒

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private SpetrumView mSpetrumView;
    private ExecutorService executorService;
    private Handler mainHandler;
    private long lastUpdateTime = 0;

    public AudioRecorder(SpetrumView spetrumView) {
        this.mSpetrumView = spetrumView;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        initAudioRecord();
    }

    private void initAudioRecord() {
        int minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
        );
        if (minBufferSize == 0) {
            throw new RuntimeException("Invalid audio configuration");
        }

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                minBufferSize * 2
        );
    }

    public void startRecording() {
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new IllegalStateException("AudioRecord not initialized");
        }
        audioRecord.startRecording();
        isRecording = true;
        executorService.execute(new SpectrumProcessor());
    }

    public void stopRecording() {
        if (isRecording) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            isRecording = false;
        }
        executorService.shutdown();
    }

    private class SpectrumProcessor implements Runnable {
        @Override
        public void run() {
            short[] audioData = new short[BUFFER_SIZE];
            double[] fftBuffer = new double[BUFFER_SIZE * 2]; // 复数形式

            while (isRecording) {
                int readResult = audioRecord.read(audioData, 0, audioData.length);
                if (readResult == AudioRecord.ERROR_INVALID_OPERATION || readResult == 0) {
                    break;
                }

                // 将short数组转为复数数组（实部为左声道+右声道/2，虚部为0）
                for (int i = 0; i < audioData.length; i++) {
                    fftBuffer[2 * i] = (byte) audioData[i]; // 实部
                    fftBuffer[2 * i + 1] = 0;             // 虚部
                }

                // 执行FFT（需自己实现或使用库）
                FFT fft = new FFT(BUFFER_SIZE * 2);
                fft.forward(fftBuffer);

                // 提取频谱幅度（dB值）
                double[] magnitudes = new double[BUFFER_SIZE / 2];
                for (int i = 0; i < magnitudes.length; i++) {
                    double real = fftBuffer[2 * i];
                    double imag = fftBuffer[2 * i + 1];
                    double magnitude = Math.sqrt(real * real + imag * imag);
                    magnitudes[i] = 20 * Math.log10(magnitude); // 转换为dB
                }

                // 将频谱数据传递给UI线程
                //updateSpectrum(magnitudes);
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastUpdateTime >= UPDATE_INTERVAL) {
                    lastUpdateTime = currentTime;
                    mainHandler.post(() -> updateSpectrum(magnitudes));
                }
            }
        }
    }

    private void updateSpectrum(double[] magnitudes) {
        // 将频谱数据截断到前16个元素（与原有逻辑兼容）
        double[] spectrumData = Arrays.copyOfRange(magnitudes, 0, Math.min(16, magnitudes.length));

        // 转换为字符串数组（根据实际需求调整阈值）
        String[] result = new String[spectrumData.length];
        for (int i = 0; i < result.length; i++) {
            // 示例：将dB值限制在0~1之间，映射到整数块数量
            double normalized = (spectrumData[i] + 100) / 100; // 假设原始范围是-100dB ~ 0dB
            int block = (int) (normalized * 16); // 16级频谱块
            result[i] = String.valueOf(block);
        }
        // 调用原有方法更新UI
        mSpetrumView.updateSpetrum(result);
    }
}