package com.awell.ctrlview;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.awell.launcher.R;
import com.awell.launcher2.IconCache;
import com.awell.launcher2.Launcher;
import com.awell.library.AwellLibrary;
import com.awell.library.AwellTool;
import com.awell.utils.SocketThread;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/*import cn.kuwo.autosdk.api.KWAPI;
import cn.kuwo.autosdk.api.OnGetSongImgUrlListener;
import cn.kuwo.autosdk.api.OnPlayPaySongListener;
import cn.kuwo.autosdk.api.PlayState;
import cn.kuwo.autosdk.api.PlayerStatus;
import cn.kuwo.base.bean.Music;*/

public class MusicWidget extends RelativeLayout implements OnClickListener {
    private static final String TAG = "MusicWidgetLog";
    private Context mContext;
    private TextView mMusicNameTextView, mArtistNameTextView;
    private TextView mCurTimeTextView, mTotalTimeTextView;
    private ImageView ivLoadnim, ivLoadnim_bar, icon_music_img;
    private ImageView mPlayStateImageView, musicPreIv, musicNextIv;
    private SeekBar bar = null;
    private ObjectAnimator objectAnimator;
    //private KWAPI kwapi;
    private int dayNight = 0;
    private boolean musicState = false;
    private int[] musicId = {R.drawable.icon_music_img, R.drawable.icon_bt_img,
            R.drawable.icon_kwplay_img, R.drawable.icon_carplay_img,};

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

    private AwellLibrary mediaLibrary;

    public MusicWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }

    /**
     * @param context
     */
    public void setActivity(Context context, View view) {
        findViews(context, view);
    }

    public void setMediaLibrary(AwellLibrary mediaLibrary) {
        this.mediaLibrary = mediaLibrary;
        Log.i(TAG, "setMediaLibrary: huang media library=>" + mediaLibrary);
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

        /*
        icon_music_img = view.findViewById(R.id.icon_music_img);
        ivLoadnim_bar = view.findViewById(R.id.music_artists_image_view_bar);
        ivLoadnim = view.findViewById(R.id.music_artists_image_view);
        objectAnimator = ObjectAnimator.ofFloat(ivLoadnim, "rotation", 0f, 360f);
        objectAnimator.setInterpolator(new LinearInterpolator());
        stopLoadAnim();*/

        mPlayStateImageView = (ImageView) view.findViewById(R.id.music_widget_play);
        mPlayStateImageView.setOnClickListener(this);
        musicPreIv = (ImageView) view.findViewById(R.id.music_widget_pre);
        musicPreIv.setOnClickListener(this);
        musicNextIv = (ImageView) view.findViewById(R.id.music_widget_next);
        musicNextIv.setOnClickListener(this);

        view.findViewById(R.id.ll_name_layout_music).setOnClickListener(this);
        //getKwMusicApi();

        setImageIcon(currentMedia);
        setCurMusicState(mediaLibrary.setDataEvent(AwellTool.MUSIC.GET_STATE).equals("true"), MUSIC);

    }

    //private SocketThread zlinkCarPlaySocketThread;
    private int mCarPlayTotalTime;

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

    private Handler carPlayhandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.e(TAG, "handleMessage arg1:" + msg.arg1 + " obj:" + msg.obj);
            switch (msg.arg1) {
                case SocketThread.MESSAGE_ERROR:
                    setCurMusicState(false, CARPLAY);
                    break;
                case SocketThread.MESSAGE_SUCCEED:
                    break;
                case SocketThread.MESSAGE_RECEIVE_SONGER:
                    setArtistNameTextView((String) msg.obj, CARPLAY);
                    break;
                case SocketThread.MESSAGE_RECEIVE_TIME_STATUS:
                    int status = (int) msg.obj;
                    if (status == 0) {
                        setCurMusicState(false, CARPLAY);
                    } else {
                        setCurMusicState(true, CARPLAY);
                    }
                    break;
                case SocketThread.MESSAGE_RECEIVE_TIME_TOTAL:
                    mCarPlayTotalTime = (int) msg.obj;
                    break;
                case SocketThread.MESSAGE_RECEIVE_WORE:
                    setMusicNameTextView((String) msg.obj, CARPLAY);
                    break;
                case SocketThread.MESSAGE_RECEIVE_TIME:
                    setMusicSeekBar((int) msg.obj, mCarPlayTotalTime, OTHER_MUSIC);
                    break;
                default:
                    break;
            }
        }
    };

    /*
        private void getKwMusicApi() {
            kwapi = KWAPI.getKWAPI();
            boolean kwFlag = kwapi.bindAutoSdkService(mContext);
            Log.e(TAG, "kwFlag = " + kwFlag);
            kwapi.registerPlayerStatusListener(mContext, (playerStatus, music) -> {
                if (music == null) return;
                Log.e(TAG, "image URL = " + music.imageURL);

                Log.e(TAG, "playerStatus = " + playerStatus.name());
                if (playerStatus.equals(PlayerStatus.PLAYING)) {
                    setCurMusicState(true, KUMUSIC);
                    if (mMusicNameTextView != null && music.name != null)
                        setMusicNameTextView(music.name, KUMUSIC);

                    if (mArtistNameTextView != null && music.artist != null)
                        setArtistNameTextView(music.artist, KUMUSIC);
                } else if (playerStatus.equals(PlayerStatus.PAUSE)) {
                    setCurMusicState(false, KUMUSIC);
                }

                kwapi.getSongPicUrl(music, new OnGetSongImgUrlListener() {
                    @Override
                    public void onGetSongImgUrlSucessed(Music music, String s) {
                        Log.i(TAG, "music = " + music.toString());
                        Log.i(TAG, "music = " + s);
    //                    setPlayImage(s);
                    }

                    @Override
                    public void onGetSongImgUrlFailed(Music music, int i) {
                        Log.i(TAG, "music = " + music.toString());
                        Log.i(TAG, "music = " + i);
                    }
                });
            });
            //监听酷我退出
            kwapi.registerExitListener(() -> {

            });
        }
    */
    public Bitmap drawCircleView(Bitmap bitmap) {
        bitmap = Bitmap.createScaledBitmap(bitmap, 118, 118, true);
        Bitmap bm = Bitmap.createBitmap(180, 186, Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawCircle(59, 59, 59, paint);
        paint.reset();
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return bm;
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
        switch (v.getId()) {
            case R.id.music_widget_next:
                if (currentMedia == MUSIC) {
                    mediaLibrary.setDataEvent(AwellTool.MUSIC.NEXT);
                } else if (currentMedia == BT) {
                    mediaLibrary.setDataEvent(AwellTool.BT.NEXT);
                } else if (currentMedia == KUMUSIC) {
                    //kwapi.setPlayState(PlayState.STATE_NEXT);
                    Launcher.mMediaListener.skipToNext();
                } else if (currentMedia == CARPLAY) {
                    //keyDealToZlink(KeyEvent.KEYCODE_MEDIA_NEXT);
                    Launcher.mMediaListener.skipToNext();
                } else if (currentMedia == OTHER_MUSIC) {
                    Launcher.mMediaListener.skipToNext();
                }
                break;
            case R.id.music_widget_pre:
                if (currentMedia == MUSIC) {
                    mediaLibrary.setDataEvent(AwellTool.MUSIC.PREVIOUS);
                } else if (currentMedia == BT) {
                    mediaLibrary.setDataEvent(AwellTool.BT.PREVIOUS);
                } else if (currentMedia == KUMUSIC) {
                    //kwapi.setPlayState(PlayState.STATE_PRE);
                    Launcher.mMediaListener.skipToPrevious();
                } else if (currentMedia == CARPLAY) {
                    //keyDealToZlink(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                    Launcher.mMediaListener.skipToPrevious();
                } else if (currentMedia == OTHER_MUSIC) {
                    Launcher.mMediaListener.skipToPrevious();
                }
                break;
            case R.id.music_widget_play:
                if (currentMedia == MUSIC) {
                    //if (Launcher.mediaLibrary.setDataEvent(AwellTool.MUSIC.GET_STATE).equals("true"))
                    if (musicState) {
                        mediaLibrary.setDataEvent(AwellTool.MUSIC.PAUSE);
                    } else {
                        mediaLibrary.setDataEvent(AwellTool.MUSIC.PLAY);
                    }
                } else if (currentMedia == BT) {
                    if (mediaLibrary.setDataEvent(AwellTool.BT.GET_STATE).equals("true"))
                        mediaLibrary.setDataEvent(AwellTool.BT.PAUSE);
                    else
                        mediaLibrary.setDataEvent(AwellTool.BT.PLAY);
                } else if (currentMedia == KUMUSIC) {
                    /*if (kwapi.getPlayerStatus().equals(PlayerStatus.PAUSE))
                        kwapi.setPlayState(PlayState.STATE_PLAY);
                    else if (kwapi.getPlayerStatus().equals(PlayerStatus.PLAYING))
                        kwapi.setPlayState(PlayState.STATE_PAUSE);*/
                    Launcher.mMediaListener.togglePlayPause();
                } else if (currentMedia == CARPLAY) {
                    //keyDealToZlink(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                    Launcher.mMediaListener.togglePlayPause();
                } else if (currentMedia == OTHER_MUSIC) {
                    Launcher.mMediaListener.togglePlayPause();
                }
                break;
            case R.id.ll_name_layout_music:
            case R.id.music_widget_rl:
                Log.i(TAG, "onClick MUSIC_MEDIA_PLAY -currentMedia=" + currentMedia);
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
                /*} else if (currentMedia == KUMUSIC) {
                    startActivity("cn.kuwo.kwmusiccar", "cn.kuwo.kwmusiccar.MainActivity");
                } else if (currentMedia == CARPLAY) {
                    startActivity("com.zjinnova.zlink", "com.zjinnova.android.zlink.features.main.MainActivity");*/
                } else if (currentMedia == OTHER_MUSIC && !TextUtils.isEmpty(currentPlayingPackage)) {
                    launchAppByPackageName(mContext, currentPlayingPackage);
                }
                break;
            default:
                break;
        }
    }

    public void stopLoadAnim() {
        if (ivLoadnim_bar != null) {
            ivLoadnim_bar.setPivotX(0);
            ivLoadnim_bar.setPivotY(0);
            ivLoadnim_bar.setRotation(-10);
        }
        if (objectAnimator != null) {
            objectAnimator.cancel();
        }
    }

    public void startLoadAnim() {
        if (ivLoadnim_bar != null) {
            ivLoadnim_bar.setPivotX(0);
            ivLoadnim_bar.setPivotY(0);
            ivLoadnim_bar.setRotation(0);
        }
        if (objectAnimator != null) {
            objectAnimator.setRepeatCount(ValueAnimator.INFINITE);
            objectAnimator.setRepeatMode(ObjectAnimator.RESTART);
            objectAnimator.setStartDelay(1000);
            objectAnimator.setDuration(4500);
            objectAnimator.start();
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

    public void setPlayImage(long long1, long long2) {
        Bitmap bitmap = getArtwork(mContext, long1, long2, true);
        setPlayImage(bitmap);
    }

    public void setPlayImage(String imageUrl) {
        Bitmap bitmap = BitmapFactory.decodeFile(imageUrl);
        setPlayImage(bitmap);
    }

    public void setPlayImage(Bitmap bitmap) {
        if (ivLoadnim == null) return;
        if (bitmap != null) {
            ivLoadnim.setImageBitmap(bitmap);
        } else {
            ivLoadnim.setImageDrawable(mContext.getDrawable(R.drawable.ablum_default_bg));
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
        //if (icon_music_img != null)
        //    icon_music_img.setImageResource(musicId[index]);

        //if (/*index == BT || */index == KUMUSIC) {
        //    ll_time_layout_music.setVisibility(INVISIBLE);
        //} else {
        ll_time_layout_music.setVisibility(VISIBLE);
        //}

        //if (index == CARPLAY) {
        //    ll_control_layout_music.setVisibility(INVISIBLE);
        //    ll_name_layout_music.setVisibility(INVISIBLE);
        //} else {
        ll_control_layout_music.setVisibility(VISIBLE);
        ll_name_layout_music.setVisibility(VISIBLE);
        //}


    }

    /**
     * 切换当前音频播放器的播放控制器
     *
     * @param packName
     * @param status
     */
    public void switchMediaController(String packName, String status, int mediaType, int curMedia) {
        Log.e(TAG, "current mediaplay packName = " + packName + ",status = " + status);
        if (packName.contains("localmusic")) {//本地音乐
            if ("start".equals(status)) {
                currentMedia = MUSIC;
                currentPlayingPackage = packName;
                setImageIcon(currentMedia);
                setCurMusicState(true);
            } else if ("stop".equals(status)) {

            }
        } else if (packName.contains("com.awell.bluetooth") || packName.contains("/system/bin/gocsdk")) {//蓝牙音乐
            if ("start".equals(status) && mediaType == AudioManager.STREAM_MUSIC) {
                currentMedia = BT;
                currentPlayingPackage = packName;
                setImageIcon(currentMedia);
                setCurMusicState(true);
            } else if ("stop".equals(status)) {

            }
        /*} else if (packName.contains("kwmusiccar")) {//酷我音乐
            if ("start".equals(status)) {
                currentMedia = KUMUSIC;
                currentPlayingPackage=packName;
                setImageIcon(currentMedia);
                setCurMusicState(true);
            } else if ("stop".equals(status)) {

            }
        } else if (packName.contains("com.zjinnova.zlink")) {//蓝牙音乐
            if ("start".equals(status) && mediaType == AudioManager.STREAM_MUSIC) {
                currentMedia = CARPLAY;
                currentPlayingPackage=packName;
                setImageIcon(currentMedia);
                setCurMusicState(true);
            } else if ("stop".equals(status)) {

            }*/
        } else if (!TextUtils.isEmpty(packName) && curMedia > BT) {
            if ("start".equals(status) && mediaType == AudioManager.STREAM_MUSIC) {
                currentMedia = OTHER_MUSIC;
                currentPlayingPackage = packName;
                setImageIcon(currentMedia);
                setCurMusicState(true);
            } else if ("stop".equals(status)) {

            }
        }
    }

    private final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart/");
    private final BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();

    public Bitmap getArtwork(Context context, long song_id, long album_id, boolean allowdefault) {
        if (album_id < 0) {
            // This is something that is not in the database, so get the album art directly
            // from the file.
            if (song_id >= 0) {
                Bitmap bm = getArtworkFromFile(context, song_id, -1);
                if (bm != null) {
                    return bm;
                }
            }
            if (allowdefault) {
                return getDefaultArtwork(context);
            }
            return null;
        }
        ContentResolver res = context.getContentResolver();
        Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);
        if (uri != null) {
            InputStream in = null;
            try {
                in = res.openInputStream(uri);
                return BitmapFactory.decodeStream(in, null, sBitmapOptions);
            } catch (FileNotFoundException ex) {
                // The album art thumbnail does not actually exist. Maybe the user deleted it, or
                // maybe it never existed to begin with.
                Bitmap bm = getArtworkFromFile(context, song_id, album_id);

                if (bm != null) {
                    if (bm.getConfig() == null) {
                        bm = bm.copy(Bitmap.Config.RGB_565, false);
                        if (bm == null && allowdefault) {
                            return getDefaultArtwork(context);
                        }
                    }
                } else if (allowdefault) {
                    bm = getDefaultArtwork(context);
                }
                return bm;
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ex) {
                }
            }
        }
        return null;
    }

    private Bitmap getArtworkFromFile(Context context, long songid, long albumid) {
        Bitmap bm = null;
        byte[] art = null;
        String path = null;
        if (albumid < 0 && songid < 0) {
            throw new IllegalArgumentException("Must specify an album or a song id");
        }
        try {
            Uri uri;
            if (albumid < 0) {
                uri = Uri.parse("content://media/external/audio/media/" + songid + "/albumart");
            } else {
                uri = ContentUris.withAppendedId(sArtworkUri, albumid);
            }
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
            if (pfd != null) {
                FileDescriptor fd = pfd.getFileDescriptor();
                bm = BitmapFactory.decodeFileDescriptor(fd);
            }
        } catch (FileNotFoundException ex) {

        }
        return bm;
    }

    private static Bitmap getDefaultArtwork(Context context) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        /*return BitmapFactory.decodeStream(
                context.getResources().openRawResource(R.mipmap.ic_launcher), null, opts);*/
//        return drawableToBitmap(context.getResources().getDrawable(R.mipmap.ablum_default_bg));
        return null;
    }

    private void keyDealToZlink(int keycode) {
        Intent intent = new Intent();
        intent.setAction("com.zjinnova.zlink");
        intent.setPackage("com.zjinnova.zlink");

        intent.putExtra("command", "REQ_SPEC_FUNC_CMD");
        intent.putExtra("specFuncCode", keycode);
        mContext.sendBroadcast(intent);
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

    public static boolean isMusicApp(Context context, String packageName) {
        // 检查是否响应音频文件播放
        PackageManager pm = context.getPackageManager();
        Intent audioIntent = new Intent(Intent.ACTION_VIEW);
        audioIntent.setDataAndType(Uri.parse("file:///test.mp3"), "audio/*");
        List<ResolveInfo> handlers = pm.queryIntentActivities(audioIntent, 0);
        for (ResolveInfo info : handlers) {
            if (info.activityInfo.packageName.equals(packageName)) {
                return true;
            }
        }

        // 检查是否声明音乐类别
        Intent mainIntent = new Intent(Intent.ACTION_MAIN);
        mainIntent.addCategory(Intent.CATEGORY_APP_MUSIC);
        List<ResolveInfo> musicApps = pm.queryIntentActivities(mainIntent, 0);
        for (ResolveInfo info : musicApps) {
            if (info.activityInfo.packageName.equals(packageName)) {
                return true;
            }
        }

        return false;
    }
}


