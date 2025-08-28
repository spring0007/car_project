/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.awell.launcher2;

import static android.media.MediaMetadata.METADATA_KEY_ALBUM_ART;
import static android.media.MediaMetadata.METADATA_KEY_ALBUM_ART_URI;
import static android.media.MediaMetadata.METADATA_KEY_ART;
import static android.media.MediaMetadata.METADATA_KEY_MEDIA_URI;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.util.Log;

import com.awell.aidl.awellface.IAwellApi;
import com.awell.ctrlview.MusicWidget;
import com.awell.launcher.R;
import com.awell.library.AwellTool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 处理注册到media session服务中的媒体
 */
public class MediaNotificationListener/* extends ServiceNotificationListenerService*/ {
    private static final String TAG = "MediaNotificationListenerLog";
    // 常量定义
    private static final long UPDATE_INTERVAL_MS = 500;
    private static final Logger log = LoggerFactory.getLogger(MediaNotificationListener.class);

    // 依赖组件
    private Context mContext;
    private IAwellApi mAwellApi;
    private MediaSessionManager mMediaSessionManager;

    // 状态变量
    private final CopyOnWriteArrayList<MediaController> mControllers = new CopyOnWriteArrayList<>();
    private final List<MediaController.Callback> mCallbacks = new ArrayList<>();

    private long mCurrentPosition = 0;
    private long mDuration = 0;
    public String mPlayingPackageName;

    // 线程处理
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private MediaController mMediaController;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private Map<String, Boolean> mAutoUpdateControllers = new HashMap<>();

    // 记录每个控制器的最后更新时间戳
    private Map<String, Long> mLastUpdateTime = new HashMap<>();

    // 手动更新任务集合
    private final Map<String, Runnable> mUpdateRunnables = new HashMap<>();

    private final Handler mUpdateHandler = new Handler(Looper.getMainLooper());

    private static final long AUTO_UPDATE_THRESHOLD_MS = 500;
    private long lastUpdateTime = 0;
    private final long UPDATE_INTERVAL = 1000; // 间隔

    private int CURRENT_UPDATE_TIMES = 0;
    private final int TOTAL_UPDATE_TIMES = 5;
    private int mLastPlayState = 0;


    /**
     * 记录可访问的图片Uri
     */
    private String mLastUri = "default";
    //该目录下可能无图片，当前播放的歌曲如果没有专辑图片，bug:会显示上一曲的专辑图片
    private String mKwPlayImageUri_300 = "/albumcover/300";
    private String mKwPlayImageUri_700 = "/albumcover/700";
    private String mKwPicImageUri_700 = "/pic_music/700";
    private boolean isRegisterCallback = false;

    private final MediaController.Callback mMediaControllerCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {

            mExecutor.execute(() -> {
                if (state != null) {

                    //Log.i(TAG, "onPlaybackStateChanged: huang state=>" + state);

                    long currentTime = System.currentTimeMillis();
                    // 检查是否达到时间间隔,到达指定间隔发送数据
                    if (currentTime - lastUpdateTime >= UPDATE_INTERVAL) {
                        lastUpdateTime = currentTime;
                        handlePlayingTime(mMediaController);
                        handleMetadataArtUri();

                        if (mLastPlayState != state.getState()) {
                            boolean isPlaying = state.getState() == PlaybackState.STATE_PLAYING;
                            if (isPlaying) {
                                sendMediaPlayInfoToWidget(mMediaController, true);
                            }
                        }

                        //减少状态更新频率
                        if (mLastPlayState != state.getState()
                                || CURRENT_UPDATE_TIMES++ < TOTAL_UPDATE_TIMES) {
                            sendPlayStateToWidget(state.getState() == PlaybackState.STATE_PLAYING);
                            mLastPlayState = state.getState();
                        }

                    }
                }
            });
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            Log.d(TAG, "onMetadataChanged---metadata: " + metadata);
            CURRENT_UPDATE_TIMES = 0;
            if (metadata != null) {

                handleMetadataArtUri();

                handleMetadataChange(mMediaController, metadata);

            }
        }

        @Override
        public void onSessionDestroyed() {
            Log.d(TAG, "--onSessionDestroyed- ");
            cleanup();
        }
    };

    private void handleMetadataArtUri() {
        if (mMediaController.getMetadata() != null) {
            String metaArtUri = mMediaController.getMetadata().getString(METADATA_KEY_ALBUM_ART_URI);
            Log.i(TAG, "handleMetadataArtUri: huang mLastUri=>" + mLastUri + " metaArtUri=>" + metaArtUri);

            if (metaArtUri != null && hasImage(metaArtUri) && !metaArtUri.equals(mLastUri)
                    //"default" 重新注册到media session里的元数据可能只包含300的图片
                    || ("default".equals(mLastUri) && metaArtUri != null)) {
                mLastUri = metaArtUri;
                Log.i(TAG, "handleMetadataArtUri: huang update uri =>" + mLastUri);
                notifyHostAlbumArtUpdate(metaArtUri);
            } else if (metaArtUri == null && !Objects.equals(mLastUri, null)) {
                //没有网络也需要调用，使用默认图片
                //获取的METADATA_KEY_ALBUM_ART 一直在变化
                mLastUri = null;
                notifyHostAlbumArtUpdate(null);
            }
        }
    }

    private boolean hasImage(String metaArtUri) {
        return metaArtUri.contains(mKwPlayImageUri_700)
                || metaArtUri.contains(mKwPicImageUri_700);
        //|| metaArtUri.contains(mKwPlayImageUri_300);
    }

    public void initDependencies(Context context) {
        mContext = context;
        IBinder binder = ServiceManager.getService("AwellAutoApi");
        mAwellApi = binder != null ? IAwellApi.Stub.asInterface(binder) : null;

        mMediaSessionManager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);

        MediaSessionManager.OnActiveSessionsChangedListener mSessionsListener = controllers -> {
            // 处理会话变化
            assert controllers != null;
            Log.d(TAG, "onActiveSessionsChanged--controllers:" + controllers.size());
            if (controllers.isEmpty()) {
                //setPlayingPackage(null);
                cleanup();
                sendPlayStateToWidget(false);
            }
        };
        Log.d(TAG, "initDependencies--mMediaSessionManager:" + mMediaSessionManager);
        if (mMediaSessionManager != null) {
            ComponentName componentName = new ComponentName(context, NotificationListenerService.class);
            List<MediaController> controllers = mMediaSessionManager.getActiveSessions(
                    new ComponentName(mContext, NotificationListenerService.class));
            mMediaSessionManager.addOnActiveSessionsChangedListener(mSessionsListener, componentName);
            Log.d(TAG, "initDependencies--mControllers:" + mControllers.size());
        }

    }

    public void removeCallbacks() {
        if (mMediaController != null && isRegisterCallback) {
            mMediaController.unregisterCallback(mMediaControllerCallback);
            mLastUri = "default";
            isRegisterCallback = false;
        }
    }

    public void startCallbacks() {
        Log.i(TAG, "startCallbacks: huang currentControlPkgIsChange()=>" + currentControlPkgIsChange() + " isRegisterCallback=>" + isRegisterCallback);
        if (!isRegisterCallback || currentControlPkgIsChange()) {
            updateMediaController();
        }
    }

    /**
     * 当前的媒体控制应用是否和当前播放的应用一致
     *
     * @return true: current pkg != media control pkg
     */
    private boolean currentControlPkgIsChange() {
        return mMediaController != null && !mMediaController.getPackageName().equals(getPlayingPackageName());
    }

    /**
     * 收到AwellTool.MEDIA_PLAY start的时候赋值
     *
     * @param pkg 当前start的应用包名
     */
    public void setPlayingPackageName(String pkg) {
        mPlayingPackageName = pkg;
    }

    public String getPlayingPackageName() {
        return mPlayingPackageName;
    }

    public void skipToNext() {
        if (mMediaController != null) {
            PlaybackState state = mMediaController.getPlaybackState();
            if (state != null && (state.getActions() & PlaybackState.ACTION_SKIP_TO_NEXT) != 0) {
                mMediaController.getTransportControls().skipToNext();
            }
        }
    }

    public void skipToPrevious() {
        if (mMediaController != null) {
            PlaybackState state = mMediaController.getPlaybackState();
            if (state != null && (state.getActions() & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0) {
                mMediaController.getTransportControls().skipToPrevious();
            }
        }
    }

    public void togglePlayPause() {
        if (mMediaController != null) {
            PlaybackState state = mMediaController.getPlaybackState();
            if (state != null) {
                long actions = state.getActions();

                if ((state.getState() == PlaybackState.STATE_PLAYING) &&
                        ((actions & PlaybackState.ACTION_PAUSE) != 0)) {
                    // 当前正在播放且支持暂停操作
                    mMediaController.getTransportControls().pause();
                } else if ((state.getState() == PlaybackState.STATE_PAUSED) &&
                        ((actions & PlaybackState.ACTION_PLAY) != 0)) {
                    // 当前已暂停且支持播放操作
                    mMediaController.getTransportControls().play();
                }
            }
        }
    }

    public void togglePause() {
        if (mMediaController != null) {
            PlaybackState state = mMediaController.getPlaybackState();
            if (state != null) {
                long actions = state.getActions();
                // 当前正在播放且支持暂停操作
                mMediaController.getTransportControls().pause();
            }
        }
    }

    public void updateMediaController() {

        MediaController activeController = null;
        // 获取当前正在播放的controller
        for (MediaController controller : mMediaSessionManager.getActiveSessions(null)) {
            PlaybackState state = controller.getPlaybackState();
            Log.i(TAG, "updateMediaController: huang control pkg=>" + controller.getPackageName());
            Log.i(TAG, "updateMediaController: huang getPlayingPackageName()=>" + getPlayingPackageName());

            if (Objects.equals(controller.getPackageName(), getPlayingPackageName())) {
                //setPlayingPackageName(controller.getPackageName());
                activeController = controller;
                if (state != null)
                    sendMediaPlayInfoToWidget(controller, state.getState() == PlaybackState.STATE_PLAYING);
                break;
            }
        }

        if (activeController == null) {
            Log.d(TAG, "No active playing controller found");
            return;
        }

        // 取消之前的回调
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mMediaControllerCallback);
        }

        mMediaController = activeController;

        mMediaController.registerCallback(mMediaControllerCallback);
        isRegisterCallback = true;

        handlePlaybackStateChange(mMediaController);

        MediaMetadata currentMetadata = mMediaController.getMetadata();
        if (currentMetadata != null) {
            handleMetadataChange(mMediaController, currentMetadata);
        }
        //可能播放了本地音乐图片已切换
        //重新注册到系统的media session需要重新更新一次图片
        mLastUri = "default";
    }

    private void detectAutoUpdateBehavior(MediaController controller, PlaybackState state) {
        if (controller == null || state == null) return;
        String packageName = controller.getPackageName();
        long currentTime = System.currentTimeMillis();

        // 安全获取上次更新时间（提供默认值0）
        Long lastUpdateTimeObj = mLastUpdateTime.get(packageName);
        long lastUpdateTime = lastUpdateTimeObj != null ? lastUpdateTimeObj : 0L;

        // 计算时间差（如果是第一次则为currentTime - 0）
        long timeDiff = currentTime - lastUpdateTime;
        Log.d(TAG, packageName + "-detectAutoUpdateBehavior timeDiff=" + timeDiff + "--lastUpdateTimeObj=" + lastUpdateTimeObj + "--state.getState()=" + state.getState());
        // 更新记录（无论是否首次都更新时间戳）
        mLastUpdateTime.put(packageName, currentTime);

        // 仅当不是第一次更新时才进行判断
        if (state.getState() == PlaybackState.STATE_PLAYING) {
            if (lastUpdateTimeObj != null && timeDiff < AUTO_UPDATE_THRESHOLD_MS) {
                mAutoUpdateControllers.put(packageName, true);
            } else {
                mAutoUpdateControllers.put(packageName, false);
            }
        }
    }

    // 安全获取方法
    public boolean isAutoUpdate(String packageName) {
        return !Boolean.TRUE.equals(mAutoUpdateControllers.get(packageName));
    }

    private boolean shouldManualUpdate(String packageName) {
        if (mAutoUpdateControllers == null) return false;
        boolean hasPackage = mAutoUpdateControllers.containsKey(packageName);
        boolean isAutoUpdate = isAutoUpdate(packageName);
        Log.d(TAG, packageName + " hasPackage = " + hasPackage + "--isAutoUpdate=" + isAutoUpdate);
        return hasPackage && isAutoUpdate;
    }

    private void startManualUpdate(MediaController controller) {
        if (controller == null) return;
        String packageName = controller.getPackageName();

        // 如果已经有更新任务在运行，则不再添加
        if (mUpdateRunnables.containsKey(packageName)) {
            return;
        }

        Runnable updateTask = new Runnable() {
            @Override
            public void run() {
                PlaybackState state = controller.getPlaybackState();
                if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                    handlePlayingTime(controller);
                    // 每秒更新一次
                    // 动态计算下次刷新时间
                    float speed = state.getPlaybackSpeed();
                    float validSpeed = speed <= 0 ? 1.0f : speed;
                    long refreshInterval = Math.max(16, (long) (1000 / validSpeed));
                    //Log.d(TAG, "startManualUpdate-updateTask: " + packageName+"--refreshInterval="+refreshInterval+"--speed="+speed);
                    //mUpdateHandler.postDelayed(this, refreshInterval);
                }
            }
        };

        //mUpdateRunnables.put(packageName, updateTask);
        //mUpdateHandler.post(updateTask);
        //Log.d(TAG, "Started manual update for " + packageName);
    }

    public void cleanup() {
        // 移除所有手动更新任务
        for (Runnable task : mUpdateRunnables.values()) {
            mUpdateHandler.removeCallbacks(task);
        }
        mUpdateRunnables.clear();
        mAutoUpdateControllers.clear();
        mLastUpdateTime.clear();
    }

    private void handlePlaybackStateChange(MediaController controller) {
        if (controller == null) return;
        PlaybackState playbackState = controller.getPlaybackState();
        if (playbackState == null) {
            return;  // 如果没有播放状态，直接返回
        }
        int state = playbackState.getState();
        String packageName = controller.getPackageName();
        Log.d(TAG, "handlePlaybackStateChange from " + packageName + ", state: " + state);

        //sendPlayStateToWidget(state == PlaybackState.STATE_PLAYING);

        detectAutoUpdateBehavior(controller, playbackState);

        switch (state) {
            case PlaybackState.STATE_PLAYING:
                if (shouldManualUpdate(packageName)) {
                    startManualUpdate(controller);
                } else {
                    cleanup();
                    handlePlayingTime(controller);
                }
                break;

            case PlaybackState.STATE_PAUSED:
            case PlaybackState.STATE_STOPPED:
            case PlaybackState.STATE_BUFFERING:
                cleanup();
                handleNonPlayingState(controller, playbackState);
                break;

            default:
                Log.d(TAG, "Unhandled playback state: " + state);
        }
    }

    private void handlePlayingTime(MediaController controller) {
        if (controller == null) return;
        // 获取当前播放状态
        PlaybackState playbackState = controller.getPlaybackState();
        if (playbackState == null) {
            return;  // 如果没有播放状态，直接返回
        }
        boolean playing = playbackState.getState() == PlaybackState.STATE_PLAYING;
        if (!playing) return;
        mCurrentPosition = playbackState.getPosition();

        // 确保我们有最新的元数据
        MediaMetadata metadata = controller.getMetadata();

        if (metadata != null) {
            mDuration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        }

        float speed = playbackState.getPlaybackSpeed();
        // 获取状态最后更新时间
        long updateTime = playbackState.getLastPositionUpdateTime();
        long currentTime = SystemClock.elapsedRealtime();

        // 计算时间差并更新位置
        if (updateTime > 0) {
            long timeDiff = currentTime - updateTime;
            mCurrentPosition += (long) (timeDiff * speed);

            if (mDuration > 0) {
                mCurrentPosition = Math.min(mCurrentPosition, mDuration);
            }

            // 这里可以使用currentPosition来更新UI或其他逻辑
            updatePlaybackPosition(playing, mCurrentPosition, mDuration);
        }
        Log.d(TAG, "handlePlayingTime -started - mCurrentPosition: " + mCurrentPosition + "--mPlayingPackageName=" + getPlayingPackageName());

    }

    private void handleNonPlayingState(MediaController controller, PlaybackState state) {
        Log.d(TAG, "handleNonPlayingState controller=" + controller + "--state=" + state);
        mCurrentPosition = state.getPosition();
    }

    private void handleMetadataChange(MediaController controller, MediaMetadata metadata) {
        Log.d(TAG, "handleMetadataChange--metadata:" + metadata);
        mDuration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        sendMusicInfoToWidget(controller, metadata);
    }

    private void updatePlaybackPosition(boolean playing, long position, long duration) {
        if (playing && position <= mDuration) {
            Bundle bundle = createPlaybackDataBundle(position, duration);
            sendDataToAwellApi(bundle);
            //Log.d(TAG, "Updating playback position: " + position);
        }
    }

    private void sendMediaPlayInfoToWidget(MediaController controller, boolean isPlaying) {
        Bundle bundle = new Bundle();
        bundle.putString(AwellTool.STATUS_ACCEPT, AwellTool.MEDIA_PLAY);
        Log.i(TAG, "sendMediaPlayInfoToWidget---MUSIC_MEDIA_PLAY : " + isPlaying + "--controller.getPackageName()=" + controller.getPackageName());
        bundle.putString(AwellTool.VALUE_M1, controller.getPackageName());
        bundle.putString(AwellTool.VALUE_M2, isPlaying ? "start" : "stop");
        bundle.putInt(AwellTool.VALUE_M3, 3);
        bundle.putInt(AwellTool.VALUE_M4, MusicWidget.OTHER_MUSIC);
        sendDataToAwellApi(bundle);
    }

    private Bundle createPlaybackDataBundle(long position, long duration) {
        Bundle bundle = new Bundle();
        bundle.putString(AwellTool.STATUS_ACCEPT, MusicWidget.OTHER_MUSIC_TIME);
        bundle.putLong(AwellTool.VALUE_M1, position);
        bundle.putLong(AwellTool.VALUE_M2, duration);
        //Log.d(TAG, "createPlaybackDataBundle PLAY_TIME: " + position+":"+mDuration);
        return bundle;
    }

    private void sendMusicInfoToWidget(MediaController controller, MediaMetadata metadata) {
        Bundle bundle = new Bundle();
        bundle.putString(AwellTool.STATUS_ACCEPT, MusicWidget.OTHER_MUSIC_PLAYNAME);
        if (controller == null || metadata == null) {
            bundle.putString(AwellTool.VALUE_M1, mContext.getResources().getString(R.string.click_play_music));
            bundle.putString(AwellTool.VALUE_M2, mContext.getResources().getString(R.string.unknow_song_artist));
            //bundle.putString(AwellTool.VALUE_M3, metadata.getString(MediaMetadata.METADATA_KEY_ALBUM));
            bundle.putInt(AwellTool.VALUE_M4, MusicWidget.MUSIC);
        } else {
            bundle.putString(AwellTool.VALUE_M1, metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
            bundle.putString(AwellTool.VALUE_M2, metadata.getString(MediaMetadata.METADATA_KEY_ARTIST));
            bundle.putString(AwellTool.VALUE_M3, metadata.getString(MediaMetadata.METADATA_KEY_ALBUM));
            //if (controller.getPackageName().contains("com.zjinnova.zlink")) {
            //    bundle.putInt(AwellTool.VALUE_M4, MusicWidget.CARPLAY);
            //} else if (controller.getPackageName().contains("kwmusiccar")) {
            //    bundle.putInt(AwellTool.VALUE_M4, MusicWidget.KUMUSIC);
            //} else if (controller.getPackageName().contains("com.awell.bluetooth") || controller.getPackageName().contains("/system/bin/gocsdk")) {//蓝牙音乐
            //    bundle.putInt(AwellTool.VALUE_M4, MusicWidget.BT);
            //} else {
            bundle.putInt(AwellTool.VALUE_M4, MusicWidget.OTHER_MUSIC);
            //}
            Log.d(TAG, "sendMusicInfoToWidget title: " + metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
        }
        sendDataToAwellApi(bundle);
    }

    private void notifyHostAlbumArtUpdate(String uri) {
        Bundle bundle = new Bundle();
        bundle.putString(AwellTool.STATUS_ACCEPT, MusicWidget.OTHER_MUSIC_PLAY_IMAGE);
        bundle.putString(AwellTool.VALUE_M3, uri);
        sendDataToAwellApi(bundle);
    }

    private void sendPlayStateToWidget(boolean isPlaying) {
        Bundle bundle = new Bundle();
        bundle.putString(AwellTool.STATUS_ACCEPT, MusicWidget.OTHER_MUSIC_PLAYSTATUS);
        bundle.putBoolean(AwellTool.VALUE_M1, isPlaying);
        sendDataToAwellApi(bundle);
    }

    public void sendDataToAwellApi(Bundle bundle) {
        try {
            if (mAwellApi != null) {
                mAwellApi.sendData(bundle);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to send data to AwellApi", e);
        }
    }

    public MediaController getmMediaController() {
        return mMediaController;
    }
}