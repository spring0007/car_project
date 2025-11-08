package com.launcher.yfd_ui01.app;

import android.annotation.SuppressLint;
import android.util.Log;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 模拟车速
 */
public class SpeedSimulator {
    private static final int MIN_SPEED = 0;      // 最小速度 0 km/h
    private static final int MAX_SPEED = 240;    // 最大速度 200 km/h
    private static final int SPEED_CHANGE_INTERVAL = 500; // 速度变化间隔 1秒

    private int currentSpeed = 0;
    private boolean isAccelerating = true;
    private Timer speedTimer;
    private SpeedChangeListener listener;
    private static Random random;

    public interface SpeedChangeListener {
        void onSpeedChanged(int speed);
    }

    public SpeedSimulator(SpeedChangeListener listener) {
        this.listener = listener;
        random = new Random(); // 创建Random对象


    }

    /**
     * 开始模拟车速变化
     */
    @SuppressLint("DiscouragedApi")
    public void startSimulation() {
        if (speedTimer != null) {
            stopSimulation();
        }

        speedTimer = new Timer();
        speedTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateSpeed();
                if (listener != null) {
                    listener.onSpeedChanged(currentSpeed);
                }
            }
        }, 0, SPEED_CHANGE_INTERVAL);
    }

    /**
     * 停止模拟车速变化
     */
    public void stopSimulation() {
        if (speedTimer != null) {
            speedTimer.cancel();
            speedTimer = null;
        }
    }

    /**
     * 更新车速
     */
    private void updateSpeed() {
        int number = random.nextInt(100); // 生成随机整数
        if (isAccelerating) {
            currentSpeed += number; // 加速 5 km/h
            if (currentSpeed >= MAX_SPEED) {
                currentSpeed = MAX_SPEED;
                isAccelerating = false; // 达到最大速度后开始减速
            }
        } else {
            currentSpeed -= number; // 减速 5 km/h
            if (currentSpeed <= MIN_SPEED) {
                currentSpeed = MIN_SPEED;
                isAccelerating = true; // 达到最小速度后开始加速
            }
        }
    }

    /**
     * 获取当前模拟速度
     */
    public int getCurrentSpeed() {
        return currentSpeed;
    }

    //oncreate如下实例化及关闭动画
//    //1. 创建速度模拟器
//    speedSimulator = new SpeedSimulator(new SpeedSimulator.SpeedChangeListener() {
//        @Override
//        public void onSpeedChanged(int speed) {
//            // 在主线程中更新UI
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    // 您可以在这里处理其他与速度相关的逻辑
//                    Log.i(TAG, "speed = " + speed);
//                    dashboardView.udDataSpeed(speed);
//                }
//            });
//        }
//    });
//
//    // 开始模拟
//        speedSimulator.startSimulation();
//

    //2. 在onPause()方法中关闭动画
//    @Override
//    protected void onPause() {
//        super.onPause();
//        if(dashboardView!= null)
//            dashboardView.closeAnimation();
//    }


}
