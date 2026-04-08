package com.launcher.zy_ui01;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.awell.control.AwellMediaControl;
import com.awell.launcher2.EarqueeTextView;
import com.awell.launcher2.IconCache;
import com.awell.library.AwellTool;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;


public class MusicWidget extends ConstraintLayout implements OnClickListener {
    private static final String TAG = "MusicWidgetLog";

    private Context mContext;
    private EarqueeTextView mMusicNameTextView, mArtistNameTextView;
    private TextView mCurTimeTextView, mTotalTimeTextView;
    private ImageView ivLoadnim;
    private ImageView mPlayStateImageView, musicPreIv, musicNextIv;
    private SeekBar mBar = null;
    //private KWAPI kwapi;
    private int dayNight = 0;
    private boolean musicState = false;
    public final static int MUSIC = 0;
    public final static int BT = 1;
    public final static int KUMUSIC = 2;
    public final static int CARPLAY = 3;
    public final static int OTHER_MUSIC = 4;

    private int currentMedia = MUSIC;//0 music   1 bt   2 kw music  3 carplay
    private String currentPlayingPackage = null;

    private LinearLayout ll_control_layout_music, ll_name_layout_music;
    private RelativeLayout ll_time_layout_music;

    private AwellMediaControl mediaControl;
    private ObjectAnimator mObjectAnimator = null;

    private final RequestListener<Drawable> listener = new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
            if (e != null) {
                Log.e(TAG, "Glide load uri failed!! " + e.getMessage());
                return true;
            }
            return false;
        }

        @Override
        public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
            return false;
        }
    };

//    private MediaNotificationListener mediaNotificationListener = null;

    public MusicWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }

    /**
     *
     */
    public void setActivity(Context context, View view) {
        findViews(context, view);
    }

    public void setMediaLibrary(AwellMediaControl mediaControl) {
        this.mediaControl = mediaControl;
    }

    private void findViews(Context context, View view) {
        this.mContext = context;
        ll_control_layout_music = view.findViewById(R.id.ll_control_layout_music);
        ll_name_layout_music = view.findViewById(R.id.ll_name_layout_music);
        ll_time_layout_music = view.findViewById(R.id.ll_time_layout_music);

        mMusicNameTextView = view.findViewById(R.id.music_name);

        mArtistNameTextView = view.findViewById(R.id.music_artist);

        mBar = view.findViewById(R.id.music_widget_seekbars);
        mBar.setOnClickListener(this);
        mBar.setMax(100);
        mBar.setProgress(0);

        mCurTimeTextView = view.findViewById(R.id.music_widget_cur_time_textview);
        mTotalTimeTextView = view.findViewById(R.id.music_widget_total_time_textview);

        mPlayStateImageView = view.findViewById(R.id.music_widget_play);
        mPlayStateImageView.setOnClickListener(this);
        musicPreIv = view.findViewById(R.id.music_widget_pre);
        musicPreIv.setOnClickListener(this);
        musicNextIv = view.findViewById(R.id.music_widget_next);
        musicNextIv.setOnClickListener(this);

        view.findViewById(R.id.layout_music_widget).setOnClickListener(this);
       // view.findViewById(R.id.img_song_art_bg).setOnClickListener(this);

        ivLoadnim = view.findViewById(R.id.img_song_art);
       // ivLoadnim.setOnClickListener(this);
        mObjectAnimator = ObjectAnimator.ofFloat(ivLoadnim, "rotation", 0f, 360f);
        mObjectAnimator.setInterpolator(new LinearInterpolator());
        stopLoadAnim();

        setImageIcon(currentMedia);
        setCurMusicState(mediaControl.sendStrToHost(AwellTool.MUSIC.GET_STATE).equals("true"), MUSIC);

    }

    public void setCarPlayData(String zlinStatus, String phoneMode) {
        Log.e(TAG, "getCarPlayData zlinStatus:" + zlinStatus + " phoneMode:" + phoneMode);
        if ("CONNECTED".equals(zlinStatus)) {

            setImageIcon(currentMedia);
        } else if ("DISCONNECT".equals(zlinStatus)) {
            setCurMusicState(false, OTHER_MUSIC);

        } else if ("ACTION_ZJ_PHONEFOUND".equals(zlinStatus)) {
            if (phoneMode.contains("carplay")) {
                //currentMedia = CARPLAY;
                setImageIcon(currentMedia);
            } else if (phoneMode.contains("auto")) {
                //currentMedia = CARPLAY;
//                if (icon_music_img != null) {
//                    icon_music_img.setImageResource(R.drawable.icon_autoplay_img);
//                }
            }
        } else if (zlinStatus.equals("MAIN_AUDIO_STOP")) {
            setCurMusicState(false, OTHER_MUSIC);
        } else if (zlinStatus.equals("MAIN_AUDIO_START")) {
            Log.d(TAG, "getCarPlayData-currentMedia: " + currentMedia);
            setCurMusicState(true, OTHER_MUSIC);
        }
    }

    public void getCarPlayData(String zlinStatus, String phoneMode) {
        Log.e(TAG, "getCarPlayData zlinStatus:" + zlinStatus + " phoneMode:" + phoneMode);
        if ("CONNECTED".equals(zlinStatus)) {
            //zlinkCarPlaySocketThread = new SocketThread(1555, carPlayhandler, phoneMode);
            //currentMedia = CARPLAY;
            setImageIcon(currentMedia);
        } else if ("DISCONNECT".equals(zlinStatus)) {
            setCurMusicState(false, OTHER_MUSIC);
            //if(zlinkCarPlaySocketThread!=null) {
            //    zlinkCarPlaySocketThread.disSocket();
            //}
        } else if ("ACTION_ZJ_PHONEFOUND".equals(zlinStatus)) {
            if (phoneMode.contains("carplay")) {
                //currentMedia = CARPLAY;
                setImageIcon(currentMedia);
            } else if (phoneMode.contains("auto")) {
                //currentMedia = CARPLAY;
//                if (icon_music_img != null) {
//                    icon_music_img.setImageResource(com.awell.launcher.library.R.drawable.icon_autoplay_img);
//                }
            }
        } else if (zlinStatus.equals("MAIN_AUDIO_STOP")) {
            setCurMusicState(false, OTHER_MUSIC);
        } else if (zlinStatus.equals("MAIN_AUDIO_START")) {
            //currentMedia = CARPLAY;
            Log.d(TAG, "getCarPlayData-currentMedia: " + currentMedia);
            setCurMusicState(true, OTHER_MUSIC);
        }
    }


    int[] sf_music_bofangId = {R.drawable.selector_play, R.drawable.selector_play};
    int[] sf_music_zantingId = {R.drawable.selector_pause, R.drawable.selector_pause};


    @Override
    public void onClick(View v) {
        Log.i(TAG, "onClick v.getId() " + v.getId());
        int id = v.getId();
        Log.i(TAG, "onClick: huang currentMedia=>" + currentMedia);
        if (id == R.id.music_widget_next) {
            if (currentMedia == MUSIC) {
                mediaControl.sendStrToHost(AwellTool.MUSIC.NEXT);
            } else if (currentMedia == BT) {
                mediaControl.sendStrToHost(AwellTool.BT.NEXT);
            } else if (currentMedia == KUMUSIC) {
                mediaControl.sendNextToHost();
            } else if (currentMedia == CARPLAY) {
                mediaControl.sendNextToHost();
            } else if (currentMedia == OTHER_MUSIC) {
                mediaControl.sendNextToHost();
            }
        } else if (id == R.id.music_widget_pre) {
            if (currentMedia == MUSIC) {
                mediaControl.sendStrToHost(AwellTool.MUSIC.PREVIOUS);
            } else if (currentMedia == BT) {
                mediaControl.sendStrToHost(AwellTool.BT.PREVIOUS);
            } else if (currentMedia == KUMUSIC) {
                mediaControl.sendPreToHost();
            } else if (currentMedia == CARPLAY) {
                mediaControl.sendPreToHost();
            } else if (currentMedia == OTHER_MUSIC) {
                mediaControl.sendPreToHost();
            }
        } else if (id == R.id.music_widget_play) {
            if (currentMedia == MUSIC) {
                if (musicState) {
                    mediaControl.sendStrToHost(AwellTool.MUSIC.PAUSE);
                } else {
                    mediaControl.sendStrToHost(AwellTool.MUSIC.PLAY);
                }
            } else if (currentMedia == BT) {
                if (mediaControl.sendStrToHost(AwellTool.BT.GET_STATE).equals("true")) {
                    mediaControl.sendStrToHost(AwellTool.BT.PAUSE);
                } else {
                    mediaControl.sendStrToHost(AwellTool.BT.PLAY);
                }
            } else if (currentMedia == KUMUSIC) {
                mediaControl.sendTogglePlayPause();
            } else if (currentMedia == CARPLAY) {
                mediaControl.sendTogglePlayPause();
            } else if (currentMedia == OTHER_MUSIC) {
                mediaControl.sendTogglePlayPause();
            }
        } else if (id == R.id.layout_music_widget
                /*|| id == R.id.img_song_art
                || id == R.id.img_song_art_bg
                || id == R.id.music_widget_seekbars*/) {
            if (currentMedia == MUSIC) {
                startActivity("com.awell.localmusic", "com.awell.localmusic.MainActivity");
            } else if (currentMedia == BT) {
                try {
                    Intent btIntent = new Intent("com.awell.bluetooth");
                    btIntent.setClassName("com.awell.bluetooth", "com.awell.bluetooth.MainActivity");
                    btIntent.putExtra("bt_preference_key", 3);
                    mContext.startActivity(btIntent);
                } catch (ActivityNotFoundException e) {
                    Log.e("TAG", "Activity not found: " + e.getMessage());
                    // 可以提示用户安装目标应用
                }
            } else if (currentMedia == OTHER_MUSIC && !TextUtils.isEmpty(currentPlayingPackage)) {
                launchAppByPackageName(mContext, currentPlayingPackage);
            }
        }
    }

    public void stopLoadAnim() {
        Log.d(TAG, "stopLoadAnim--objectAnimator = " + mObjectAnimator);
        if (mObjectAnimator != null) {
            mObjectAnimator.cancel();
        }
    }

    public void pauseLoadAnim() {
        Log.d(TAG, "pauseLoadAnim--objectAnimator = " + mObjectAnimator);
        if (mObjectAnimator != null && mObjectAnimator.isRunning()) {
            mObjectAnimator.pause();
        }
    }

    public void resumeLoadAnim() {

        Log.d(TAG, "pauseLoadAnim--objectAnimator = " + mObjectAnimator);
        if (mObjectAnimator != null && mObjectAnimator.isPaused()) {
            mObjectAnimator.resume();
        }
    }

    public void startLoadAnim() {

        Log.d(TAG, "startLoadAnim--objectAnimator = " + mObjectAnimator);
        if (mObjectAnimator != null) {
            mObjectAnimator.setRepeatCount(ValueAnimator.INFINITE);
            mObjectAnimator.setRepeatMode(ObjectAnimator.RESTART);
            //objectAnimator.setStartDelay(500);
            mObjectAnimator.setDuration(40000);
            mObjectAnimator.start();
        }
    }

    private void startActivity(String pkg, String className) {
        Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(pkg);
        boolean isboot = true;
        if (intent != null) {
            for (int index = 0; index < IconCache.WorkSpacePackageName.length; index++) {
                Log.d(TAG, "packagename11 = " + pkg);
                if (!pkg.equals(IconCache.WorkSpacePackageName[index])) {
                    isboot = false;
                    break;
                }
            }
            if (pkg.contains("com.autonavi")) {
                if (isboot)
                    android.provider.Settings.System.putString(mContext.getContentResolver(), "boot_apk1", pkg);
            } else {
                if (isboot)
                    android.provider.Settings.System.putString(mContext.getContentResolver(), "boot_apk2", pkg);
            }
            mContext.startActivity(intent);
        }
    }

    public void setMusicNameTextView(String musicname, int flag) {
        Log.d(TAG, "setMusicNameTextView: flag= " + flag + " musicname=" + musicname + " currentMedia=" + currentMedia);
        if (mMusicNameTextView != null && currentMedia == flag) {
            if (!TextUtils.isEmpty(musicname)) {
                mMusicNameTextView.setText(musicname);
            } else {
                mMusicNameTextView.setText(getResources().getString(R.string.click_play_music));
            }
        }
    }

    public void setArtistNameTextView(String artistName, int flag) {
        Log.d(TAG, "setArtistNameTextView = " + flag + "--artistName=" + artistName + "--mArtistNameTextView=" + mArtistNameTextView);
        if (mArtistNameTextView != null && currentMedia == flag) {
            if (!TextUtils.isEmpty(artistName)) {
                mArtistNameTextView.setText(artistName);
            } else {
                mArtistNameTextView.setText(getResources().getString(R.string.music_artist));
            }
        }
    }

    public void loadAlbumArtByUri(Uri uri) {

        if (uri == null) {
            setDefaultImage();
            return;
        }

        Glide.with(this)
                .load(uri)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .addListener(listener)
                .circleCrop()
                .placeholder(R.drawable.ablum_default_bg)
                .error(R.drawable.ablum_default_bg)
                .into(ivLoadnim);

    }


    @SuppressLint("UseCompatLoadingForDrawables")
    private void setDefaultImage() {
        ivLoadnim.setImageDrawable(mContext.getDrawable(R.drawable.ablum_default_bg));
    }

    public void setMusicSeekBar(int curProgress, int totalProgress, int flag) {
        if (currentMedia == flag) {
            if (mBar != null) {
                mBar.setMax(totalProgress);
                mBar.setProgress(curProgress);
            }
            if (curProgress > 0 && curProgress < 1000) {
                stopLoadAnim();
                startLoadAnim();
            }
            //if (mTotalTimeTextView != null) {
            //    mTotalTimeTextView.setText(getCurOrTotalTime(totalProgress));
            //}

            //if (mCurTimeTextView != null) {
            //    mCurTimeTextView.setText(getCurOrTotalTime(curProgress));
            //}
        }
    }

    public void setCurMusicState(boolean musicState, int flag) {
        Log.e(TAG, "flag = " + flag + "--currentMedia=" + currentMedia);
        if (currentMedia == flag) {
            setCurMusicState(musicState);
        }
    }

    /**
     * 根据当前播放状态，切换UI
     */
    private void setCurMusicState(boolean musicState) {
        Log.i(TAG, "setCurMusicState = " + musicState);
        setTextEarquee(musicState);
        if (mPlayStateImageView == null) return;
        this.musicState = musicState;
        if (musicState) {
            if (currentMedia <= BT && mObjectAnimator != null) {
                Log.i(TAG, "setCurMusicState objectAnimator.isRunning()= " + mObjectAnimator.isRunning());
                if (!mObjectAnimator.isRunning()) {
                    startLoadAnim();
                } else {
                    resumeLoadAnim();
                }
            }
            mPlayStateImageView.setImageResource(sf_music_zantingId[dayNight]);
        } else {
            pauseLoadAnim();
            mPlayStateImageView.setImageResource(sf_music_bofangId[dayNight]);
        }
    }

    private void setImageIcon(int index) {

        ll_time_layout_music.setVisibility(INVISIBLE);

        ll_control_layout_music.setVisibility(VISIBLE);
        ll_name_layout_music.setVisibility(VISIBLE);

    }

    /**
     * 切换当前音频播放器的播放控制器
     */
    public void switchMediaController(String packName, String status, int mediaType, int curMedia) {
        Log.i(TAG, "switchMediaController: packName = " + packName + ", status= " + status);
        Log.i(TAG, "switchMediaController: mediaType = " + mediaType + ", curMedia= " + curMedia);

        if ("start".equals(status)) {
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
            setImageIcon(currentMedia);
        }

    }


    public static boolean launchAppByPackageName(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        // 检查应用是否安装
        try {
            pm.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(context, "应用未安装", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 获取主 Activity Intent
        Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
            return true;
        } else {
            Toast.makeText(context, "无法启动应用", Toast.LENGTH_SHORT).show();
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


