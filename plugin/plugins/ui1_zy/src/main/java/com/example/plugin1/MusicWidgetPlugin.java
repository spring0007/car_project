package com.example.plugin1;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.awell.control.AwellMediaControl;
import com.awell.launcher2.IconCache;
import com.awell.library.AwellTool;

/*import cn.kuwo.autosdk.api.KWAPI;
import cn.kuwo.autosdk.api.OnGetSongImgUrlListener;
import cn.kuwo.autosdk.api.OnPlayPaySongListener;
import cn.kuwo.autosdk.api.PlayState;
import cn.kuwo.autosdk.api.PlayerStatus;
import cn.kuwo.base.bean.Music;*/

public class MusicWidgetPlugin extends RelativeLayout implements OnClickListener {
    private static final String TAG = "MusicWidgetLog";

    private Context mContext;
    private TextView mMusicNameTextView, mArtistNameTextView;
    private TextView mCurTimeTextView, mTotalTimeTextView;
    private ImageView icon_music_img;
    private ImageView mPlayStateImageView, musicPreIv, musicNextIv;
    private SeekBar bar = null;
    private int dayNight = 0;
    private boolean musicState = false;

    public final static int MUSIC = 0;
    public final static int BT = 1;
    public final static int KUMUSIC = 2;
    public final static int CARPLAY = 3;
    public final static int OTHER_MUSIC = 4;

    public final static String OTHER_MUSIC_PLAYSTATUS = "other_music_playStatus";
    public final static String OTHER_MUSIC_PLAYNAME = "other_music_playName";
    public final static String OTHER_MUSIC_TIME = "other_music_playTime";
    private int currentMedia = MUSIC;//0 music   1 bt   2 kw music  3 carplay
    private String currentPlayingPackage = null;

    private LinearLayout ll_control_layout_music, ll_name_layout_music;
    private RelativeLayout ll_time_layout_music;

    private AwellMediaControl mediaControl;

    public MusicWidgetPlugin(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }

    /**
     * @param context
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
        view.findViewById(R.id.music_widget_rl).setOnClickListener(this);

        mMusicNameTextView = (TextView) view.findViewById(R.id.music_widget_music_name);

        mArtistNameTextView = (TextView) view.findViewById(R.id.music_artist);

        bar = view.findViewById(R.id.music_widget_seekbars);
        bar.setOnTouchListener((v, event) -> true);//禁止进度条拖动
        mCurTimeTextView = (TextView) view.findViewById(R.id.music_widget_cur_time_textview);
        mTotalTimeTextView = (TextView) view.findViewById(R.id.music_widget_total_time_textview);

        mPlayStateImageView = (ImageView) view.findViewById(R.id.music_widget_play);
        mPlayStateImageView.setOnClickListener(this);
        musicPreIv = (ImageView) view.findViewById(R.id.music_widget_pre);
        musicPreIv.setOnClickListener(this);
        musicNextIv = (ImageView) view.findViewById(R.id.music_widget_next);
        musicNextIv.setOnClickListener(this);

        view.findViewById(R.id.ll_name_layout_music).setOnClickListener(this);
        //getKwMusicApi();

        setImageIcon(currentMedia);
        setCurMusicState(mediaControl.sendStrToHost(AwellTool.MUSIC.GET_STATE).equals("true"), MUSIC);

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
                if (icon_music_img != null) {
                    icon_music_img.setImageResource(R.drawable.icon_autoplay_img);
                }
            }
        } else if (zlinStatus.equals("MAIN_AUDIO_STOP")) {
            setCurMusicState(false, OTHER_MUSIC);
        } else if (zlinStatus.equals("MAIN_AUDIO_START")) {
            //currentMedia = CARPLAY;
            Log.d(TAG, "getCarPlayData-currentMedia: " + currentMedia);
            setCurMusicState(true, OTHER_MUSIC);
        }
    }

    int[] textColorId = {Color.BLACK, Color.WHITE};
    int[] sf_music_nextId = {R.drawable.sf_music_next, R.drawable.sf_music_next_n};
    int[] sf_music_preId = {R.drawable.sf_music_pre, R.drawable.sf_music_pre_n};
    int[] sf_music_bofangId = {R.drawable.sf_music_bofang, R.drawable.sf_music_bofang_n};
    int[] sf_music_zantingId = {R.drawable.sf_music_zanting, R.drawable.sf_music_zanting_n};
    int[] sf_music_seekbarId = {R.drawable.sf_music_seekbar, R.drawable.sf_music_seekbar_n};

    public void setDayNight(int dayNight) {
        this.dayNight = dayNight;
        mMusicNameTextView.setTextColor(textColorId[dayNight]);
        mArtistNameTextView.setTextColor(textColorId[dayNight]);
        mCurTimeTextView.setTextColor(textColorId[dayNight]);
        mTotalTimeTextView.setTextColor(textColorId[dayNight]);
        mPlayStateImageView.setImageResource(musicState ? sf_music_zantingId[dayNight] : sf_music_bofangId[dayNight]);
        musicPreIv.setImageResource(sf_music_preId[dayNight]);
        musicNextIv.setImageResource(sf_music_nextId[dayNight]);
        bar.setProgressDrawable(mContext.getDrawable(sf_music_seekbarId[dayNight]));
    }

    @Override
    public void onClick(View v) {
        Log.i(TAG, "onClick v.getId() " + v.getId());
        int id = v.getId();
        if (id == R.id.music_widget_next) {
           // if (currentMedia == MUSIC) {
                mediaControl.sendStrToHost(AwellTool.MUSIC.NEXT);
            /*} else if (currentMedia == BT) {
                mediaControl.sendStrToHost(AwellTool.BT.NEXT);
            } else if (currentMedia == KUMUSIC) {
                mediaControl.sendNextToHost();
            } else if (currentMedia == CARPLAY) {
                mediaControl.sendNextToHost();
            } else if (currentMedia == OTHER_MUSIC) {
                mediaControl.sendNextToHost();
            }*/
        } else if (id == R.id.music_widget_pre) {
            //if (currentMedia == MUSIC) {
                mediaControl.sendStrToHost(AwellTool.MUSIC.PREVIOUS);
           /* } else if (currentMedia == BT) {
                mediaControl.sendStrToHost(AwellTool.BT.PREVIOUS);
            } else if (currentMedia == KUMUSIC) {
                mediaControl.sendPreToHost();
            } else if (currentMedia == CARPLAY) {
                mediaControl.sendPreToHost();
            } else if (currentMedia == OTHER_MUSIC) {
                mediaControl.sendPreToHost();
            }*/
        } else if (id == R.id.music_widget_play) {
            //if (currentMedia == MUSIC) {
                if (musicState) {
                    mediaControl.sendStrToHost(AwellTool.MUSIC.PAUSE);
                } else {
                    mediaControl.sendStrToHost(AwellTool.MUSIC.PLAY);
                }
            /*} else if (currentMedia == BT) {
                if (mediaControl.sendStrToHost(AwellTool.BT.GET_STATE).equals("true")) {
                    mediaControl.sendStrToHost(AwellTool.BT.PAUSE);
                } else {
                    mediaControl.sendStrToHost(AwellTool.BT.PLAY);
                }
            } else if (currentMedia == KUMUSIC) {
        
                mediaControl.sendTogglePlayPause();
            } else if (currentMedia == CARPLAY) {
                //keyDealToZlink(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                //Launcher.mMediaListener.togglePlayPause();
                mediaControl.sendTogglePlayPause();
            } else if (currentMedia == OTHER_MUSIC) {
                //Launcher.mMediaListener.togglePlayPause();
                mediaControl.sendTogglePlayPause();
            }*/
        } else if (id == R.id.ll_name_layout_music || id == R.id.music_widget_rl) {
            Log.i(TAG, "onClick MUSIC_MEDIA_PLAY -currentMedia=" + currentMedia);
            //if (currentMedia == MUSIC) {
                startActivity("com.awell.localmusic", "com.awell.localmusic.MainActivity");
            /*} else if (currentMedia == BT) {
                try {
                    Intent btIntent = new Intent("com.awell.bluetooth");
                    btIntent.setClassName("com.awell.bluetooth", "com.awell.bluetooth.MainActivity");
                    btIntent.putExtra("bt_preference_key", 3);
                    mContext.startActivity(btIntent);
                } catch (ActivityNotFoundException e) {
                    Log.e("TAG", "Activity not found: " + e.getMessage());
                    // 可以提示用户安装目标应用
                }
                //} else if (currentMedia == KUMUSIC) {
                //    startActivity("cn.kuwo.kwmusiccar", "cn.kuwo.kwmusiccar.MainActivity");
                //} else if (currentMedia == CARPLAY) {
                   // startActivity("com.zjinnova.zlink", "com.zjinnova.android.zlink.features.main.MainActivity");
            } else if (currentMedia == OTHER_MUSIC && !TextUtils.isEmpty(currentPlayingPackage)) {
                launchAppByPackageName(mContext, currentPlayingPackage);
            }*/
        }
    }

    private void startActivity(String packName, String className) {
        Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(packName);
        boolean isboot = true;
        if (intent != null) {
            for (int index = 0; index < IconCache.WorkSpacePackageName.length; index++) {
                Log.d(TAG, "packagename11 = " + packName);
                if (!packName.equals(IconCache.WorkSpacePackageName[index])) {
                    isboot = false;
                    break;
                }
            }
            if (packName.contains("com.autonavi")) {
                if (isboot)
                    android.provider.Settings.System.putString(mContext.getContentResolver(), "boot_apk1", packName);
            } else {
                if (isboot)
                    android.provider.Settings.System.putString(mContext.getContentResolver(), "boot_apk2", packName);
            }
            mContext.startActivity(intent);
        }
    }

    public void setMusicNameTextView(String musicname, int flag) {
        Log.d(TAG, "setMusicNameTextView = " + flag + "--musicname=" + musicname + "--mMusicNameTextView=" + mMusicNameTextView + "--currentMedia=" + currentMedia);
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

    public void setMusicSeekBar(int curProgress, int totalProgress, int flag) {
        if (currentMedia == flag) {
            if (bar != null) {
                bar.setMax(totalProgress);
                bar.setProgress(curProgress);
            }
            if (mTotalTimeTextView != null) {
                mTotalTimeTextView.setText(getCurOrTotalTime(totalProgress));
            }

            if (mCurTimeTextView != null) {
                mCurTimeTextView.setText(getCurOrTotalTime(curProgress));
            }
        }
    }

    /**
     * 根据传入值返回String类型播放时长或总时长
     *
     * @param duration
     */
    public static String getCurOrTotalTime(long duration) {
        long currectTime = duration / 1000;
        String currectMinute = currectTime / 60 + "";
        String currectSecond = (currectTime - Integer.parseInt(currectMinute) * 60) + "";
        if (currectMinute.length() == 1) {
            currectMinute = "0" + currectMinute;
        }
        if (currectSecond.length() == 1) {
            currectSecond = "0" + currectSecond;
        }
        return currectMinute + ":" + currectSecond;
    }

    public void setCurMusicState(boolean musicState, int flag) {
        Log.e(TAG, "flag = " + flag + "--currentMedia=" + currentMedia);
        if (currentMedia == flag) {
            setCurMusicState(musicState);
        }
    }

    /**
     * 根据当前播放状态，切换UI
     *
     * @param musicState
     */
    private void setCurMusicState(boolean musicState) {
        Log.e(TAG, "setCurMusicState = " + musicState);
        if (mPlayStateImageView == null) return;
        this.musicState = musicState;
        if (musicState) {
            //startLoadAnim();
            mPlayStateImageView.setImageResource(sf_music_zantingId[dayNight]);
        } else {
            //stopLoadAnim();
            mPlayStateImageView.setImageResource(sf_music_bofangId[dayNight]);
        }
    }

    private void setImageIcon(int index) {

        if (ll_time_layout_music != null)
            ll_time_layout_music.setVisibility(VISIBLE);

        ll_control_layout_music.setVisibility(VISIBLE);
        ll_name_layout_music.setVisibility(VISIBLE);

    }

    /**
     * 切换当前音频播放器的播放控制器
     *
     * @param packName
     * @param status
     */
    public void switchMediaController(String packName, String status, int mediaType, int curMedia) {
        Log.i(TAG, "switchMediaController : packName=>" + packName + ", status = " + status);
        Log.i(TAG, "switchMediaController : mediaType=>" + mediaType + ", curMedia = " + curMedia);

        if ("start".equals(status)) {
            //currentPlayingPackage = packName;
            //if (packName.contains("localmusic")) {
                currentMedia = MUSIC;
            /*} else if ((packName.contains("com.awell.bluetooth") || packName.contains("/system/bin/gocsdk"))
                    && mediaType == AudioManager.STREAM_MUSIC) {
                currentMedia = BT;
            } else if (mediaType == AudioManager.STREAM_MUSIC) {
                currentMedia = OTHER_MUSIC;
            }*/
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

}


