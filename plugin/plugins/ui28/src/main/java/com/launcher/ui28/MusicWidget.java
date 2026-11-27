package com.launcher.ui28;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.awell.control.AwellMediaControl;
import com.awell.launcher2.EarqueeTextView;
import com.awell.launcher2.IconCache;
import com.awell.library.AwellTool;
import com.awell.utils.LogUtil;

import java.util.Arrays;

public class MusicWidget extends ConstraintLayout implements OnClickListener {
    private static final String TAG = "MusicWidget";

    public static final int MUSIC = 0;
    public static final int BT = 1;
    public static final int KUMUSIC = 2;
    public static final int CARPLAY = 3;
    public static final int OTHER_MUSIC = 4;

    private Context mContext;
    private EarqueeTextView mMusicNameTextView, mArtistNameTextView;
    private ImageView mPlayStateImageView;
    private SeekBar mBar;

    private boolean musicState = false;
    private int currentMedia = MUSIC;
    private String currentPlayingPackage = null;
    private AwellMediaControl mediaControl;

    public MusicWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }

    public void setMediaLibrary(AwellMediaControl mediaControl) {
        this.mediaControl = mediaControl;
    }

    /**
     * 绑定子 View 并同步初始播放状态,需在 setMediaLibrary 之后调用
     */
    public void init() {
        mContext = getContext();
        mMusicNameTextView = findViewById(R.id.music_widget_music_name);
        mArtistNameTextView = findViewById(R.id.music_artist);

        mBar = findViewById(R.id.music_widget_seekbars);
        mBar.setOnClickListener(this);
        mBar.setMax(100);
        mBar.setProgress(0);

        mPlayStateImageView = findViewById(R.id.music_widget_play);
        mPlayStateImageView.setOnClickListener(this);
        findViewById(R.id.music_widget_pre).setOnClickListener(this);
        findViewById(R.id.music_widget_next).setOnClickListener(this);
        findViewById(R.id.layout_music_widget).setOnClickListener(this);

        setCurMusicState("true".equals(mediaControl.sendStrToHost(AwellTool.MUSIC.GET_STATE)), MUSIC);
    }

    public void getCarPlayData(String zlinStatus, String phoneMode) {
        if (zlinStatus == null) {
            return;
        }
        switch (zlinStatus) {
            case "DISCONNECT":
            case "MAIN_AUDIO_STOP":
                setCurMusicState(false, OTHER_MUSIC);
                break;
            case "MAIN_AUDIO_START":
                setCurMusicState(true, OTHER_MUSIC);
                break;
            case "ACTION_ZJ_PHONEFOUND":
                // 手机投屏连接成功,播放状态由媒体回调更新,这里无需额外处理
                break;
            default:
                break;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.music_widget_next) {
            sendNext();
        } else if (id == R.id.music_widget_pre) {
            sendPrevious();
        } else if (id == R.id.music_widget_play) {
            togglePlay();
        } else if (id == R.id.layout_music_widget) {
            openCurrentPlayer();
        }
    }

    private void sendNext() {
        switch (currentMedia) {
            case MUSIC:
                mediaControl.sendStrToHost(AwellTool.MUSIC.NEXT);
                break;
            case BT:
                mediaControl.sendStrToHost(AwellTool.BT.NEXT);
                break;
            default:
                mediaControl.sendNextToHost();
                break;
        }
    }

    private void sendPrevious() {
        switch (currentMedia) {
            case MUSIC:
                mediaControl.sendStrToHost(AwellTool.MUSIC.PREVIOUS);
                break;
            case BT:
                mediaControl.sendStrToHost(AwellTool.BT.PREVIOUS);
                break;
            default:
                mediaControl.sendPreToHost();
                break;
        }
    }

    private void togglePlay() {
        switch (currentMedia) {
            case MUSIC:
                mediaControl.sendStrToHost(musicState ? AwellTool.MUSIC.PAUSE : AwellTool.MUSIC.PLAY);
                break;
            case BT:
                boolean btPlaying = "true".equals(mediaControl.sendStrToHost(AwellTool.BT.GET_STATE));
                mediaControl.sendStrToHost(btPlaying ? AwellTool.BT.PAUSE : AwellTool.BT.PLAY);
                break;
            default:
                mediaControl.sendTogglePlayPause();
                break;
        }
    }

    private void openCurrentPlayer() {
        switch (currentMedia) {
            case MUSIC:
                launchApp("com.awell.localmusic");
                break;
            case BT:
                openBluetoothPage();
                break;
            default:
                if (!TextUtils.isEmpty(currentPlayingPackage)) {
                    launchAppByPackageName(mContext, currentPlayingPackage);
                }
                break;
        }
    }

    private void openBluetoothPage() {
        try {
            Intent intent = new Intent("com.awell.bluetooth");
            intent.setClassName("com.awell.bluetooth", "com.awell.bluetooth.MainActivity");
            intent.putExtra("bt_preference_key", 3);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Bluetooth activity not found", e);
        }
    }

    private void launchApp(String packageName) {
        Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent == null) {
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Arrays.asList(IconCache.WorkSpacePackageName).contains(packageName)) {
            String key = packageName.contains("com.autonavi") ? "boot_apk1" : "boot_apk2";
            Settings.System.putString(mContext.getContentResolver(), key, packageName);
        }
        mContext.startActivity(intent);
    }

    public void setMusicNameTextView(String musicName, int flag) {
        if (mMusicNameTextView != null && currentMedia == flag) {
            mMusicNameTextView.setText(TextUtils.isEmpty(musicName)
                    ? getResources().getString(R.string.click_play_music) : musicName);
        }
    }

    public void setArtistNameTextView(String artistName, int flag) {
        if (mArtistNameTextView != null && currentMedia == flag) {
            mArtistNameTextView.setText(TextUtils.isEmpty(artistName)
                    ? getResources().getString(R.string.music_artist) : artistName);
        }
    }

    public void setMusicSeekBar(int curProgress, int totalProgress, int flag) {
        if (mBar != null && currentMedia == flag) {
            mBar.setMax(totalProgress);
            mBar.setProgress(curProgress);
        }
    }

    public void setCurMusicState(boolean musicState, int flag) {
        if (currentMedia == flag) {
            setCurMusicState(musicState);
        }
    }

    /**
     * 根据当前播放状态切换按钮图标与跑马灯
     */
    private void setCurMusicState(boolean musicState) {
        setTextEarquee(musicState);
        if (mPlayStateImageView == null  || musicState == this.musicState) {
            return;
        }
        this.musicState = musicState;

        mPlayStateImageView.setImageResource(musicState
                ? R.drawable.sf_music_zanting : R.drawable.sf_music_bofang);
    }

    /**
     * 切换当前音频播放器的播放控制器
     */
    public void switchMediaController(String packName, String status, int mediaType) {
        if (!"start".equals(status)) {
            return;
        }
        currentPlayingPackage = packName;
        if (packName.contains("localmusic")) {
            currentMedia = MUSIC;
        } else if ((packName.contains("com.awell.bluetooth") || packName.contains("/system/bin/gocsdk"))
                && mediaType == AudioManager.STREAM_MUSIC) {
            currentMedia = BT;
        } else if (mediaType == AudioManager.STREAM_MUSIC) {
            currentMedia = OTHER_MUSIC;
        }
        setCurMusicState(true);
    }

    public static boolean launchAppByPackageName(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(context, context.getString(R.string.app_not_installed), Toast.LENGTH_SHORT).show();
            return false;
        }

        Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
        if (launchIntent == null) {
            Toast.makeText(context, context.getString(R.string.cannot_launch_app), Toast.LENGTH_SHORT).show();
            return false;
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(launchIntent);
            return true;
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, context.getString(R.string.cannot_launch_app), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void setTextEarquee(boolean earquee) {
        if (mMusicNameTextView != null) {
            mMusicNameTextView.setMarqueeEnabled(earquee);
        }
        if (mArtistNameTextView != null) {
            mArtistNameTextView.setMarqueeEnabled(earquee);
        }
    }
}
