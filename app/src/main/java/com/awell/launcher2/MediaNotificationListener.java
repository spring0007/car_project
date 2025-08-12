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

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.awell.aidl.awellautointer.IAwellCallBack;
import com.awell.aidl.awellface.IAwellApi;
import com.awell.ctrlview.MusicWidget;
import com.awell.launcher.R;
import com.awell.library.AwellTool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediaNotificationListener/* extends ServiceNotificationListenerService*/ {
    private static final String TAG = "MediaNotificationListenerLog";
    // 常量定义
    private static final long UPDATE_INTERVAL_MS = 500;

    // 依赖组件
    private Context mContext;
    private IAwellApi mAwellApi;
    private MediaSessionManager mMediaSessionManager;
    private MediaSessionManager.OnActiveSessionsChangedListener mSessionsListener;

    // 状态变量
    private final CopyOnWriteArrayList<MediaController> mControllers = new CopyOnWriteArrayList<>();
    private final List<MediaController.Callback> mCallbacks = new ArrayList<>();

    private int mPlayState = 0;
    private int mActivePlaybackCount = 0;
    private long mCurrentPosition = 0;
    private long mDuration = 0;
    public static String mPlayingPackageName;

    // 线程处理
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private MediaController mMediaController;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private Handler mHandler = new Handler();
    private Map<String, Boolean> mAutoUpdateControllers = new HashMap<>();

    // 记录每个控制器的最后更新时间戳
    private Map<String, Long> mLastUpdateTime = new HashMap<>();

    // 手动更新任务集合
    private Map<String, Runnable> mUpdateRunnables = new HashMap<>();

    private Handler mUpdateHandler = new Handler(Looper.getMainLooper());

    private static final long AUTO_UPDATE_THRESHOLD_MS = 500;
    private final MediaController.Callback mMediaControllerCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            Log.d(TAG, "onPlaybackStateChanged--state: " + state);
            mExecutor.execute(() -> {
                if (state != null) {
                    boolean isPlay = state.getState() == PlaybackState.STATE_PLAYING;
                    boolean isPause = state.getState() == PlaybackState.STATE_PAUSED;
                    Log.d(TAG, "onPlaybackStateChanged--state: " + state.getState() + " from " + mMediaController.getPackageName() + "--mPlayState=" + mPlayState);
                    if (isPlay || isPause) {
                        if (mPlayState != state.getState()) {
                            if (isPlay) {
                                //mMainHandler.removeCallbacksAndMessages(null);
                                mMainHandler.postDelayed(() -> {
                                    sendMediaPlayInfoToWidget(mMediaController, isPlay);
                                    MediaMetadata metadata = mMediaController.getMetadata();
                                    if (metadata != null) {
                                        Log.d(TAG, "onPlaybackStateChanged-Title: " + metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
                                        Log.d(TAG, "onPlaybackStateChanged-Artist: " + metadata.getString(MediaMetadata.METADATA_KEY_ARTIST));
                                        handleMetadataChange(mMediaController, metadata);
                                    }
                                }, 100);
                            }
                        }
                        handlePlaybackStateChange(mMediaController);
                    }
                    mPlayState = state.getState();
                }
            });
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            mPlayState = 0;
            Log.d(TAG, "onMetadataChanged---metadata: " + metadata);
            if (metadata != null) {
                mMainHandler.postDelayed(() -> {
                    PlaybackState state = mMediaController.getPlaybackState();
                    Log.d(TAG, "onMetadataChanged-state: " + state + " from " + mMediaController.getPackageName() + "--mPlayState=" + mPlayState);
                    boolean playing = false;
                    if (state != null) {
                        playing = (state.getState() == PlaybackState.STATE_PLAYING);
                    }

                    if (metadata != null) {
                        Log.d(TAG, "onMetadataChanged---METADATA_KEY_TITLE: " + metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
                        Log.d(TAG, "onMetadataChanged---METADATA_KEY_ARTIST: " + metadata.getString(MediaMetadata.METADATA_KEY_ARTIST));
                        Log.d(TAG, "onMetadataChanged---METADATA_KEY_ALBUM: " + metadata.getString(MediaMetadata.METADATA_KEY_ALBUM));
                        Log.d(TAG, "onMetadataChanged---METADATA_KEY_DISPLAY_TITLE: " + metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE));
                        Log.d(TAG, "onMetadataChanged---METADATA_KEY_DISPLAY_SUBTITLE: " + metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE));
                        Log.d(TAG, "onMetadataChanged---METADATA_KEY_AUTHOR: " + metadata.getString(MediaMetadata.METADATA_KEY_AUTHOR));
                        Log.d(TAG, "onMetadataChanged---METADATA_KEY_WRITER: " + metadata.getString(MediaMetadata.METADATA_KEY_WRITER));
                        Log.d(TAG, "onMetadataChanged---METADATA_KEY_COMPOSER: " + metadata.getString(MediaMetadata.METADATA_KEY_COMPOSER));
                        Log.d(TAG, "onMetadataChanged---METADATA_KEY_ALBUM_ART: " + metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART));
                        Log.d(TAG, "onMetadataChanged---METADATA_KEY_ALBUM_ARTIST: " + metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST));
                    }
                    if (playing) {
                        sendMediaPlayInfoToWidget(mMediaController, playing);
                        handleMetadataChange(mMediaController, metadata);
                        if (!mUpdateRunnables.containsKey(mMediaController.getPackageName())) {
                            if (shouldManualUpdate(mMediaController.getPackageName())) {
                                //stopManualUpdate(mMediaController);
                                startManualUpdate(mMediaController);
                            } else {
                                cleanup();
                                handlePlayingTime(mMediaController);
                            }
                        }
                    }
                }, 300);
            }
        }

        @Override
        public void onSessionDestroyed() {
            Log.d(TAG, "--onSessionDestroyed- ");
            cleanup();
        }
    };

    public void initDependencies(Context context) {
        mContext = context;
        IBinder binder = ServiceManager.getService("AwellAutoApi");
        mAwellApi = binder != null ? IAwellApi.Stub.asInterface(binder) : null;

        mMediaSessionManager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        mSessionsListener = new MediaSessionManager.OnActiveSessionsChangedListener() {
            @Override
            public void onActiveSessionsChanged(List<MediaController> controllers) {
                // 处理会话变化
                mPlayState = 0;
                Log.d(TAG, "onActiveSessionsChanged--controllers:" + controllers.size() + "--mPlayState=" + mPlayState);
                if (controllers.size() <= 0) {
                    mPlayingPackageName = null;
                    //mEntryPackageName=null;
                    cleanup();
                    sendPlayStateToWidget(false);
                    //sendMediaPlayInfoToWidget(null,false);
                    //sendMusicInfoToWidget(null,null);
                    //Bundle bundle = createPlaybackDataBundle(0,0);
                    //sendDataToAwellApi(bundle);
                }
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

    private Runnable mPollingRunnable = new Runnable() {
        @Override
        public void run() {
            updateMediaController();
        }
    };

    public void removeCallbacks() {
        Log.d(TAG, "removeCallbacks--mHandler:" + mHandler + "--mPlayingPackageName=" + mPlayingPackageName);
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mMediaControllerCallback);
        }
    }

    public void startCallbacks() {
        Log.d(TAG, "startCallbacks--mHandler:" + mHandler + "--mPlayingPackageName=" + mPlayingPackageName);
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler.postDelayed(mPollingRunnable, UPDATE_INTERVAL_MS);
        }
    }

    public void setPlayingPackage(String pkg) {
        mPlayingPackageName = pkg;
    }

    public String getCurrentPlayingPackage() {
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

    public Bitmap getPlayingAlbumBitmap() {
        Bitmap albumArt = null;
        if (mMediaController == null) return albumArt;
        // 获取当前播放状态
        PlaybackState playbackState = mMediaController.getPlaybackState();
        if (playbackState == null) {
            return albumArt;  // 如果没有播放状态，直接返回
        }
        boolean playing = playbackState.getState() == PlaybackState.STATE_PLAYING;//(state.getState()==PlaybackState.STATE_PLAYING);
        if (!playing) return albumArt;
        // 确保我们有最新的元数据
        MediaMetadata metadata = mMediaController.getMetadata();

        // 获取高分辨率专辑图（Bitmap）
        albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
        Log.d(TAG, "Unhandled playback state: albumArt00=" + albumArt);
        // 如果没有高分辨率图，尝试低分辨率图
        if (albumArt == null) {
            albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            Log.d(TAG, "Unhandled playback state: albumArt11=" + albumArt);
        }

        // 如果仍然没有，检查是否有显示图标（如通知栏小图标）
        if (albumArt == null) {
            albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
            Log.d(TAG, "Unhandled playback state: albumArt22=" + albumArt);
        }
        return albumArt;
    }

    public Uri getPlayingAlbumUri() {
        Uri albumArtUri = null;
        if (mMediaController == null) return albumArtUri;
        // 获取当前播放状态
        PlaybackState playbackState = mMediaController.getPlaybackState();
        if (playbackState == null) {
            return albumArtUri;  // 如果没有播放状态，直接返回
        }
        boolean playing = playbackState.getState() == PlaybackState.STATE_PLAYING;//(state.getState()==PlaybackState.STATE_PLAYING);
        if (!playing) return albumArtUri;
        // 确保我们有最新的元数据
        MediaMetadata metadata = mMediaController.getMetadata();

        MediaDescription description = metadata.getDescription();
        if (description != null) {
            albumArtUri = description.getIconUri();
            Log.d(TAG, "Unhandled playback state: albumArtUri=" + albumArtUri);
            if (albumArtUri != null) {
                // 使用 Glide/Picasso 加载图片
                //Glide.with(context)
                //        .load(albumArtUri)
                //        .into(imageView);
            }
        }
        return albumArtUri;
    }


    private void updateMediaController() {
        MediaController activeController = null;
        //ComponentName componentName = new ComponentName(mContext, NotificationListenerService.class);
        Log.d(TAG, "------updateMediaController------");
        // 获取当前正在播放的controller
        for (MediaController controller : mMediaSessionManager.getActiveSessions(null)) {
            PlaybackState state = controller.getPlaybackState();
            Log.d(TAG, "updateMediaController package list: " + controller.getPackageName() + "--mPlayingPackageName=" + mPlayingPackageName + "--mPlayState=" + mPlayState + "--state=" + state);
            if (state != null && state.getState() == PlaybackState.STATE_PLAYING || (mPlayingPackageName != null && controller.getPackageName().equals(mPlayingPackageName))) {
                mPlayingPackageName = controller.getPackageName();
                activeController = controller;
                sendMediaPlayInfoToWidget(controller, state.getState() == PlaybackState.STATE_PLAYING);
                break;
            }
        }

        if (activeController == null) {
            Log.d(TAG, "No active playing controller found");
            return;
        }

        Log.d(TAG, "Active controller package: " + activeController.getPackageName());

        // 取消之前的回调
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mMediaControllerCallback);
        }

        mMediaController = activeController;

        mMediaController.registerCallback(mMediaControllerCallback);

        handlePlaybackStateChange(mMediaController);

        MediaMetadata currentMetadata = mMediaController.getMetadata();
        if (currentMetadata != null) {
            handleMetadataChange(mMediaController, currentMetadata);
        }
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
                //Log.d(TAG, packageName + " confirmed auto-update");
            } else {
                mAutoUpdateControllers.put(packageName, false);
                //Log.d(TAG, packageName + " requires manual update");
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
                    mUpdateHandler.postDelayed(this, refreshInterval);
                }
            }
        };

        mUpdateRunnables.put(packageName, updateTask);
        mUpdateHandler.post(updateTask);
        //Log.d(TAG, "Started manual update for " + packageName);
    }

    private void stopManualUpdate(MediaController controller) {
        String packageName = controller.getPackageName();
        Runnable task = mUpdateRunnables.get(packageName);
        if (task != null) {
            mUpdateHandler.removeCallbacks(task);
            mUpdateRunnables.remove(packageName);
            //Log.d(TAG, "Stopped manual update for " + packageName);
        }
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
        sendPlayStateToWidget(state == PlaybackState.STATE_PLAYING);

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
        //mPlayingPackageName = controller.getPackageName();
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
        Log.d(TAG, "handlePlayingTime 000---started - position: " + mCurrentPosition + "--updateTime=" + updateTime + "--speed=" + speed + "--currentTime=" + currentTime);
        // 计算时间差并更新位置
        if (updateTime > 0) {
            long timeDiff = currentTime - updateTime;
            mCurrentPosition += (long) (timeDiff * speed);

            if (mDuration > 0) {
                mCurrentPosition = Math.min(mCurrentPosition, mDuration);
            }

            // 这里可以使用currentPosition来更新UI或其他逻辑
        }
        Log.d(TAG, "handlePlayingTime 111---started - position: " + mCurrentPosition + "--mPlayingPackageName=" + mPlayingPackageName);
        updatePlaybackPosition(playing, mCurrentPosition, mDuration);

    }

    private void handleNonPlayingState(MediaController controller, PlaybackState state) {
        Log.d(TAG, "handleNonPlayingState controller=" + controller + "--state=" + state);
        mCurrentPosition = state.getPosition();
    }

    private void handleMetadataChange(MediaController controller, MediaMetadata metadata) {
        Log.d(TAG, "handleMetadataChange--metadata:" + metadata);
        mCurrentPosition = 0;
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
        /*if(controller==null){
            Log.i(TAG, "sendMediaPlayInfoToWidget---MUSIC_MEDIA_PLAY : " + isPlaying);
            bundle.putString(AwellTool.VALUE_M1, "com.awell.localmusic");
            bundle.putString(AwellTool.VALUE_M2, isPlaying ? "start" : "stop");
            bundle.putInt(AwellTool.VALUE_M3, 3);
            bundle.putInt(AwellTool.VALUE_M4, MusicWidget.MUSIC);
        }else {*/
        Log.i(TAG, "sendMediaPlayInfoToWidget---MUSIC_MEDIA_PLAY : " + isPlaying + "--controller.getPackageName()=" + controller.getPackageName());
        bundle.putString(AwellTool.VALUE_M1, controller.getPackageName());
        bundle.putString(AwellTool.VALUE_M2, isPlaying ? "start" : "stop");
        bundle.putInt(AwellTool.VALUE_M3, 3);
        bundle.putInt(AwellTool.VALUE_M4, MusicWidget.OTHER_MUSIC);
        //}
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
        //Log.d(TAG, "sendMusicInfoToWidget ARTIST: " + metadata.getString(MediaMetadata.METADATA_KEY_ARTIST));
        //Log.d(TAG, "sendMusicInfoToWidget ALBUM): " + metadata.getString(MediaMetadata.METADATA_KEY_ALBUM));
        //Log.d(TAG, "sendMusicInfoToWidget getPackageName: " + controller.getPackageName());
        sendDataToAwellApi(bundle);
    }

    private void sendPlayStateToWidget(boolean isPlaying) {
        Bundle bundle = new Bundle();
        bundle.putString(AwellTool.STATUS_ACCEPT, MusicWidget.OTHER_MUSIC_PLAYSTATUS);
        bundle.putBoolean(AwellTool.VALUE_M1, isPlaying);
        sendDataToAwellApi(bundle);
    }

    private void notifyClientAlbumPath(boolean isPlaying) {
        Log.d(TAG, "notifyClientAlbumPath isPlaying: " + isPlaying);
        Bundle bundle = new Bundle();
        bundle.putString(AwellTool.STATUS_ACCEPT, MusicWidget.OTHER_MUSIC_PLAY_IMAGE);
        bundle.putBoolean(AwellTool.VALUE_M1, isPlaying);
        sendDataToAwellApi(bundle);
    }

    private void sendDataToAwellApi(Bundle bundle) {
        try {
            if (mAwellApi != null) {
                mAwellApi.sendData(bundle);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to send data to AwellApi", e);
        }
    }
}