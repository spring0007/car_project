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

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.SearchManager;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaMetadata;
import android.media.MediaRecorder;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.StrictMode;
import android.os.SystemClock;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.speech.RecognizerIntent;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Advanceable;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.awell.addapp.AddSelectAppCallback;
import com.awell.addapp.AppInfo;
import com.awell.addapp.AppInofAdapter;
import com.awell.addapp.AppPopAdapter;
import com.awell.addapp.MyDbHelper;
import com.awell.addapp.ShowPopupI;
import com.awell.ctrlview.FrequencyTextView;
import com.awell.ctrlview.MusicWidget;
import com.awell.ctrlview.MyPageTransformer2;
import com.awell.ctrlview.MyQAnalogClock;
import com.awell.ctrlview.NewCalendar;
import com.awell.launcher.R;
import com.awell.launcher2.DropTarget.DragObject;
import com.awell.library.AwellLibrary;
import com.awell.library.AwellTool;
import com.awell.ui.UIActivity;
import com.awell.utils.CommonData;
import com.awell.utils.Utils;
import com.awell.ctrlview.WaveformView;
import com.awell.ctrlview.VisualizerView;
import com.awell.ctrlview.SpetrumView;
import com.awell.ctrlview.AudioRecorder;
import com.awell.ctrlview.NewCalendar;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Timer;

/**
 * Default launcher application.
 */
public final class Launcher extends Activity implements View.OnClickListener, OnLongClickListener, LauncherModel.Callbacks, View.OnTouchListener {
    public static final String TAG = "LauncherMainLog";

    private final boolean PROFILE_STARTUP = false;
    private final boolean DEBUG_WIDGETS = false;
    private final boolean DEBUG_STRICT_MODE = false;

    private final int MENU_GROUP_WALLPAPER = 1;
    private final int MENU_WALLPAPER_SETTINGS = Menu.FIRST + 1;
    private final int MENU_MANAGE_APPS = MENU_WALLPAPER_SETTINGS + 1;
    private final int MENU_SYSTEM_SETTINGS = MENU_MANAGE_APPS + 1;
    private final int MENU_HELP = MENU_SYSTEM_SETTINGS + 1;

    private final int REQUEST_CREATE_SHORTCUT = 1;
    private final int REQUEST_CREATE_APPWIDGET = 5;
    private final int REQUEST_PICK_APPLICATION = 6;
    private final int REQUEST_PICK_SHORTCUT = 7;
    private final int REQUEST_PICK_APPWIDGET = 9;
    private final int REQUEST_PICK_WALLPAPER = 10;
    private final int MSG_UPDATE_SPEED = 1;
    private final int MSG_CLEAR_SPEED = 2;
    private final int REQUEST_BIND_APPWIDGET = 11;

    static final String EXTRA_SHORTCUT_DUPLICATE = "duplicate";
    /**
     * hxw change 5 to 0
     */
    static final int SCREEN_COUNT = 0;
    static final int DEFAULT_SCREEN = 2;

    private final String PREFERENCES = "launcher.preferences";
    final String FORCE_ENABLE_ROTATION_PROPERTY = "debug.force_enable_rotation";
    final String DUMP_STATE_PROPERTY = "debug.dumpstate";

    // The Intent extra that defines whether to ignore the launch animation
    static final String INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION = "com.awell.launcher.intent.extra.shortcut.INGORE_LAUNCH_ANIMATION";

    // Type: int
    private final String RUNTIME_STATE_CURRENT_SCREEN = "launcher.current_screen";
    // Type: int
    private final String RUNTIME_STATE = "launcher.state";
    // Type: int
    private final String RUNTIME_STATE_PENDING_ADD_CONTAINER = "launcher.add_container";
    // Type: int
    private final String RUNTIME_STATE_PENDING_ADD_SCREEN = "launcher.add_screen";
    // Type: int
    private final String RUNTIME_STATE_PENDING_ADD_CELL_X = "launcher.add_cell_x";
    // Type: int
    private final String RUNTIME_STATE_PENDING_ADD_CELL_Y = "launcher.add_cell_y";
    // Type: boolean
    private final String RUNTIME_STATE_PENDING_FOLDER_RENAME = "launcher.rename_folder";
    // Type: long
    private final String RUNTIME_STATE_PENDING_FOLDER_RENAME_ID = "launcher.rename_folder_id";
    // Type: int
    private final String RUNTIME_STATE_PENDING_ADD_SPAN_X = "launcher.add_span_x";
    // Type: int
    private final String RUNTIME_STATE_PENDING_ADD_SPAN_Y = "launcher.add_span_y";
    // Type: parcelable
    private final String RUNTIME_STATE_PENDING_ADD_WIDGET_INFO = "launcher.add_widget_info";

    private final String TOOLBAR_ICON_METADATA_NAME = "com.awell.launcher.toolbar_icon";
    private static final String TOOLBAR_SEARCH_ICON_METADATA_NAME = "com.awell.launcher.toolbar_search_icon";
    private static final String TOOLBAR_VOICE_SEARCH_ICON_METADATA_NAME = "com.awell.launcher.toolbar_voice_search_icon";
    private boolean isSpeehONorOFFflag;

    public final String ACTION_SEND_TO_CANBUS = "com.acloud.intent.android.XY.ANDROID.CANBUSDISPINFO1";
    public final String KEY_EXTRA_MAINUI = "extras";// 鏀堕煶鏈哄箍鎾殑key鍊�

    /**
     * The different states that Launcher can be in.
     */
    private enum State {
        NONE, WORKSPACE, APPS_CUSTOMIZE, APPS_CUSTOMIZE_SPRING_LOADED
    }

    private State mState = State.WORKSPACE;
    private AnimatorSet mStateAnimation;
    private AnimatorSet mDividerAnimator;

    static final int APPWIDGET_HOST_ID = 1024;
    private static final int EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT = 300;
    private static final int EXIT_SPRINGLOADED_MODE_LONG_TIMEOUT = 600;
    private static final int SHOW_CLING_DURATION = 550;
    private final int DISMISS_CLING_DURATION = 250;

    public static final Object sLock = new Object();
    private static int sScreen = DEFAULT_SCREEN;

    // How long to wait before the new-shortcut animation automatically pans the
    // workspace
    private static final int NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS = 10;

    private final BroadcastReceiver mCloseSystemDialogsReceiver = new CloseSystemDialogsIntentReceiver();
    private final ContentObserver mWidgetObserver = new AppWidgetResetObserver();

    private LayoutInflater mInflater;

    public static Workspace mWorkspace;
    private View mQsbDivider;
    private View mDockDivider;
    private DragLayer mDragLayer;
    private DragController mDragController;

    private AppWidgetManager mAppWidgetManager;
    private LauncherAppWidgetHost mAppWidgetHost;

    private final ItemInfo mPendingAddInfo = new ItemInfo();
    private AppWidgetProviderInfo mPendingAddWidgetInfo;

    private final int[] mTmpAddItemCellCoordinates = new int[2];

    private FolderInfo mFolderInfo;

    private Hotseat mHotseat;
    private View mAllAppsButton;

    private SearchDropTargetBar mSearchDropTargetBar;
    private AppsCustomizeTabHost mAppsCustomizeTabHost;
    /**
     * 二级菜单图标
     */
    private AppsCustomizePagedView mAppsCustomizeContent;
    private boolean mAutoAdvanceRunning = false;

    private Bundle mSavedState;
    private State mOnResumeState = State.NONE;

    private SpannableStringBuilder mDefaultKeySsb = null;

    private boolean mWorkspaceLoading = true;

    private boolean mPaused = true;
    private boolean mRestoring;
    private boolean mWaitingForResult;
    private boolean mOnResumeNeedsLoad;

    // Keep track of whether the user has left launcher
    private static boolean sPausedFromUserAction = false;

    private LauncherModel mModel;
    private IconCache mIconCache;
    private boolean mUserPresent = true;
    private boolean mVisible = false;
    private boolean mAttached = false;

    private static LocaleConfiguration sLocaleConfiguration = null;

    private static final HashMap<Long, FolderInfo> sFolders = new HashMap<Long, FolderInfo>();

    private Intent mAppMarketIntent = null;

    // Related to the auto-advancing of widgets
    private final int ADVANCE_MSG = 1;
    private final int mAdvanceInterval = 20000;
    private static final int mAdvanceStagger = 250;
    private long mAutoAdvanceSentTime;
    private long mAutoAdvanceTimeLeft = -1;
    private final HashMap<View, AppWidgetProviderInfo> mWidgetsToAdvance = new HashMap<View, AppWidgetProviderInfo>();

    private final int mRestoreScreenOrientationDelay = 500;

    // External icons saved in case of resource changes, orientation, etc.
    private static Drawable.ConstantState[] sGlobalSearchIcon = new Drawable.ConstantState[2];
    private static Drawable.ConstantState[] sVoiceSearchIcon = new Drawable.ConstantState[2];
    private static Drawable.ConstantState[] sAppMarketIcon = new Drawable.ConstantState[2];

    private final ArrayList<Integer> mSynchronouslyBoundPages = new ArrayList<Integer>();

    final ArrayList<String> sDumpLogs = new ArrayList<String>();

    // We only want to get the SharedPreferences once since it does an FS stat
    // each time we get
    // it from the context.
    private SharedPreferences mSharedPrefs;

    // Holds the page that we need to animate to, and the icon views that we
    // need to animate up
    // when we scroll to that page on resume.
    private int mNewShortcutAnimatePage = -1;
    private ArrayList<View> mNewShortcutAnimateViews = new ArrayList<View>();
    private ImageView mFolderIconImageView;
    private Bitmap mFolderIconBitmap;
    private Canvas mFolderIconCanvas;
    private Rect mRectForFolderAnimation = new Rect();

    private BubbleTextView mWaitingForResume;

    private HideFromAccessibilityHelper mHideFromAccessibilityHelper = new HideFromAccessibilityHelper();


    public static ViewGroup mIndicatorDots;
    private ViewGroup mAppsCustomizeIndicatorDots;
    private final static int dot_gap = 16;
    private Paint mPaint = null;
    private Bitmap bmp_dot;
    private Bitmap bmp_dot_sel;
    public static IndicatorPanel mIndicatorPanel;
    public static AppsCustomizeIndicatorPanel mAppsCustomizeIndicatorPanel;
    private int mov_x = 0;
    Timer timer = new Timer();

    public static Boolean isSettingOrAndroidPage = false;

    private Handler mHandlerSpeed = null;
    static int canbus_set_dis = 0;

    private MediaBrowserCompat mMediaBrowser;
    private Context mContext;
    public static MediaNotificationListener mMediaListener;

    public static void setSettingOrAndroidPage(Boolean boolean1) {
        isSettingOrAndroidPage = boolean1;
    }

    public static Boolean getSettingOrAndroidPage() {
        return isSettingOrAndroidPage;
    }

    static int getCurrPage() {
        return mWorkspace.getCurrentPage();
    }

    private Runnable mBuildLayersRunnable = new Runnable() {
        public void run() {
            if (mWorkspace != null) {
                mWorkspace.buildPageHardwareLayers();
            }
        }
    };

    private static ArrayList<PendingAddArguments> sPendingAddList = new ArrayList<PendingAddArguments>();

    private static class PendingAddArguments {
        int requestCode;
        Intent intent;
        long container;
        int screen;
        int cellX;
        int cellY;
    }

    private boolean doesFileExist(String filename) {
        FileInputStream fis = null;
        try {
            fis = openFileInput(filename);
            fis.close();
            return true;
        } catch (java.io.FileNotFoundException e) {
            return false;
        } catch (java.io.IOException e) {
            return true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate start ...");
        if (DEBUG_STRICT_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork() // or
                    // .detectAll()
                    // for
                    // all
                    // detectable
                    // problems
                    .penaltyLog().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects().detectLeakedClosableObjects().penaltyLog().penaltyDeath().build());
        }

        super.onCreate(savedInstanceState);

        mContext = this;
        mMediaListener = new MediaNotificationListener();
        mMediaListener.initDependencies(mContext);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 0x10);
        }

		/*Intent service = new Intent("xy.android.intent.action_GLA_DATA");
		this.startService(service);*/
        LauncherApplication app = ((LauncherApplication) getApplication());
        mSharedPrefs = getSharedPreferences(LauncherApplication.getSharedPreferencesKey(), Context.MODE_PRIVATE);
        mModel = app.setLauncher(this);
        mIconCache = app.getIconCache();
        mDragController = new DragController(this);
        mInflater = getLayoutInflater();

        mAppWidgetManager = AppWidgetManager.getInstance(this);
        mAppWidgetHost = new LauncherAppWidgetHost(this, APPWIDGET_HOST_ID);
        mAppWidgetHost.startListening();

        // If we are getting an onCreate, we can actually preempt onResume and
        // unset mPaused here,
        // this also ensures that any synchronous binding below doesn't
        // re-trigger another
        // LauncherModel load.
        mPaused = false;

        if (PROFILE_STARTUP) {
            android.os.Debug.startMethodTracing(Environment.getExternalStorageDirectory() + "/launcher");
        }
        checkForLocaleChange();
        setContentView(R.layout.launcher);
        Log.i(TAG, "onCreate setContentView ...");
        setupViews();
        // showFirstRunWorkspaceCling();
        Log.i(TAG, "onCreate setupViews ...");
        registerContentObservers();

        lockAllApps();

        mSavedState = savedInstanceState;
        restoreState(mSavedState);

        // Update customization drawer _after_ restoring the states
        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.onPackagesUpdated();
        }

        if (PROFILE_STARTUP) {
            android.os.Debug.stopMethodTracing();
        }

        if (!mRestoring) {
            if (sPausedFromUserAction) {
                // If the user leaves launcher, then we should just load items
                // asynchronously when
                // they return.
                mModel.startLoader(true, -1);
            } else {
                // We only load the page synchronously if the user rotates (or
                // triggers a
                // configuration change) while launcher is in the foreground
                mModel.startLoader(true, mWorkspace.getCurrentPage());
            }
        }
        Log.i(TAG, "onCreate setupViews2 ...");
        if (!mModel.isAllAppsLoaded()) {
            ViewGroup appsCustomizeContentParent = (ViewGroup) mAppsCustomizeContent.getParent();
            mInflater.inflate(R.layout.apps_customize_progressbar, appsCustomizeContentParent);
        }

        // For handling default keys
        mDefaultKeySsb = new SpannableStringBuilder();
        Selection.setSelection(mDefaultKeySsb, 0);

        IntentFilter filter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mCloseSystemDialogsReceiver, filter);

        updateGlobalIcons();

        // On large interfaces, we want the screen to auto-rotate based on the
        // current orientation
        unlockScreenOrientation(true);

        startBinding();

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            handler.removeMessages(SPEEDHOME);
            handler.sendEmptyMessageDelayed(SPEEDHOME, 1000);
        }
        updateSpeedUnitText();
        Log.i(TAG, "onCreate end ...");

        Intent serviceIntent = new Intent(this, MediaNotificationListener.class);
        startForegroundService(serviceIntent);
    }

    private final int SPEEDHOME = 20;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0x10 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            handler.removeMessages(SPEEDHOME);
            handler.sendEmptyMessageDelayed(SPEEDHOME, 1000);
        }
    }

    private void unRegisterBroadcastReceiver() {

        unregisterReceiver(mainReceiver);
    }


    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        sPausedFromUserAction = true;
    }

    private void updateGlobalIcons() {
        boolean searchVisible = false;
        boolean voiceVisible = false;
        // If we have a saved version of these external icons, we load them up
        // immediately
        int coi = getCurrentOrientationIndexForGlobalIcons();
        if (sGlobalSearchIcon[coi] == null || sVoiceSearchIcon[coi] == null || sAppMarketIcon[coi] == null) {
            updateAppMarketIcon();
            searchVisible = updateGlobalSearchIcon();
            voiceVisible = updateVoiceSearchIcon(searchVisible);
        }
        if (sGlobalSearchIcon[coi] != null) {
            updateGlobalSearchIcon(sGlobalSearchIcon[coi]);
            searchVisible = true;
        }
        if (sVoiceSearchIcon[coi] != null) {
            updateVoiceSearchIcon(sVoiceSearchIcon[coi]);
            voiceVisible = true;
        }
        if (sAppMarketIcon[coi] != null) {
            updateAppMarketIcon(sAppMarketIcon[coi]);
        }
        if (mSearchDropTargetBar != null) {
            mSearchDropTargetBar.onSearchPackagesChanged(searchVisible, voiceVisible);
        }
    }

    private void checkForLocaleChange() {
        if (sLocaleConfiguration == null) {
            new AsyncTask<Void, Void, LocaleConfiguration>() {
                @Override
                protected LocaleConfiguration doInBackground(Void... unused) {
                    LocaleConfiguration localeConfiguration = new LocaleConfiguration();
                    readConfiguration(Launcher.this, localeConfiguration);
                    return localeConfiguration;
                }

                @Override
                protected void onPostExecute(LocaleConfiguration result) {
                    sLocaleConfiguration = result;
                    checkForLocaleChange(); // recursive, but now with a locale
                    // configuration
                }
            }.execute();
            return;
        }

        final Configuration configuration = getResources().getConfiguration();

        final String previousLocale = sLocaleConfiguration.locale;
        final String locale = configuration.locale.toString();

        final int previousMcc = sLocaleConfiguration.mcc;
        final int mcc = configuration.mcc;

        final int previousMnc = sLocaleConfiguration.mnc;
        final int mnc = configuration.mnc;

        boolean localeChanged = !locale.equals(previousLocale) || mcc != previousMcc || mnc != previousMnc;

        if (localeChanged) {
            sLocaleConfiguration.locale = locale;
            sLocaleConfiguration.mcc = mcc;
            sLocaleConfiguration.mnc = mnc;

            mIconCache.flush();

            final LocaleConfiguration localeConfiguration = sLocaleConfiguration;
            new Thread("WriteLocaleConfiguration") {
                @Override
                public void run() {
                    writeConfiguration(Launcher.this, localeConfiguration);
                }
            }.start();
        }
    }

    private static class LocaleConfiguration {
        public String locale;
        public int mcc = -1;
        public int mnc = -1;
    }

    private void readConfiguration(Context context, LocaleConfiguration configuration) {
        DataInputStream in = null;
        try {
            in = new DataInputStream(context.openFileInput(PREFERENCES));
            configuration.locale = in.readUTF();
            configuration.mcc = in.readInt();
            configuration.mnc = in.readInt();
        } catch (FileNotFoundException e) {
            // Ignore
        } catch (IOException e) {
            // Ignore
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    private void writeConfiguration(Context context, LocaleConfiguration configuration) {
        DataOutputStream out = null;
        try {
            out = new DataOutputStream(context.openFileOutput(PREFERENCES, MODE_PRIVATE));
            out.writeUTF(configuration.locale);
            out.writeInt(configuration.mcc);
            out.writeInt(configuration.mnc);
            out.flush();
        } catch (FileNotFoundException e) {
            // Ignore
        } catch (IOException e) {
            // noinspection ResultOfMethodCallIgnored
            context.getFileStreamPath(PREFERENCES).delete();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    public DragLayer getDragLayer() {
        return mDragLayer;
    }

    boolean isDraggingEnabled() {
        // We prevent dragging when we are loading the workspace as it is
        // possible to pick up a view
        // that is subsequently removed from the workspace in startBinding().
        return !mModel.isLoadingWorkspace();
    }

    public static int getScreen() {
        synchronized (sLock) {
            return sScreen;
        }
    }

    public static void setScreen(int screen) {
        synchronized (sLock) {
            sScreen = screen;
        }
    }

    /**
     * Returns whether we should delay spring loaded mode -- for shortcuts and
     * widgets that have a configuration step, this allows the proper animations
     * to run after other transitions.
     */
    private boolean completeAdd(PendingAddArguments args) {
        boolean result = false;
        switch (args.requestCode) {
            case REQUEST_PICK_APPLICATION:
                completeAddApplication(args.intent, args.container, args.screen, args.cellX, args.cellY);
                break;
            case REQUEST_PICK_SHORTCUT:
                processShortcut(args.intent);
                break;
            case REQUEST_CREATE_SHORTCUT:
                completeAddShortcut(args.intent, args.container, args.screen, args.cellX, args.cellY);
                result = true;
                break;
            case REQUEST_CREATE_APPWIDGET:
                int appWidgetId = args.intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                completeAddAppWidget(appWidgetId, args.container, args.screen, null, null);
                result = true;
                break;
            case REQUEST_PICK_WALLPAPER:
                // We just wanted the activity result here so we can clear
                // mWaitingForResult
                break;
        }
        // Before adding this resetAddInfo(), after a shortcut was added to a
        // workspace screen,
        // if you turned the screen off and then back while in All Apps,
        // Launcher would not
        // return to the workspace. Clearing mAddInfo.container here fixes this
        // issue
        resetAddInfo();
        return result;
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == REQUEST_BIND_APPWIDGET) {
            int appWidgetId = data != null ? data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) : -1;
            if (resultCode == RESULT_CANCELED) {
                completeTwoStageWidgetDrop(RESULT_CANCELED, appWidgetId);
            } else if (resultCode == RESULT_OK) {
                addAppWidgetImpl(appWidgetId, mPendingAddInfo, null, mPendingAddWidgetInfo);
            }
            return;
        }
        boolean delayExitSpringLoadedMode = false;
        boolean isWidgetDrop = (requestCode == REQUEST_PICK_APPWIDGET || requestCode == REQUEST_CREATE_APPWIDGET);
        mWaitingForResult = false;

        // We have special handling for widgets
        if (isWidgetDrop) {
            int appWidgetId = data != null ? data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) : -1;
            if (appWidgetId < 0) {
                Log.e(TAG, "Error: appWidgetId (EXTRA_APPWIDGET_ID) was not returned from the \\" + "widget configuration activity.");
                completeTwoStageWidgetDrop(RESULT_CANCELED, appWidgetId);
            } else {
                completeTwoStageWidgetDrop(resultCode, appWidgetId);
            }
            return;
        }

        // The pattern used here is that a user PICKs a specific application,
        // which, depending on the target, might need to CREATE the actual
        // target.

        // For example, the user would PICK_SHORTCUT for "Music playlist", and
        // we
        // launch over to the Music app to actually CREATE_SHORTCUT.
        if (resultCode == RESULT_OK && mPendingAddInfo.container != ItemInfo.NO_ID) {
            final PendingAddArguments args = new PendingAddArguments();
            args.requestCode = requestCode;
            args.intent = data;
            args.container = mPendingAddInfo.container;
            args.screen = mPendingAddInfo.screen;
            args.cellX = mPendingAddInfo.cellX;
            args.cellY = mPendingAddInfo.cellY;
            if (isWorkspaceLocked()) {
                sPendingAddList.add(args);
            } else {
                delayExitSpringLoadedMode = completeAdd(args);
            }
        }
        mDragLayer.clearAnimatedView();
        // Exit spring loaded mode if necessary after cancelling the
        // configuration of a widget
        exitSpringLoadedDragModeDelayed((resultCode != RESULT_CANCELED), delayExitSpringLoadedMode, null);
    }

    private void completeTwoStageWidgetDrop(final int resultCode, final int appWidgetId) {
        CellLayout cellLayout = (CellLayout) mWorkspace.getChildAt(mPendingAddInfo.screen);
        Runnable onCompleteRunnable = null;
        int animationType = 0;

        AppWidgetHostView boundWidget = null;
        if (resultCode == RESULT_OK) {
            animationType = Workspace.COMPLETE_TWO_STAGE_WIDGET_DROP_ANIMATION;
            final AppWidgetHostView layout = mAppWidgetHost.createView(this, appWidgetId, mPendingAddWidgetInfo);
            boundWidget = layout;
            onCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    completeAddAppWidget(appWidgetId, mPendingAddInfo.container, mPendingAddInfo.screen, layout, null);
                    exitSpringLoadedDragModeDelayed((resultCode != RESULT_CANCELED), false, null);
                }
            };
        } else if (resultCode == RESULT_CANCELED) {
            animationType = Workspace.CANCEL_TWO_STAGE_WIDGET_DROP_ANIMATION;
            onCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    exitSpringLoadedDragModeDelayed((resultCode != RESULT_CANCELED), false, null);
                }
            };
        }
        if (mDragLayer.getAnimatedView() != null) {
            mWorkspace.animateWidgetDrop(mPendingAddInfo, cellLayout, (DragView) mDragLayer.getAnimatedView(), onCompleteRunnable, animationType, boundWidget, true);
        } else {
            // The animated view may be null in the case of a rotation during
            // widget configuration
            onCompleteRunnable.run();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Restore the previous launcher state
        if (mOnResumeState == State.WORKSPACE) {
            showWorkspace(false);
        } else if (mOnResumeState == State.APPS_CUSTOMIZE) {
            showAllApps(false);
        }
        mOnResumeState = State.NONE;

        // Process any items that were added while Launcher was away
        InstallShortcutReceiver.flushInstallQueue(this);

        mPaused = false;
        sPausedFromUserAction = false;
        if (mRestoring || mOnResumeNeedsLoad) {
            mWorkspaceLoading = true;
            mModel.startLoader(true, -1);
            mRestoring = false;
            mOnResumeNeedsLoad = false;
        }

        // Reset the pressed state of icons that were locked in the press state
        // while activities
        // were launching
        if (mWaitingForResume != null) {
            // Resets the previous workspace icon press state
            mWaitingForResume.setStayPressed(false);
        }
        if (mAppsCustomizeContent != null) {
            // Resets the previous all apps icon press state
            mAppsCustomizeContent.resetDrawableState();
        }
        // It is possible that widgets can receive updates while launcher is not
        // in the foreground.
        // Consequently, the widgets will be inflated in the orientation of the
        // foreground activity
        // (framework issue). On resuming, we ensure that any widgets are
        // inflated for the current
        // orientation.
        getWorkspace().reinflateWidgetsIfNecessary();

        // Again, as with the above scenario, it's possible that one or more of
        // the global icons
        // were updated in the wrong orientation.
        updateGlobalIcons();

        Log.i(TAG, "onResume()=====mState=" + mState);
        /* hxw delete
        if (mState == State.WORKSPACE) {
            showWorkspace(true);
        }
         */
        updateGotoPageButtonStatus();

        startUIActivity();

    }

    /**
     * start UI activity
     */
    private void startUIActivity() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(this, UIActivity.class));
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        // NOTE: We want all transitions from launcher to act as if the
        // wallpaper were enabled
        // to be consistent. So re-enable the flag here, and we will re-disable
        // it as necessary
        // when Launcher resumes and we are still in AllApps.
        updateWallpaperVisibility(true);

        super.onPause();
        mPaused = true;
        mDragController.cancelDrag();
        mDragController.resetLastGestureUpTime();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        // Flag the loader to stop early before switching
        mModel.stopLoader();
        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.surrender();
        }
        return Boolean.TRUE;
    }

    // We can't hide the IME if it was forced open. So don't bother
    /*
     * @Override public void onWindowFocusChanged(boolean hasFocus) {
     * super.onWindowFocusChanged(hasFocus);
     *
     * if (hasFocus) { final InputMethodManager inputManager =
     * (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
     * WindowManager.LayoutParams lp = getWindow().getAttributes();
     * inputManager.hideSoftInputFromWindow(lp.token, 0, new
     * android.os.ResultReceiver(new android.os.Handler()) { protected void
     * onReceiveResult(int resultCode, Bundle resultData) { Log.d(TAG,
     * "ResultReceiver got resultCode=" + resultCode); } }); Log.d(TAG,
     * "called hideSoftInputFromWindow from onWindowFocusChanged"); } }
     */

    private boolean acceptFilter() {
        final InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        return !inputManager.isFullscreenMode();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        final int uniChar = event.getUnicodeChar();
        final boolean handled = super.onKeyDown(keyCode, event);
        final boolean isKeyNotWhitespace = uniChar > 0 && !Character.isWhitespace(uniChar);
        if (!handled && acceptFilter() && isKeyNotWhitespace) {
            boolean gotKey = TextKeyListener.getInstance().onKeyDown(mWorkspace, mDefaultKeySsb, keyCode, event);
            if (gotKey && mDefaultKeySsb != null && mDefaultKeySsb.length() > 0) {
                // something usable has been typed - start a search
                // the typed text will be retrieved and cleared by
                // showSearchDialog()
                // If there are multiple keystrokes before the search dialog
                // takes focus,
                // onSearchRequested() will be called for every keystroke,
                // but it is idempotent, so it's fine.
                return onSearchRequested();
            }
        }

        // Eat the long press event so the keyboard doesn't come up.
        if (keyCode == KeyEvent.KEYCODE_MENU && event.isLongPress()) {
            return true;
        }

        return handled;
    }

    private String getTypedText() {
        return mDefaultKeySsb.toString();
    }

    private void clearTypedText() {
        mDefaultKeySsb.clear();
        mDefaultKeySsb.clearSpans();
        Selection.setSelection(mDefaultKeySsb, 0);
    }

    /**
     * Given the integer (ordinal) value of a State enum instance, convert it to
     * a variable of type State
     */
    private static State intToState(int stateOrdinal) {
        State state = State.WORKSPACE;
        final State[] stateValues = State.values();
        for (int i = 0; i < stateValues.length; i++) {
            if (stateValues[i].ordinal() == stateOrdinal) {
                state = stateValues[i];
                break;
            }
        }
        return state;
    }

    /**
     * Restores the previous state, if it exists.
     *
     * @param savedState The previous state.
     */
    private void restoreState(Bundle savedState) {
        if (savedState == null) {
            return;
        }

        State state = intToState(savedState.getInt(RUNTIME_STATE, State.WORKSPACE.ordinal()));
        if (state == State.APPS_CUSTOMIZE) {
            mOnResumeState = State.APPS_CUSTOMIZE;
        }

      /*  int currentScreen = savedState.getInt(RUNTIME_STATE_CURRENT_SCREEN, -1);
        if (currentScreen > -1) {
            mWorkspace.setCurrentPage(currentScreen);
        }*/

        final long pendingAddContainer = savedState.getLong(RUNTIME_STATE_PENDING_ADD_CONTAINER, -1);
        final int pendingAddScreen = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SCREEN, -1);

        if (pendingAddContainer != ItemInfo.NO_ID && pendingAddScreen > -1) {
            mPendingAddInfo.container = pendingAddContainer;
            mPendingAddInfo.screen = pendingAddScreen;
            mPendingAddInfo.cellX = savedState.getInt(RUNTIME_STATE_PENDING_ADD_CELL_X);
            mPendingAddInfo.cellY = savedState.getInt(RUNTIME_STATE_PENDING_ADD_CELL_Y);
            mPendingAddInfo.spanX = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SPAN_X);
            mPendingAddInfo.spanY = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y);
            mPendingAddWidgetInfo = savedState.getParcelable(RUNTIME_STATE_PENDING_ADD_WIDGET_INFO);
            mWaitingForResult = true;
            mRestoring = true;
        }

        boolean renameFolder = savedState.getBoolean(RUNTIME_STATE_PENDING_FOLDER_RENAME, false);
        if (renameFolder) {
            long id = savedState.getLong(RUNTIME_STATE_PENDING_FOLDER_RENAME_ID);
            mFolderInfo = mModel.getFolderById(this, sFolders, id);
            mRestoring = true;
        }

        // Restore the AppsCustomize tab
        if (mAppsCustomizeTabHost != null) {
            String curTab = savedState.getString("apps_customize_currentTab");
            if (curTab != null) {
                mAppsCustomizeTabHost.setContentTypeImmediate(mAppsCustomizeTabHost.getContentTypeForTabTag(curTab));
                mAppsCustomizeContent.loadAssociatedPages(mAppsCustomizeContent.getCurrentPage());
            }

            int currentIndex = savedState.getInt("apps_customize_currentIndex");
            mAppsCustomizeContent.restorePageForIndex(currentIndex);
        }
    }

    private Uri someSettingUri = Settings.System.getUriFor("launcherTypeDN");
    private Uri radioUri = Settings.System.getUriFor("RadioIsLocOrDX");


    private ContentObserver settingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange);
            Log.i(TAG, "selfChange = " + selfChange + "uri = " + uri);
            if (uri.equals(someSettingUri)) {
//                int value = Settings.System.getInt(getContentResolver(), "launcherTypeDN", 0);
//                Log.d(TAG,"launcherTypeDN: "+ value);
//                dayNightImages(value == 1 ? 1 : 0);
//                handler.removeMessages(SWTWALLPAPER);
//                handler.obtainMessage(SWTWALLPAPER, value, 0).sendToTarget();
            } else if (uri.equals(radioUri)) {
                int mIsLocOrDX = android.provider.Settings.System.getInt(getContentResolver(), "RadioIsLocOrDX", 0);
                Log.i(TAG, "mIsLocOrDX = " + mIsLocOrDX);
                if (mIsLocOrDX == 0) {
                    tvRadioDxLoc.setText(R.string.radio_loc);
                } else if (mIsLocOrDX == 1) {
                    tvRadioDxLoc.setText(R.string.radio_dx);
                }
            }
        }
    };

    public void handleMediaPlaybackResult(String value1, String value2, int value3, int value4) {
        if (mMediaListener == null) {
            return;
        }

        String oldPlayingPackage = mMediaListener.getCurrentPlayingPackage();
        boolean isStartCommand = "start".equals(value2);
        boolean isStopCommand = "stop".equals(value2);
        boolean isValidPackage = !TextUtils.isEmpty(value1);
        Log.i(TAG, "handleMediaPlaybackResult-- MUSIC_MEDIA_PLAY:value1=" + value1 + " --oldPlayingPackage=" + oldPlayingPackage + "--value2=" + value2);
        Log.i(TAG, "handleMediaPlaybackResult-- MUSIC_MEDIA_PLAY:isValidPackage=" + isValidPackage + " --isStartCommand=" + isStartCommand + "-isStopCommand=" + isStopCommand);
        // 处理本地音乐的特殊情况
        if (isValidPackage && (value1.contains("localmusic") || value1.contains("com.awell.bluetooth") || value1.contains("/system/bin/gocsdk")) && isStartCommand) {
            mMediaListener.removeCallbacks();
            return;
        }

        // 处理停止播放的情况
        //if (isValidPackage && isStopCommand) {
        //    mMediaListener.setCurrentPlayingPackage(null);
        //    return;
        //}

        // 处理开始播放的情况
        if (isValidPackage && isStartCommand) {
            // 当前没有播放或切换到新包时，更新并启动回调
            if (oldPlayingPackage != null && !oldPlayingPackage.equals(value1)) {
                mMediaListener.togglePause();//有些播放器未暂停，手动暂停
                mMediaListener.removeCallbacks();
            }

            mMediaListener.setPlayingPackage(value1);
            mMediaListener.startCallbacks();
            //Log.i(TAG, "0000----Switched to new package: " + value1);
        }
    }

    public static AwellLibrary mediaLibrary;
    private final AwellLibrary.OnDataListener awellLibraryDataListener = new AwellLibrary.OnDataListener() {
        @Override
        public void onResult(Bundle bundle) {
            llMusic.post(() -> {
                if (bundle == null) return;
                String status = bundle.getString(AwellTool.STATUS_ACCEPT, AwellTool.DEFAULT_S);
                Log.i(TAG, "onResult---status = " + status + "  " + bundle);
                switch (status) {
                    case AwellTool.MEDIA_PLAY: {
                        String value1 = bundle.getString(AwellTool.VALUE_M1);
                        String value2 = bundle.getString(AwellTool.VALUE_M2);
                        int value3 = bundle.getInt(AwellTool.VALUE_M3, 3);
                        int value4 = bundle.getInt(AwellTool.VALUE_M4, MusicWidget.MUSIC);
                        //if(value1!=null&&!value1.contains("localmusic")&&"start".equals(value2)){
                        //    value4=MusicWidget.OTHER_MUSIC;
                        //}
                        Log.i(TAG, "onResult---MUSIC_MEDIA_PLAY : " + value1 + " " + value2 + " " + value3 + " " + value4);
                        Log.i(TAG, "onResult---MUSIC_MEDIA_PLAY : " + mMediaListener.getCurrentPlayingPackage());
                        //handleMediaPlaybackResult(value1,value2,value3,value4);
                        boolean isStartCommand = "start".equals(value2);
                        boolean isStopCommand = "stop".equals(value2);
                        boolean isValidPackage = !TextUtils.isEmpty(value1);
                        boolean isLocalMusicPackage = (value1.contains("localmusic") || value1.contains("com.awell.bluetooth") || value1.contains("/system/bin/gocsdk"));
                        if (isValidPackage && mMediaListener != null) {
                            if (isLocalMusicPackage && isStartCommand) {
                                mMediaListener.removeCallbacks();
                            } else if (!isLocalMusicPackage
                                    && value1.equals(mMediaListener.getCurrentPlayingPackage())
                                    && isStopCommand
                                    && llMusic != null) {
                                llMusic.setCurMusicState(false, MusicWidget.OTHER_MUSIC);
                            }
                        }
                        llMusic.switchMediaController(value1, value2, value3, value4);
                        if ("com.awell.radio".equals(value1)) {
                            if ("start".equals(value2)) {
                                mWaveformView.startAnimation();
                            } else {
                                mWaveformView.stopAnimation();
                            }
                        }
                    }
                    break;
                    case AwellTool.MUSIC.PLAY_STATUS: {
                        // 音乐监听 PLAY_STATUS 返回一个参数
                        // VALUE_M1 = (boolean)音乐播放状态 true 播放 false 没播放
                        boolean musicStatus = bundle.getBoolean(AwellTool.VALUE_M1);
                        Log.i(TAG, "onResult---MUSIC_PLAY_STATUS : " + musicStatus);
                        if (llMusic != null) {
                            llMusic.setCurMusicState(musicStatus, MusicWidget.MUSIC);
                        }
                    }
                    break;
                    case AwellTool.MUSIC.PLAY_NAME: {
                        // 音乐监听 PLAY_NAME 返回三个参数
                        // VALUE_M1 = (String)歌曲名称
                        // VALUE_M2 = (String)歌手名称
                        // VALUE_M3 = (String)专辑
                        String value1 = bundle.getString(AwellTool.VALUE_M1);
                        String value2 = bundle.getString(AwellTool.VALUE_M2);
                        String value3 = bundle.getString(AwellTool.VALUE_M3);
                        int value4 = bundle.getInt(AwellTool.VALUE_M4, MusicWidget.MUSIC);
                        Log.i(TAG, "onResult---MUSIC_PLAY_NAME : " + value1 + " " + value2 + " " + value3);
                        if (llMusic != null) {
                            llMusic.setMusicNameTextView(value1, MusicWidget.MUSIC);
                            llMusic.setArtistNameTextView(value2, MusicWidget.MUSIC);
                            if ("NO_MUSIC_LIST".equals(value1)
                                    && "NO_MUSIC_LIST".equals(value2)
                                    && "NO_MUSIC_LIST".equals(value3)) {
                                llMusic.setMusicNameTextView(getResources().getString(R.string.click_play_music), MusicWidget.MUSIC);
                                llMusic.setArtistNameTextView(getResources().getString(R.string.music_artist), MusicWidget.MUSIC);
                            }
                        }
                    }
                    break;
                    case AwellTool.MUSIC.PLAY_IMAGE: {
                        String value1 = bundle.getString(AwellTool.VALUE_M1);
                        Log.i(TAG, "onResult---MUSIC_PLAY_IMAGE " + value1);
                        String[] strs = value1.split(" , ");
                        long long1 = Long.parseLong(strs[0]);
                        long long2 = Long.parseLong(strs[1]);
                        llMusic.setPlayImage(long1, long2);
                    }
                    break;
                    case AwellTool.MUSIC.PLAY_TIME: {
                        // 音乐监听 PLAY_TIME 返回两个参数
                        // VALUE_M1 = (long)当期时间 单位毫秒
                        // VALUE_M2 = (long)总时间 单位毫秒
                        long value1 = bundle.getLong(AwellTool.VALUE_M1);
                        long value2 = bundle.getLong(AwellTool.VALUE_M2);
                        Log.i(TAG, "onResult---MUSIC_PLAY_TIME : " + value1 + "-" + value2);
                        if (llMusic != null) {
                            llMusic.setMusicSeekBar((int) value1, (int) value2, MusicWidget.MUSIC);
                        }
                    }
                    break;
                    case AwellTool.BT.PLAY_STATUS: {
                        // 蓝牙监听 PLAY_STATUS 返回一个参数
                        boolean musicStatus = bundle.getBoolean(AwellTool.VALUE_M1);
                        Log.i(TAG, "onResult---BT_PLAY_STATUS : " + musicStatus);
                        if (llMusic != null) {
                            llMusic.setCurMusicState(musicStatus, MusicWidget.BT);
                        }
                    }
                    break;
                    case AwellTool.BT.PLAY_NAME: {
                        // 音乐监听 PLAY_NAME 返回三个参数
                        // VALUE_M1 = (String)歌曲名称
                        // VALUE_M2 = (String)歌手名称
                        // VALUE_M3 = (String)专辑
                        String value1 = bundle.getString(AwellTool.VALUE_M1);
                        String value2 = bundle.getString(AwellTool.VALUE_M2);
                        String value3 = bundle.getString(AwellTool.VALUE_M3);
                        Log.i(TAG, "onResult---BT_PLAY_NAME : " + value1 + " " + value2 + " " + value3);
                        if (llMusic != null) {
                            llMusic.setMusicNameTextView(value1, MusicWidget.BT);
                            llMusic.setArtistNameTextView(value2, MusicWidget.BT);
                        }
                    }
                    break;
                    case AwellTool.BT.PLAY_TIME: {
                        // 音乐监听 PLAY_TIME 返回两个参数
                        // VALUE_M1 = (long)当期时间 单位秒
                        // VALUE_M2 = (long)总时间 单位秒
                        long value1 = bundle.getInt(AwellTool.VALUE_M1) * 1000;
                        long value2 = bundle.getInt(AwellTool.VALUE_M2) * 1000;
                        Log.i(TAG, "onResult---BT_MUSIC_PLAY_TIME : " + value1 + "-" + value2);
                        if (llMusic != null) {
                            llMusic.setMusicSeekBar((int) value1, (int) value2, MusicWidget.BT);
                        }
                    }
                    break;

                    case AwellTool.RADIO.FREQUENCY: {
                        // 收音机监听 FREQUENCY 返回三个参数
                        // VALUE_M1 = (String)FM or AM
                        // VALUE_M2 = (String)具体频率 87.5 or 531
                        // VALUE_M3 = (String)单位 MHz or KHz
                        String value1 = bundle.getString(AwellTool.VALUE_M1);
                        String value2 = bundle.getString(AwellTool.VALUE_M2);
                        String value3 = bundle.getString(AwellTool.VALUE_M3);
                        Log.i(TAG, value1 + " " + value2 + " " + value3);
                        tv_radio_am_fm.setText(value1);
                        tvRadioButAFM.setText(value1);
                        tv_radio_freq.setText(value2);
                        tv_radio_freq_unit.setText(value3);
                    }
                    break;
                    case MusicWidget.OTHER_MUSIC_PLAYNAME: {
                        // 音乐监听 PLAY_NAME 返回三个参数
                        // VALUE_M1 = (String)歌曲名称
                        // VALUE_M2 = (String)歌手名称
                        // VALUE_M3 = (String)专辑
                        String value1 = bundle.getString(AwellTool.VALUE_M1);
                        String value2 = bundle.getString(AwellTool.VALUE_M2);
                        String value3 = bundle.getString(AwellTool.VALUE_M3);
                        int value4 = bundle.getInt(AwellTool.VALUE_M4, MusicWidget.OTHER_MUSIC);
                        Log.i(TAG, "onResult---OTHER_MUSIC_PLAYNAME : " + value1 + " " + value2 + " " + value3);
                        if (llMusic != null) {
                            int currentMedia = MusicWidget.OTHER_MUSIC;

                            if ("NO_MUSIC_LIST".equals(value1)
                                    && "NO_MUSIC_LIST".equals(value2)
                                    && "NO_MUSIC_LIST".equals(value3)) {
                                llMusic.setMusicNameTextView(getResources().getString(R.string.click_play_music), currentMedia);
                                llMusic.setArtistNameTextView(getResources().getString(R.string.music_artist), currentMedia);
                            }

                            if (value1 != null && !TextUtils.isEmpty(value1)) {
                                llMusic.setMusicNameTextView(value1, currentMedia);
                            } else {
                                llMusic.setMusicNameTextView(getResources().getString(R.string.click_play_music), currentMedia);
                            }
                            if (value2 != null && !TextUtils.isEmpty(value2)) {
                                llMusic.setArtistNameTextView(value2, currentMedia);
                            } else {
                                llMusic.setArtistNameTextView(getResources().getString(R.string.music_artist), currentMedia);
                            }
                        }
                    }
                    break;
                    case MusicWidget.OTHER_MUSIC_PLAYSTATUS: {
                        // 音乐监听 PLAY_STATUS 返回一个参数
                        // VALUE_M1 = (boolean)音乐播放状态 true 播放 false 没播放
                        boolean musicStatus = bundle.getBoolean(AwellTool.VALUE_M1);
                        Log.i(TAG, "onResult---OTHER_MUSIC_PLAYSTATUS : " + musicStatus);
                        if (llMusic != null) {
                            llMusic.setCurMusicState(musicStatus, MusicWidget.OTHER_MUSIC);
                        }
                    }
                    break;
                    case MusicWidget.OTHER_MUSIC_TIME: {
                        // 音乐监听 PLAY_TIME 返回两个参数
                        // VALUE_M1 = (long)当期时间 单位毫秒
                        // VALUE_M2 = (long)总时间 单位毫秒
                        long value1 = bundle.getLong(AwellTool.VALUE_M1);
                        long value2 = bundle.getLong(AwellTool.VALUE_M2);
                        Log.i(TAG, "onResult---OTHER_MUSIC_TIME : " + value1 + "-" + value2);
                        if (llMusic != null) {
                            llMusic.setMusicSeekBar((int) value1, (int) value2, MusicWidget.OTHER_MUSIC);
                        }
                    }
                    break;
                }
            });
        }
    };


    /**
     * Finds all the views we need and configure them properly.
     */
    private void setupViews() {
        final DragController dragController = mDragController;

        mDragLayer = (DragLayer) findViewById(R.id.drag_layer);
        mWorkspace = (Workspace) mDragLayer.findViewById(R.id.workspace);
        mQsbDivider = (ImageView) findViewById(R.id.qsb_divider);
        mDockDivider = (ImageView) findViewById(R.id.dock_divider);

        // Setup the drag layer
        mDragLayer.setup(this, dragController);

        // Setup the hotseat
        mHotseat = (Hotseat) findViewById(R.id.hotseat);
        if (mHotseat != null) {
            mHotseat.setup(this);
        }

        // Setup the workspace
        mWorkspace.setHapticFeedbackEnabled(false);
        mWorkspace.setOnLongClickListener(this);
        mWorkspace.setup(dragController);
        dragController.addDragListener(mWorkspace);

        // Get the search/delete bar
        mSearchDropTargetBar = (SearchDropTargetBar) mDragLayer.findViewById(R.id.qsb_bar);

        // Setup AppsCustomize
        mAppsCustomizeTabHost = (AppsCustomizeTabHost) findViewById(R.id.apps_customize_pane);
        mAppsCustomizeContent = (AppsCustomizePagedView) mAppsCustomizeTabHost.findViewById(R.id.apps_customize_pane_content);
        mAppsCustomizeContent.setup(this, dragController);

        dragController.setDragScoller(mWorkspace);
        dragController.setScrollView(mDragLayer);
        dragController.setMoveTarget(mWorkspace);
        dragController.addDropTarget(mWorkspace);
        if (mSearchDropTargetBar != null) {
            mSearchDropTargetBar.setup(this, dragController);
        }

        // 二级菜单页面指示点
        mPaint = new Paint();
        Resources res = getResources();
        BitmapDrawable bmpDraw_sel = (BitmapDrawable) res.getDrawable(R.drawable.dot_nor3);
        bmp_dot_sel = bmpDraw_sel.getBitmap();
        BitmapDrawable bmpDraw = (BitmapDrawable) res.getDrawable(R.drawable.dot_sel3);
        bmp_dot = bmpDraw.getBitmap();

        mIndicatorDots = (ViewGroup) findViewById(R.id.mIndicatorDots);
        mIndicatorPanel = new IndicatorPanel(this);
        mIndicatorDots.addView(mIndicatorPanel);

        mAppsCustomizeIndicatorDots = (ViewGroup) mAppsCustomizeTabHost.findViewById(R.id.mIndicatorDots);
        mAppsCustomizeIndicatorPanel = new AppsCustomizeIndicatorPanel(this);
        mAppsCustomizeIndicatorDots.addView(mAppsCustomizeIndicatorPanel);

        initTimeReceiver();

        mediaLibrary = new AwellLibrary(AwellTool.OPEN);
        mediaLibrary.init(this);
        mediaLibrary.setOnDataListener(awellLibraryDataListener);
        wallpaperManager = (WallpaperManager) getSystemService(Context.WALLPAPER_SERVICE);

        findView();
        handler.removeMessages(INITVIEW);
//        handler.obtainMessage(INITVIEW).sendToTarget();
        handler.sendEmptyMessageDelayed(INITVIEW, 0);
    }

    private void findView() {
        /*viewPager = findViewById(R.id.viewpager);
        dots = new ImageView[dotsId.length];
        for (int i = 0; i < dotsId.length; i++) {
            dots[i] = findViewById(dotsId[i]);
        }
        viewpagerLayout1 = getLayoutInflater().inflate(R.layout.homepage_1, null);
        viewpagerLayout2 = getLayoutInflater().inflate(R.layout.homepage_2, null);
        viewpagerLayout3 = getLayoutInflater().inflate(R.layout.homepage_3, null);*/

        llMusic = findViewById(R.id.music_widget_layout);
        musicName = llMusic.findViewById(R.id.music_widget_music_name);
        /*
        date_bg_rl_h = viewpagerLayout2.findViewById(R.id.date_bg_rl_h);
        date_bg_rl_m = viewpagerLayout2.findViewById(R.id.date_bg_rl_m);
        date_bg_tc_h = viewpagerLayout2.findViewById(R.id.date_bg_tc_h);
        date_bg_tc_m = viewpagerLayout2.findViewById(R.id.date_bg_tc_m);
        date_bg_tc_mm = viewpagerLayout2.findViewById(R.id.date_bg_tc_mm);
        date_bg_tv_h = viewpagerLayout2.findViewById(R.id.date_bg_tv_h);
        date_bg_tv_m = viewpagerLayout2.findViewById(R.id.date_bg_tv_m);*/

        radio_rl = findViewById(R.id.radio_rl);
        //radio_rl.setOnClickListener(this);
        radio_iv = findViewById(R.id.radio_iv);
        radio_iv.setOnClickListener(this);
        radioPreIv = findViewById(R.id.iv_radio_pre);
        radioPreIv.setOnClickListener(this);
        radioNextIv = findViewById(R.id.iv_radio_next);
        radioNextIv.setOnClickListener(this);
        tv_radio_am_fm = findViewById(R.id.tv_radio_am_fm);
        tv_radio_am_fm.setOnClickListener(this);
        tv_radio_freq = findViewById(R.id.tv_radio_freq);
        tv_radio_freq.setOnClickListener(this);
        tv_radio_freq_unit = findViewById(R.id.tv_radio_freq_unit);
        radio_control_ll = findViewById(R.id.radio_control_ll);
        tvRadioButAFM = findViewById(R.id.iv_radio_setFM);
        tvRadioButAFM.setOnClickListener(this);
        iv_radio_setSearch = findViewById(R.id.iv_radio_setSearch);
        iv_radio_setSearch.setOnClickListener(this);
        tvRadioDxLoc = findViewById(R.id.iv_radio_setYC);
        tvRadioDxLoc.setOnClickListener(this);
/*
        time_widget_change = findViewById(R.id.time_widget_change);
        time_widget_change.setOnLongClickListener(this);
        time_ll_one = findViewById(R.id.time_ll_one);
        myQAnalogClock = findViewById(R.id.time_biao);

        biaopan_one_iv = findViewById(R.id.biaopan_one_iv);
        myQAnalogClock1 = findViewById(R.id.time_biao1);
        page2_date_m = findViewById(R.id.page2_date_m);
        page2_date_d = findViewById(R.id.page2_date_d);

        small_calendar_layout = findViewById(R.id.small_calendar_layout);
        small_calendar_month = findViewById(R.id.small_calendar_month);
        small_calendar_day = findViewById(R.id.small_calendar_day);
        small_calendar_year = findViewById(R.id.small_calendar_year);

        contentView = findViewById(android.R.id.content);
        calendar  = findViewById(R.id.calendarView);
        time_ll = new LinearLayout[time_ll_id.length];
        for (int i = 0; i < time_ll_id.length; i++) {
            time_ll[i] = findViewById(time_ll_id[i]);
        }
*/
        rv_layout = findViewById(R.id.rv_layout);
        contentView = findViewById(android.R.id.content);
        rvshowAllApp = findViewById(R.id.rv_showapp);

        findViewById(R.id.hotset_setting).setOnClickListener(this);
        findViewById(R.id.hotset_bluetooth).setOnClickListener(this);
        findViewById(R.id.hotset_navi).setOnClickListener(this);
        findViewById(R.id.hotset_allapp).setOnClickListener(this);
        findViewById(R.id.hotset_dianhua).setOnClickListener(this);
        findViewById(R.id.hotset_video).setOnClickListener(this);
        hotset_moshi = findViewById(R.id.hotset_moshi);
        hotset_moshi.setOnClickListener(this);

        //findViewById(R.id.rl_car).setOnClickListener(this);
        ivAnimTwo = findViewById(R.id.iv_anim_speed_road);
        ivLampSwitchBg = findViewById(R.id.iv_main_xiaodeng);
        //ivLampSwitchBg.setOnClickListener(this);
        tvGPSSpeed = findViewById(R.id.tv_gps_speed);
        tvGPSSpeedUnit = findViewById(R.id.tv_gps_speed_unit);

        mWaveformView = findViewById(R.id.waveformView);
        mWaveformView.setOnClickListener(this);
    }

    private WallpaperManager wallpaperManager;
    private ViewPager viewPager;
    private List<View> viewsList;
    private View viewpagerLayout1, viewpagerLayout2, viewpagerLayout3;
    private int[] dotsId = {R.id.dot_1, R.id.dot_2, R.id.dot_3};
    private ImageView[] dots;
    private PagerAdapter pagerAdapter;
    private final int INITVIEW = 111;

    private void initViewPager() {
        viewsList = new ArrayList<>();
        viewsList.add(viewpagerLayout1);
        viewsList.add(viewpagerLayout3);
        viewsList.add(viewpagerLayout2);

        pagerAdapter = new PagerAdapter() {
            @Override
            public int getCount() {
                return viewsList.size();
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
                return view == object;
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                container.addView(viewsList.get(position));
                return viewsList.get(position);
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                container.removeView(viewsList.get(position));
            }
        };
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                Log.i(TAG, "position = " + position);
                for (int i = 0; i < dots.length; i++) {
                    dots[i].setBackgroundResource(position == i ? R.drawable.sel_o : R.drawable.nor_t);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        viewPager.setPageTransformer(true, new MyPageTransformer2());
//        viewPager.setCurrentItem(0);
    }

    private MusicWidget llMusic;
    private RelativeLayout date_bg_rl_h, date_bg_rl_m;
    private TextClock date_bg_tc_h, date_bg_tc_m, date_bg_tc_mm;
    private TextView date_bg_tv_h, date_bg_tv_m;
    //private TextView musicName;
    private EarqueeTextView musicName;

    private void initMusicWidget() {
        llMusic.setActivity(this, llMusic);
    }

    private LinearLayout radio_rl, radio_control_ll;
    private ImageView radioPreIv, radioNextIv, radio_iv;
    private FrequencyTextView tv_radio_freq;
    private TextView tv_radio_am_fm, tv_radio_freq_unit;
    private Button tvRadioButAFM, iv_radio_setSearch, tvRadioDxLoc;

    private VisualizerView mWaveformView;

    private LinearLayout mMusicEntry;

    private void initRadioWidget() {
        int mIsLocOrDX = android.provider.Settings.System.getInt(getContentResolver(), "RadioIsLocOrDX", 1);
        int mIsFMOrAM = android.provider.Settings.System.getInt(getContentResolver(), "RadioIsFMOrAM", 0);
        Log.d(TAG, "mIsFMOrAM:" + mIsFMOrAM + " mIsLocOrDX:" + mIsLocOrDX);
        if (mIsLocOrDX == 0) {
            tvRadioDxLoc.setText(R.string.radio_loc);
        } else if (mIsLocOrDX == 1) {
            tvRadioDxLoc.setText(R.string.radio_dx);
        }
        if (mIsFMOrAM == 0) {
            tv_radio_am_fm.setText("FM");
            tvRadioButAFM.setText("FM");
            tv_radio_freq_unit.setText("MHz");
        } else if (mIsFMOrAM == 1) {
            tv_radio_am_fm.setText("AM");
            tvRadioButAFM.setText("AM");
            tv_radio_freq_unit.setText("KHz");
        }
        getContentResolver().registerContentObserver(someSettingUri, false, settingsObserver);
        getContentResolver().registerContentObserver(radioUri, false, settingsObserver);

    }

    private MyQAnalogClock myQAnalogClock, myQAnalogClock1;
    private int[] time_ll_id = new int[]{R.id.time_ll_one, R.id.time_ll_two,
            R.id.time_ll_three, R.id.time_ll_four};
    private LinearLayout[] time_ll;

    private int[] time_img_id = new int[]{R.id.time_w_one_img, R.id.time_w_two_img,
            R.id.time_w_three_img, R.id.time_w_four_img};
    private ImageView[] time_img;
    private View contentView, layoutView3;
    private NewCalendar calendar;
    private RelativeLayout time_widget_change;
    private LinearLayout time_ll_one, small_calendar_layout;
    private ImageView biaopan_one_iv;
    private TextClock page2_date_m, page2_date_d, small_calendar_month, small_calendar_day, small_calendar_year;

    private void initClock() {
        layoutView3 = LayoutInflater.from(this).inflate(R.layout.layout_pop_time_widget, null);
        time_img = new ImageView[time_img_id.length];
        for (int i = 0; i < time_img_id.length; i++) {
            time_img[i] = layoutView3.findViewById(time_img_id[i]);
            time_img[i].setOnClickListener(this);
        }

        int timeIndex = mSharedPrefs.getInt("time_widget_index", 0);
        Log.d(TAG, "time_widget_index:" + timeIndex);
        setTimeWidgetVisible(timeIndex);

        placehodlerInfo = new AppInfo();
    }

    private void setTimeWidgetVisible(int timeIndex) {
        for (int i = 0; i < time_ll_id.length; i++) {
            if (timeIndex == i) {
                time_ll[i].setVisibility(View.VISIBLE);
            } else {
                time_ll[i].setVisibility(View.GONE);
            }
        }
    }

    private PopupWindow popupWindow3;

    private void showPopupTimeWidget() {
        View view = layoutView3;
        popupWindow3 = new PopupWindow(view, RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT);
        popupWindow3.setOutsideTouchable(true);
        //popupWindow3.setFocusable(true);
        popupWindow3.setOnDismissListener(() -> {
            backgroundAlpha(1.0f);
        });
        popupWindow3.showAtLocation(contentView, Gravity.CENTER, 0, 10);
    }

    private void backgroundAlpha(float alpha) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = alpha; //0.0-1.0
        getWindow().setAttributes(lp);
    }

    private void setTimeWidgetIndex(int index) {
        if (popupWindow3 != null) {
            popupWindow3.dismiss();
        }
        SharedPreferences.Editor editor = mSharedPrefs
                .edit();
        editor.putInt("time_widget_index", index);
        editor.commit();
    }

    private LinearLayout rv_layout;
    private RecyclerView rvshowAllApp;
    private LinearLayoutManager linearLayoutManager;
    private AppInofAdapter appInofAdapter;
    private List<AppInfo> allAppInfoList, showAppInfoLis;
    private final int INITADDAPPVIEW = 101;
    private AppInfo placehodlerInfo;
    private MyDbHelper myDbHelper;
    private SQLiteDatabase sqLiteDatabase;

    private void initAddAppView() {
        placehodlerInfo.setLabel(getString(R.string.icon_add_name));
        allAppInfoList = getAllAppInfo(Launcher.this, false);
        showAppInfoLis = new ArrayList<>();
        // 获取已保存需要显示的app包名，如果没有，则显示默认
        myDbHelper = new MyDbHelper(this, "show_app", null, 1);
        sqLiteDatabase = myDbHelper.getWritableDatabase();
        List<String> stroageAppList = new ArrayList<>();
        Cursor cursor = sqLiteDatabase.query("showapp", null, null, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range")
                String packageName = cursor.getString(cursor.getColumnIndex("packagename"));
                stroageAppList.add(packageName);
                // 删除记录
                sqLiteDatabase.delete("showapp", "packagename=?", new String[]{packageName});
            }
            for (String packageName : stroageAppList) {
                AppInfo appInfo1 = Utils.getAppInfoFromPackage(packageName, allAppInfoList);
                if (appInfo1 != null) {
                    showAppInfoLis.add(appInfo1);
                }
            }
            cursor.close();
        }
        Log.i(TAG, "show_app cursor");
        // 如果数据库中没有数据，加载默认数据
        if (showAppInfoLis.size() == 0)
            for (String packName : Utils.defaultShowApp) {
                AppInfo appInfo = Utils.getAppInfoFromPackage(packName, allAppInfoList);
                if (appInfo != null) {
                    showAppInfoLis.add(Utils.getAppInfoFromPackage(packName, allAppInfoList));
                }
            }
        // 添加到数据库
        for (AppInfo storagePac : showAppInfoLis) {
            if (storagePac != null) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("packagename", storagePac.package_name);
                sqLiteDatabase.insert("showapp", null, contentValues);
            }
        }

        showAppInfoLis.add(placehodlerInfo);
        appInofAdapter = new AppInofAdapter(this, showAppInfoLis, showPopupI, addSelectAppCallback);
        rvshowAllApp.setAdapter(appInofAdapter);
        linearLayoutManager = new GridLayoutManager(this, 3);
        rvshowAllApp.setLayoutManager(linearLayoutManager);
    }

    private ShowPopupI showPopupI = new ShowPopupI() {
        @Override
        public void showPopup() {
            showPopupAllapp();
            backgroundAlpha(0.2f);
        }

        @Override
        public void hidePopup() {
            if (popupWindow != null)
                popupWindow.dismiss();
        }
    };

    private AddSelectAppCallback addSelectAppCallback = new AddSelectAppCallback() {
        @Override
        public void addAppInfo(AppInfo appInfo) {
            for (AppInfo pcka : showAppInfoLis)
                if (pcka.package_name.equals(appInfo.package_name)) {
                    if (popupWindow != null)
                        popupWindow.dismiss();

                    showPopupI.hidePopup();
                    return;
                }
            showAppInfoLis.remove(placehodlerInfo);

            showAppInfoLis.add(appInfo);
            showAppInfoLis.add(placehodlerInfo);
            appInofAdapter.setContentList(showAppInfoLis);
            appInofAdapter.notifyDataSetChanged();

            new Thread(() -> {
                ContentValues contentValues = new ContentValues();
                contentValues.put("packagename", appInfo.package_name);
                sqLiteDatabase.insert("showapp", null, contentValues);
            }).start();

            showPopupI.hidePopup();
        }

        @Override
        public void removeAppInfo(String packageName) {
            sqLiteDatabase.delete("showapp", "packagename=?", new String[]{packageName});
            for (AppInfo pcka : showAppInfoLis)
                if (pcka.package_name.equals(packageName)) {
                    showAppInfoLis.remove(pcka);
                    break;
                }

            if (appInofAdapter != null) {
                appInofAdapter.setContentList(showAppInfoLis);
            }
        }
    };

    PopupWindow popupWindow;

    /**
     * popupwindow列表显示所有已安装应用
     */
    private void showPopupAllapp() {

        View view = LayoutInflater.from(this).inflate(R.layout.layout_allapp, null);
        popupWindow = new PopupWindow(view, RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT);

        popupWindow.setOutsideTouchable(true);
        //popupWindow.setFocusable(true);

        allAppInfoList = getAllAppInfo(this, false);

        RecyclerView rvPop = view.findViewById(R.id.rv_pop_allapp);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 4);
        gridLayoutManager.setSpanCount(2);
        gridLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
        AppPopAdapter appInofAdapter = new AppPopAdapter(this, allAppInfoList, addSelectAppCallback);
        rvPop.setLayoutManager(gridLayoutManager);
        rvPop.setAdapter(appInofAdapter);

        popupWindow.setOnDismissListener(() -> {
            backgroundAlpha(1.0f);
        });

        popupWindow.showAtLocation(contentView, Gravity.CENTER, 0, 10);

    }

    /**
     * 获取手机已安装应用列表
     *
     * @param ctx
     * @param isFilterSystem 是否过滤系统应用
     * @return
     */
    private ArrayList<AppInfo> getAllAppInfo(Context ctx, boolean isFilterSystem) {
        ArrayList<AppInfo> appBeanList = new ArrayList<>();
        AppInfo bean = null;
        PackageManager packageManager = ctx.getPackageManager();
        List<PackageInfo> list = packageManager.getInstalledPackages(0);
        for (PackageInfo p : list) {
            bean = new AppInfo();
            int randome = new Random().nextInt(5);
            bean.setIcon(p.applicationInfo.loadIcon(packageManager));
            bean.setLabel(packageManager.getApplicationLabel(p.applicationInfo).toString());
            String pckaName = p.applicationInfo.packageName;
            bean.setPackage_name(pckaName);
            int flags = p.applicationInfo.flags;
            bean.setFlags(flags);
            if ((flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 &&
                    Utils.needToShowPackageName.contains(pckaName)) {
                appBeanList.add(bean);
            } else if ((flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0
                    && !Utils.filterAppPackageName.contains(pckaName)) {
                appBeanList.add(bean);
            } else if ((flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                    && Utils.otherNeedToShowPackageName.contains(pckaName)) {
                appBeanList.add(bean);
            }
        }


        for (AppInfo a : appBeanList) {
            String str = a.getPackage_name();
            for (int i = 0; i < Utils.mHomePackName.length; i++) {
                if (Utils.mHomePackName[i].equals(str)) {
                    a.setIcon(getResources().getDrawable(Utils.mHomeIcon[i]));
                    break;
                }
            }
        }
        return appBeanList;
    }

    private ImageView ivAnimTwo, ivLampSwitchBg, hotset_moshi;
    private TextView tvGPSSpeed, tvGPSSpeedUnit;
    private final int ADDCARANIM = 120;
    private boolean accRecor;

    private void initCarView() {

        ivLampSwitchBg.postDelayed(() -> accRecor = false, 8 * 1000);

        if (Settings.System.getInt(getContentResolver(), "Headlamp", 0) == 1)
            ivLampSwitchBg.setImageResource(R.drawable.open);
        else
            ivLampSwitchBg.setImageResource(R.drawable.off);

        //handler.removeMessages(ADDCARANIM);
        //handler.sendEmptyMessageDelayed(ADDCARANIM, 2000);
    }

    private AnimationDrawable animationDrawableTwo;

    private void addCarAnim() {
        ivAnimTwo.setBackgroundResource(R.drawable.sf_speed_line_two);
        animationDrawableTwo = (AnimationDrawable) ivAnimTwo.getBackground();
//        animationDrawableTwo.start();
        byte[] unit = new byte[1];
        CommonData.readDataToMeta(unit, 0x84);
        int unitData = unit[0];
        Log.e(TAG, "unit Data = " + unitData);
        if (unitData == 0) {
            tvGPSSpeedUnit.setText("KM/h");
        } else if (unitData == 1) {
            tvGPSSpeedUnit.setText("mph");
        }
    }

    @SuppressLint("HandlerLeak")
    private void speedhome() {
        Log.i(TAG, "speed come in");
        mHandlerSpeed = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_UPDATE_SPEED:
                        int speed = msg.arg1;
                        tvGPSSpeed.setText("" + speed);
                        /**
                         * gps车速
                         */
                        int speed_km = msg.arg1;
                        int speed_mile = msg.arg2;
                        Log.i(TAG, "accRecor = " + accRecor);
                        if (!accRecor) {
                            byte[] unit = new byte[1];
                            CommonData.readDataToMeta(unit, 0x84);
                            int unitData = unit[0];
                            Log.e(TAG, "unit Data = " + unitData);
                            if (unitData == 0) {
                                tvGPSSpeed.setText(speed_km + "");
                                tvGPSSpeedUnit.setText("KM/h");
                            } else if (unitData == 1) {
                                tvGPSSpeed.setText(speed_mile + "");
                                tvGPSSpeedUnit.setText("mph");
                            }
                            if (animationDrawableTwo != null) {
                                if (speed_km > 0 || speed_mile > 0) {
                                    animationDrawableTwo.start();
                                } else {
                                    animationDrawableTwo.stop();
                                }
                            }
                        }
                        mHandlerSpeed.removeMessages(MSG_CLEAR_SPEED);
                        mHandlerSpeed.sendEmptyMessageDelayed(MSG_CLEAR_SPEED, 2000);
                        break;
                    case MSG_CLEAR_SPEED:
                        tvGPSSpeed.setText("" + 0);
                        if (animationDrawableTwo != null) {
                            animationDrawableTwo.stop();
                        }
                        break;
                }
            }
        };
        LocationManager mlocationManager = null;
        if (null == mlocationManager)
            mlocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Log.i(TAG, "mlocationManager==" + mlocationManager);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mlocationManager.requestLocationUpdates("gps", 1000, 10, new LocationListener() {

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }

            @Override
            public void onLocationChanged(Location location) {

                if (location != null && location.hasSpeed()) {

                    int speed = (int) (location.getSpeed() * 3.6);// m/s ---> km/h
                    int speedMile = (int) (speed / 1.6093);// km/h  ---> miles/h
                    Log.i(TAG, "onLocationChanged: float speed = " + speed);
                    Log.i(TAG, "onLocationChanged: float speedMile = " + speedMile);


                    Message msg = mHandlerSpeed.obtainMessage();
                    msg.what = MSG_UPDATE_SPEED;
                    msg.arg1 = speed;
                    msg.arg2 = speedMile;
                    mHandlerSpeed.sendMessage(msg);
                }
            }
        }, mHandlerSpeed.getLooper());
    }

    int[] hotset_moshiID = {R.drawable.hotset_moshi, R.drawable.hotset_moshi1};
    int[] sf_widget_musicID = {R.drawable.sf_widget_music_bg, R.drawable.sf_widget_music_bg_n};
    int[] date_bgID = {R.drawable.date_bg, R.drawable.date_bg_n};
    int[] radio_bar_icon_bgID = {R.drawable.radio_bar_icon_bg, R.drawable.radio_bar_icon_bg_n};
    int[] sf_radio_preID = {R.drawable.sf_radio_pre, R.drawable.sf_radio_pre_n};
    int[] sf_radio_nextID = {R.drawable.sf_radio_next, R.drawable.sf_radio_next_n};
    int[] date_bg_tvID = {Color.parseColor("#D5D7DD"), Color.parseColor("#141414")};
    int[] textColorId = {Color.parseColor("#99ffffff"), Color.parseColor("#99ffffff")};
    int[] textColorId1 = {Color.WHITE, Color.BLACK};
    int[] sf_time_bg_mainID = {R.drawable.sf_time_bg_main, R.drawable.sf_time_bg_main_n};
    int[] biaopanID = {R.drawable.biaopan, R.drawable.biaopan_n};
    int[] biaopan_oneID = {R.drawable.biaopan_one, R.drawable.biaopan_one_n};
    int[] small_calendar_layoutID = {R.drawable.four_rili_bg, R.drawable.four_rili_bg_n};
    int[][] time_imgID = {{R.drawable.time_w_one, R.drawable.time_w_one_night},
            {R.drawable.time_w_two, R.drawable.time_w_two_night},
            {R.drawable.time_w_three, R.drawable.time_w_three_night},
            {R.drawable.time_w_four, R.drawable.time_w_four_night}};
    int[] sf_app_bg_mainID = {R.drawable.sf_app_bg_main, R.drawable.sf_app_bg_main_n};
    int[] sf_add2_dID = {R.drawable.sf_add2_d, R.drawable.sf_add2_n};

    private void dayNightImages(int dayNight) {
        hotset_moshi.setImageResource(hotset_moshiID[dayNight]);
        //viewpagerLayout1.setBackgroundResource(sf_widget_musicID[dayNight]);
        //viewpagerLayout2.setBackgroundResource(sf_widget_musicID[dayNight]);
        //viewpagerLayout3.setBackgroundResource(sf_widget_musicID[dayNight]);
        llMusic.setDayNight(dayNight);

        radio_iv.setBackgroundResource(radio_bar_icon_bgID[dayNight]);
        radioPreIv.setImageResource(sf_radio_preID[dayNight]);
        radioNextIv.setImageResource(sf_radio_nextID[dayNight]);
        tv_radio_am_fm.setTextColor(textColorId[dayNight]);
        tv_radio_freq.setFlag(dayNight);
        tv_radio_freq_unit.setTextColor(textColorId[dayNight]);
        radio_control_ll.setBackgroundResource(date_bgID[dayNight]);
        tvRadioButAFM.setTextColor(textColorId[dayNight]);
        iv_radio_setSearch.setTextColor(textColorId[dayNight]);
        tvRadioDxLoc.setTextColor(textColorId[dayNight]);
/*
        date_bg_rl_h.setBackgroundResource(date_bgID[dayNight]);
        date_bg_rl_m.setBackgroundResource(date_bgID[dayNight]);
        date_bg_tc_h.setTextColor(textColorId[dayNight]);
        date_bg_tc_m.setTextColor(textColorId[dayNight]);
        date_bg_tc_mm.setTextColor(textColorId[dayNight]);
        date_bg_tv_h.setBackgroundColor(date_bg_tvID[dayNight]);
        date_bg_tv_m.setBackgroundColor(date_bg_tvID[dayNight]);

        time_widget_change.setBackgroundResource(sf_time_bg_mainID[dayNight]);
        time_ll_one.setBackgroundResource(biaopanID[dayNight]);
        myQAnalogClock.setValue(dayNight);

        biaopan_one_iv.setBackgroundResource(biaopan_oneID[dayNight]);
        myQAnalogClock1.setValue(dayNight);
        page2_date_m.setTextColor(textColorId[dayNight]);
        page2_date_d.setTextColor(textColorId[dayNight]);

        calendar.renderCalendar(dayNight);

        small_calendar_layout.setBackgroundResource(small_calendar_layoutID[dayNight]);
        small_calendar_month.setTextColor(textColorId1[dayNight]);
        small_calendar_day.setTextColor(textColorId1[dayNight]);
        small_calendar_year.setTextColor(textColorId[dayNight]);

        for (int i = 0; i < time_img.length; i++) {
            time_img[i].setBackgroundResource(time_imgID[i][dayNight]);
        }

        rv_layout.setBackgroundResource(sf_app_bg_mainID[dayNight]);
        placehodlerInfo.setIcon(getDrawable(sf_add2_dID[dayNight]));
        if (appInofAdapter != null)
            appInofAdapter.notifyDataSetChanged();
*/
        //tvGPSSpeed.setTextColor(textColorId[dayNight]);
        //tvGPSSpeedUnit.setTextColor(textColorId[dayNight]);
        tvGPSSpeed.setTextColor(Color.parseColor("#ccffffff"));
        tvGPSSpeedUnit.setTextColor(Color.parseColor("#ccffffff"));
    }

    int[] wallpaperID = {R.drawable.wallpaper_00, R.drawable.wallpaper_01};
    private final int SWTWALLPAPER = 50;

    private void setWallpaper(int dayNight) {
        try {
            Log.i(TAG, "dayNight = " + dayNight);
            wallpaperManager.setResource(wallpaperID[dayNight]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.i(TAG, "handlerNew msg.what = " + msg.what);
            switch (msg.what) {
                case 100:
                    break;
                case INITVIEW:
                    //initViewPager();
                    initMusicWidget();
                    initRadioWidget();
                    //initClock();
                    initCarView();
                    int value = Settings.System.getInt(getContentResolver(), "launcherTypeDN", 0);
                    dayNightImages(value == 1 ? 1 : 0);

                    //handler.removeMessages(INITADDAPPVIEW);
                    //handler.sendEmptyMessageDelayed(INITADDAPPVIEW, 1000);
                    break;
                case INITADDAPPVIEW:
                    //initAddAppView();
                    break;
                case ADDCARANIM:
                    //addCarAnim();
                    break;
                case SPEEDHOME:
                    speedhome();
                    break;
                case SWTWALLPAPER:
                    setWallpaper(msg.arg1);
                    break;
                default:
                    break;
            }
        }
    };


    // workspace 页码图标
    class IndicatorPanel extends View {
        public IndicatorPanel(Context context) {
            super(context);
        }

        public void onDraw(Canvas canvas) {
            // canvas.drawColor(Color.BLACK);
            mPaint.setAntiAlias(true);
            canvas.clipRect(0, 0, mIndicatorDots.getWidth(), mIndicatorDots.getHeight());

            canvas.save();
            canvas.rotate(0.0f);
            // mPaint.setColor(Color.RED);

            int bmp_dot_w = bmp_dot.getWidth();
            bmp_dot.getHeight();
            int start_x = (mIndicatorDots.getWidth() - (bmp_dot_w + dot_gap) * mWorkspace.getPageCount() + dot_gap) / 2;
            // Log.e(TAG,
            // "mIndicatorDots.getWidth()="+mIndicatorDots.getWidth()+",bmp_dot_w="+bmp_dot_w+",mWorkspace.getPageCount()="+mWorkspace.getPageCount());
            // Log.e(TAG, "start_x="+start_x);
            int start_x_tmp = start_x;

            for (int i = 0; i < mWorkspace.getPageCount(); i++) {
                canvas.drawBitmap(bmp_dot, start_x_tmp, (mIndicatorDots.getHeight() - bmp_dot.getHeight()) / 2, mPaint);
                start_x_tmp += bmp_dot_w + dot_gap;
            }

            canvas.drawBitmap(bmp_dot_sel, start_x + (bmp_dot_w + dot_gap) * mWorkspace.getNextPage() + mov_x, (mIndicatorDots.getHeight() - bmp_dot.getHeight()) / 2, mPaint);

            canvas.restore();
        }
    }

    // all app 页码图标
    class AppsCustomizeIndicatorPanel extends View {
        public AppsCustomizeIndicatorPanel(Context context) {
            super(context);
        }

        public void onDraw(Canvas canvas) {

            // canvas.drawColor(Color.BLACK);
            mPaint.setAntiAlias(true);

            canvas.clipRect(0, 0, mAppsCustomizeIndicatorDots.getWidth(), mAppsCustomizeIndicatorDots.getHeight());

            canvas.save();

            canvas.rotate(0.0f);

            //mPaint.setColor(Color.RED);

            int bmp_dot_w = bmp_dot.getWidth();
            bmp_dot.getHeight();
            int start_x = (mAppsCustomizeIndicatorDots.getWidth() - (bmp_dot_w + dot_gap) * mAppsCustomizeTabHost.mAppsCustomizePane.getPageCount() + dot_gap) / 2 + 20;
            int start_x_tmp = start_x;


            for (int i = 0; i < mAppsCustomizeTabHost.mAppsCustomizePane.getPageCount(); i++) {
                canvas.drawBitmap(bmp_dot, start_x_tmp - 20, 0, mPaint);
                start_x_tmp += bmp_dot_w + dot_gap;
            }

            canvas.drawBitmap(bmp_dot_sel, start_x + (bmp_dot_w + dot_gap) * mAppsCustomizeTabHost.mAppsCustomizePane.getNextPage() + mov_x - 20, 0, mPaint);

            canvas.restore();
        }
    }

    /**
     * Creates a view representing a shortcut.
     *
     * @param info The data structure describing the shortcut.
     * @return A View inflated from R.layout.application.
     */
    View createShortcut(ShortcutInfo info) {
        return createShortcut(R.layout.application, (ViewGroup) mWorkspace.getChildAt(mWorkspace.getCurrentPage()), info);
    }

    /**
     * Creates a view representing a shortcut inflated from the specified
     * resource.
     *
     * @param layoutResId The id of the XML layout used to create the shortcut.
     * @param parent      The group the shortcut belongs to.
     * @param info        The data structure describing the shortcut.
     * @return A View inflated from layoutResId.
     */
    View createShortcut(int layoutResId, ViewGroup parent, ShortcutInfo info) {
        BubbleTextView favorite = (BubbleTextView) mInflater.inflate(layoutResId, parent, false);
        favorite.applyFromShortcutInfo(info, mIconCache);

        favorite.setOnClickListener(this);

        return favorite;
    }

    /**
     * Add an application shortcut to the workspace.
     *
     * @param data The intent describing the application.
     *             The position on screen where to create the shortcut.
     */
    void completeAddApplication(Intent data, long container, int screen, int cellX, int cellY) {
        final int[] cellXY = mTmpAddItemCellCoordinates;
        final CellLayout layout = getCellLayout(container, screen);

        // First we check if we already know the exact location where we want to
        // add this item.
        if (cellX >= 0 && cellY >= 0) {
            cellXY[0] = cellX;
            cellXY[1] = cellY;
        } else if (!layout.findCellForSpan(cellXY, 1, 1)) {
            showOutOfSpaceMessage(isHotseatLayout(layout));
            return;
        }

        final ShortcutInfo info = mModel.getShortcutInfo(getPackageManager(), data, this);

        if (info != null) {
            info.setActivity(data.getComponent(), Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            info.container = ItemInfo.NO_ID;
            mWorkspace.addApplicationShortcut(info, layout, container, screen, cellXY[0], cellXY[1], isWorkspaceLocked(), cellX, cellY);
        } else {
            Log.e(TAG, "Couldn't find ActivityInfo for selected application: " + data);
        }
    }

    /**
     * Add a shortcut to the workspace.
     *
     * @param data The intent describing the shortcut.
     *             The position on screen where to create the shortcut.
     */
    private void completeAddShortcut(Intent data, long container, int screen, int cellX, int cellY) {
        int[] cellXY = mTmpAddItemCellCoordinates;
        int[] touchXY = mPendingAddInfo.dropPos;
        CellLayout layout = getCellLayout(container, screen);

        boolean foundCellSpan = false;

        ShortcutInfo info = mModel.infoFromShortcutIntent(this, data, null);
        if (info == null) {
            return;
        }
        final View view = createShortcut(info);

        // First we check if we already know the exact location where we want to
        // add this item.
        if (cellX >= 0 && cellY >= 0) {
            cellXY[0] = cellX;
            cellXY[1] = cellY;
            foundCellSpan = true;

            // If appropriate, either create a folder or add to an existing
            // folder
            if (mWorkspace.createUserFolderIfNecessary(view, container, layout, cellXY, 0, true, null, null)) {
                return;
            }
            DragObject dragObject = new DragObject();
            dragObject.dragInfo = info;
            if (mWorkspace.addToExistingFolderIfNecessary(view, layout, cellXY, 0, dragObject, true)) {
                return;
            }
        } else if (touchXY != null) {
            // when dragging and dropping, just find the closest free spot
            int[] result = layout.findNearestVacantArea(touchXY[0], touchXY[1], 1, 1, cellXY);
            foundCellSpan = (result != null);
        } else {
            foundCellSpan = layout.findCellForSpan(cellXY, 1, 1);
        }

        if (!foundCellSpan) {
            showOutOfSpaceMessage(isHotseatLayout(layout));
            return;
        }

        LauncherModel.addItemToDatabase(this, info, container, screen, cellXY[0], cellXY[1], false);

        if (!mRestoring) {
            mWorkspace.addInScreen(view, container, screen, cellXY[0], cellXY[1], 1, 1, isWorkspaceLocked());
        }
    }

    static int[] getSpanForWidget(Context context, ComponentName component, int minWidth, int minHeight) {
        Rect padding = AppWidgetHostView.getDefaultPaddingForWidget(context, component, null);
        // We want to account for the extra amount of padding that we are adding
        // to the widget
        // to ensure that it gets the full amount of space that it has requested
        int requiredWidth = minWidth + padding.left + padding.right;
        int requiredHeight = minHeight + padding.top + padding.bottom;
        return CellLayout.rectToCell(context.getResources(), requiredWidth, requiredHeight, null);
    }

    static int[] getSpanForWidget(Context context, AppWidgetProviderInfo info) {
        return getSpanForWidget(context, info.provider, info.minWidth, info.minHeight);
    }

    static int[] getMinSpanForWidget(Context context, AppWidgetProviderInfo info) {
        return getSpanForWidget(context, info.provider, info.minResizeWidth, info.minResizeHeight);
    }

    static int[] getSpanForWidget(Context context, PendingAddWidgetInfo info) {
        return getSpanForWidget(context, info.componentName, info.minWidth, info.minHeight);
    }

    static int[] getMinSpanForWidget(Context context, PendingAddWidgetInfo info) {
        return getSpanForWidget(context, info.componentName, info.minResizeWidth, info.minResizeHeight);
    }

    /**
     * Add a widget to the workspace.
     *
     * @param appWidgetId The app widget id
     *                    The position on screen where to create the widget.
     */
    private void completeAddAppWidget(final int appWidgetId, long container, int screen, AppWidgetHostView hostView, AppWidgetProviderInfo appWidgetInfo) {
        if (appWidgetInfo == null) {
            appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        }

        // Calculate the grid spans needed to fit this widget
        CellLayout layout = getCellLayout(container, screen);

        int[] minSpanXY = getMinSpanForWidget(this, appWidgetInfo);
        int[] spanXY = getSpanForWidget(this, appWidgetInfo);

        // Try finding open space on Launcher screen
        // We have saved the position to which the widget was dragged-- this
        // really only matters
        // if we are placing widgets on a "spring-loaded" screen
        int[] cellXY = mTmpAddItemCellCoordinates;
        int[] touchXY = mPendingAddInfo.dropPos;
        int[] finalSpan = new int[2];
        boolean foundCellSpan = false;
        if (mPendingAddInfo.cellX >= 0 && mPendingAddInfo.cellY >= 0) {
            cellXY[0] = mPendingAddInfo.cellX;
            cellXY[1] = mPendingAddInfo.cellY;
            spanXY[0] = mPendingAddInfo.spanX;
            spanXY[1] = mPendingAddInfo.spanY;
            foundCellSpan = true;
        } else if (touchXY != null) {
            // when dragging and dropping, just find the closest free spot
            int[] result = layout.findNearestVacantArea(touchXY[0], touchXY[1], minSpanXY[0], minSpanXY[1], spanXY[0], spanXY[1], cellXY, finalSpan);
            spanXY[0] = finalSpan[0];
            spanXY[1] = finalSpan[1];
            foundCellSpan = (result != null);
        } else {
            foundCellSpan = layout.findCellForSpan(cellXY, minSpanXY[0], minSpanXY[1]);
        }

        if (!foundCellSpan) {
            if (appWidgetId != -1) {
                // Deleting an app widget ID is a void call but writes to disk
                // before returning
                // to the caller...
                new Thread("deleteAppWidgetId") {
                    public void run() {
                        mAppWidgetHost.deleteAppWidgetId(appWidgetId);
                    }
                }.start();
            }
            showOutOfSpaceMessage(isHotseatLayout(layout));
            return;
        }

        // Build Launcher-specific widget info and save to database
        LauncherAppWidgetInfo launcherInfo = new LauncherAppWidgetInfo(appWidgetId, appWidgetInfo.provider);
        launcherInfo.spanX = spanXY[0];
        launcherInfo.spanY = spanXY[1];
        launcherInfo.minSpanX = mPendingAddInfo.minSpanX;
        launcherInfo.minSpanY = mPendingAddInfo.minSpanY;

        LauncherModel.addItemToDatabase(this, launcherInfo, container, screen, cellXY[0], cellXY[1], false);

        if (!mRestoring) {
            if (hostView == null) {
                // Perform actual inflation because we're live
                launcherInfo.hostView = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
                launcherInfo.hostView.setAppWidget(appWidgetId, appWidgetInfo);
            } else {
                // The AppWidgetHostView has already been inflated and
                // instantiated
                launcherInfo.hostView = hostView;
            }

            launcherInfo.hostView.setTag(launcherInfo);
            launcherInfo.hostView.setVisibility(View.VISIBLE);
            launcherInfo.notifyWidgetSizeChanged(this);

            mWorkspace.addInScreen(launcherInfo.hostView, container, screen, cellXY[0], cellXY[1], launcherInfo.spanX, launcherInfo.spanY, isWorkspaceLocked());

            addWidgetToAutoAdvanceIfNeeded(launcherInfo.hostView, appWidgetInfo);
        }
        resetAddInfo();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                mUserPresent = false;
                mDragLayer.clearAllResizeFrames();
                updateRunning();

                // Reset AllApps to its initial state only if we are not in the
                // middle of
                // processing a multi-step drop
                if (mAppsCustomizeTabHost != null && mPendingAddInfo.container == ItemInfo.NO_ID) {
                    mAppsCustomizeTabHost.reset();
                    showWorkspace(false);
                }
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                mUserPresent = true;
                updateRunning();
            }
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Listen for broadcasts related to user-presence
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(mReceiver, filter);

        mAttached = true;
        mVisible = true;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mVisible = false;

        if (mAttached) {
            unregisterReceiver(mReceiver);
            mAttached = false;
        }
        updateRunning();
    }

    public void onWindowVisibilityChanged(int visibility) {
        mVisible = visibility == View.VISIBLE;
        updateRunning();
        // The following code used to be in onResume, but it turns out onResume
        // is called when
        // you're in All Apps and click home to go to the workspace.
        // onWindowVisibilityChanged
        // is a more appropriate event to handle
        if (mVisible) {
            mAppsCustomizeTabHost.onWindowVisible();
            if (!mWorkspaceLoading) {
                final ViewTreeObserver observer = mWorkspace.getViewTreeObserver();
                // We want to let Launcher draw itself at least once before we
                // force it to build
                // layers on all the workspace pages, so that transitioning to
                // Launcher from other
                // apps is nice and speedy. Usually the first call to preDraw
                // doesn't correspond to
                // a true draw so we wait until the second preDraw call to be
                // safe
                observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    public boolean onPreDraw() {
                        // We delay the layer building a bit in order to give
                        // other message processing a time to run. In particular
                        // this avoids a delay in hiding the IME if it was
                        // currently shown, because doing that may involve
                        // some communication back with the app.
                        mWorkspace.postDelayed(mBuildLayersRunnable, 500);

                        observer.removeOnPreDrawListener(this);
                        return true;
                    }
                });
            }
            // When Launcher comes back to foreground, a different Activity
            // might be responsible for
            // the app market intent, so refresh the icon
            updateAppMarketIcon();
            clearTypedText();
        }
    }

    private void sendAdvanceMessage(long delay) {
        mHandler.removeMessages(ADVANCE_MSG);
        Message msg = mHandler.obtainMessage(ADVANCE_MSG);
        mHandler.sendMessageDelayed(msg, delay);
        mAutoAdvanceSentTime = System.currentTimeMillis();
    }

    private void updateRunning() {
        boolean autoAdvanceRunning = mVisible && mUserPresent && !mWidgetsToAdvance.isEmpty();
        if (autoAdvanceRunning != mAutoAdvanceRunning) {
            mAutoAdvanceRunning = autoAdvanceRunning;
            if (autoAdvanceRunning) {
                long delay = mAutoAdvanceTimeLeft == -1 ? mAdvanceInterval : mAutoAdvanceTimeLeft;
                sendAdvanceMessage(delay);
            } else {
                if (!mWidgetsToAdvance.isEmpty()) {
                    mAutoAdvanceTimeLeft = Math.max(0, mAdvanceInterval - (System.currentTimeMillis() - mAutoAdvanceSentTime));
                }
                mHandler.removeMessages(ADVANCE_MSG);
                mHandler.removeMessages(0); // Remove messages sent using
                // postDelayed()
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ADVANCE_MSG) {
                int i = 0;
                for (View key : mWidgetsToAdvance.keySet()) {
                    final View v = key.findViewById(mWidgetsToAdvance.get(key).autoAdvanceViewId);
                    final int delay = mAdvanceStagger * i;
                    if (v instanceof Advanceable) {
                        postDelayed(new Runnable() {
                            public void run() {
                                ((Advanceable) v).advance();
                            }
                        }, delay);
                    }
                    i++;
                }
                sendAdvanceMessage(mAdvanceInterval);
            }
        }
    };

    void addWidgetToAutoAdvanceIfNeeded(View hostView, AppWidgetProviderInfo appWidgetInfo) {
        if (appWidgetInfo == null || appWidgetInfo.autoAdvanceViewId == -1) return;
        View v = hostView.findViewById(appWidgetInfo.autoAdvanceViewId);
        if (v instanceof Advanceable) {
            mWidgetsToAdvance.put(hostView, appWidgetInfo);
            ((Advanceable) v).fyiWillBeAdvancedByHostKThx();
            updateRunning();
        }
    }

    void removeWidgetToAutoAdvance(View hostView) {
        if (mWidgetsToAdvance.containsKey(hostView)) {
            mWidgetsToAdvance.remove(hostView);
            updateRunning();
        }
    }

    public void removeAppWidget(LauncherAppWidgetInfo launcherInfo) {
        removeWidgetToAutoAdvance(launcherInfo.hostView);
        launcherInfo.hostView = null;
    }

    void showOutOfSpaceMessage(boolean isHotseatLayout) {
        int strId = (isHotseatLayout ? R.string.hotseat_out_of_space : R.string.out_of_space);
        Toast.makeText(this, getString(strId), Toast.LENGTH_SHORT).show();
    }

    public LauncherAppWidgetHost getAppWidgetHost() {
        return mAppWidgetHost;
    }

    public LauncherModel getModel() {
        return mModel;
    }

    void closeSystemDialogs() {
        getWindow().closeAllPanels();

        // Whatever we were doing is hereby canceled.
        mWaitingForResult = false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Close the menu
        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            // also will cancel mWaitingForResult.
            closeSystemDialogs();

            final boolean alreadyOnHome = ((intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);

            Runnable processIntent = new Runnable() {
                public void run() {
                    Folder openFolder = mWorkspace.getOpenFolder();
                    // In all these cases, only animate if we're already on home
                    mWorkspace.exitWidgetResizeMode();
                    if (alreadyOnHome && mState == State.WORKSPACE && !mWorkspace.isTouchActive() && openFolder == null) {
                        mWorkspace.moveToDefaultScreen(true);
                    }

                    closeFolder();
                    exitSpringLoadedDragMode();

                    // If we are already on home, then just animate back to the
                    // workspace,
                    // otherwise, just wait until onResume to set the state back
                    // to Workspace
                    if (alreadyOnHome) {
                        showWorkspace(true);
                    } else {
                        mOnResumeState = State.WORKSPACE;
                    }

                    final View v = getWindow().peekDecorView();
                    if (v != null && v.getWindowToken() != null) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }

                    // Reset AllApps to its initial state
                    if (!alreadyOnHome && mAppsCustomizeTabHost != null) {
                        mAppsCustomizeTabHost.reset();
                    }
                }
            };

            if (alreadyOnHome && !mWorkspace.hasWindowFocus()) {
                // Delay processing of the intent to allow the status bar
                // animation to finish
                // first in order to avoid janky animations.
                mWorkspace.postDelayed(processIntent, 0);
            } else {
                // Process the intent immediately.
                processIntent.run();
            }

        }
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        for (int page : mSynchronouslyBoundPages) {
            mWorkspace.restoreInstanceStateForChild(page);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(RUNTIME_STATE_CURRENT_SCREEN, mWorkspace.getNextPage());
        super.onSaveInstanceState(outState);

        outState.putInt(RUNTIME_STATE, mState.ordinal());
        // We close any open folder since it will not be re-opened, and we need
        // to make sure
        // this state is reflected.
        closeFolder();

        if (mPendingAddInfo.container != ItemInfo.NO_ID && mPendingAddInfo.screen > -1 && mWaitingForResult) {
            outState.putLong(RUNTIME_STATE_PENDING_ADD_CONTAINER, mPendingAddInfo.container);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SCREEN, mPendingAddInfo.screen);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_X, mPendingAddInfo.cellX);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_Y, mPendingAddInfo.cellY);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_X, mPendingAddInfo.spanX);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y, mPendingAddInfo.spanY);
            outState.putParcelable(RUNTIME_STATE_PENDING_ADD_WIDGET_INFO, mPendingAddWidgetInfo);
        }

        if (mFolderInfo != null && mWaitingForResult) {
            outState.putBoolean(RUNTIME_STATE_PENDING_FOLDER_RENAME, true);
            outState.putLong(RUNTIME_STATE_PENDING_FOLDER_RENAME_ID, mFolderInfo.id);
        }

        // Save the current AppsCustomize tab
        if (mAppsCustomizeTabHost != null) {
            String currentTabTag = mAppsCustomizeTabHost.getCurrentTabTag();
            if (currentTabTag != null) {
                outState.putString("apps_customize_currentTab", currentTabTag);
            }
            int currentIndex = mAppsCustomizeContent.getSaveInstanceStateIndex();
            outState.putInt("apps_customize_currentIndex", currentIndex);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unRegisterBroadcastReceiver();
        // Remove all pending runnables
        mHandler.removeMessages(ADVANCE_MSG);
        mHandler.removeMessages(0);
        mWorkspace.removeCallbacks(mBuildLayersRunnable);
        LauncherApplication app = ((LauncherApplication) getApplication());
        mModel.stopLoader();
        app.setLauncher(null);

        try {
            mAppWidgetHost.stopListening();
        } catch (NullPointerException ex) {
            Log.w(TAG, "problem while stopping AppWidgetHost during Launcher destruction", ex);
        }
        mAppWidgetHost = null;

        mWidgetsToAdvance.clear();

        TextKeyListener.getInstance().release();

        // Disconnect any of the callbacks and drawables associated with
        // ItemInfos on the workspace
        // to prevent leaking Launcher activities on orientation change.
        if (mModel != null) {
            mModel.unbindItemInfosAndClearQueuedBindRunnables();
        }

        getContentResolver().unregisterContentObserver(mWidgetObserver);
        unregisterReceiver(mCloseSystemDialogsReceiver);

        mDragLayer.clearAllResizeFrames();
        ((ViewGroup) mWorkspace.getParent()).removeAllViews();
        mWorkspace.removeAllViews();
        mWorkspace = null;
        mDragController = null;

      /*  kwapi.unbindAutoSdkService(this);
        kwapi.unRegisterPlayerStatusListener();*/

        mMediaListener.cleanup();
        LauncherAnimUtils.onDestroyActivity();
        timer.cancel();
    }

    public DragController getDragController() {
        return mDragController;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        if (requestCode >= 0) mWaitingForResult = true;
        super.startActivityForResult(intent, requestCode);
    }

    /**
     * Indicates that we want global search for this activity by setting the
     * globalSearch argument for {@link #startSearch} to true.
     */
    @Override
    public void startSearch(String initialQuery, boolean selectInitialQuery, Bundle appSearchData, boolean globalSearch) {

        showWorkspace(true);

        if (initialQuery == null) {
            // Use any text typed in the launcher as the initial query
            initialQuery = getTypedText();
        }
        if (appSearchData == null) {
            appSearchData = new Bundle();
            // appSearchData.putString(Search.SOURCE, "launcher-search");
        }
        Rect sourceBounds = new Rect();
        if (mSearchDropTargetBar != null) {
            sourceBounds = mSearchDropTargetBar.getSearchBarBounds();
        }

        startGlobalSearch(initialQuery, selectInitialQuery, appSearchData, sourceBounds);
    }

    /**
     * Starts the global search activity. This code is a copied from
     * SearchManager
     */
    public void startGlobalSearch(String initialQuery, boolean selectInitialQuery, Bundle appSearchData, Rect sourceBounds) {
        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        ComponentName globalSearchActivity = searchManager.getGlobalSearchActivity();
        if (globalSearchActivity == null) {
            Log.w(TAG, "No global search activity found.");
            return;
        }
        Intent intent = new Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setComponent(globalSearchActivity);
        // Make sure that we have a Bundle to put source in
        if (appSearchData == null) {
            appSearchData = new Bundle();
        } else {
            appSearchData = new Bundle(appSearchData);
        }
        // Set source to package name of app that starts global search, if not
        // set already.
        if (!appSearchData.containsKey("source")) {
            appSearchData.putString("source", getPackageName());
        }
        intent.putExtra(SearchManager.APP_DATA, appSearchData);
        if (!TextUtils.isEmpty(initialQuery)) {
            intent.putExtra(SearchManager.QUERY, initialQuery);
        }
        if (selectInitialQuery) {
            intent.putExtra(SearchManager.EXTRA_SELECT_QUERY, selectInitialQuery);
        }
        intent.setSourceBounds(sourceBounds);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Global search activity not found: " + globalSearchActivity);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isWorkspaceLocked()) {
            return false;
        }

        super.onCreateOptionsMenu(menu);

        Intent manageApps = new Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS);
        manageApps.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        Intent settings = new Intent(android.provider.Settings.ACTION_SETTINGS);
        settings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        String helpUrl = getString(R.string.help_url);
        Intent help = new Intent(Intent.ACTION_VIEW, Uri.parse(helpUrl));
        help.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        menu.add(MENU_GROUP_WALLPAPER, MENU_WALLPAPER_SETTINGS, 0, R.string.menu_wallpaper).setIcon(android.R.drawable.ic_menu_gallery).setAlphabeticShortcut('W');
        menu.add(0, MENU_MANAGE_APPS, 0, R.string.menu_manage_apps).setIcon(android.R.drawable.ic_menu_manage).setIntent(manageApps).setAlphabeticShortcut('M');
        menu.add(0, MENU_SYSTEM_SETTINGS, 0, R.string.menu_settings).setIcon(android.R.drawable.ic_menu_preferences).setIntent(settings).setAlphabeticShortcut('P');
        if (!helpUrl.isEmpty()) {
            menu.add(0, MENU_HELP, 0, R.string.menu_help).setIcon(android.R.drawable.ic_menu_help).setIntent(help).setAlphabeticShortcut('H');
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (mAppsCustomizeTabHost.isTransitioning()) {
            return false;
        }
        boolean allAppsVisible = (mAppsCustomizeTabHost.getVisibility() == View.VISIBLE);
        menu.setGroupVisible(MENU_GROUP_WALLPAPER, !allAppsVisible);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_WALLPAPER_SETTINGS:
                startWallpaper();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSearchRequested() {
        startSearch(null, false, null, true);
        // Use a custom animation for launching search
        return true;
    }

    public boolean isWorkspaceLocked() {
        return mWorkspaceLoading || mWaitingForResult;
    }

    private void resetAddInfo() {
        mPendingAddInfo.container = ItemInfo.NO_ID;
        mPendingAddInfo.screen = -1;
        mPendingAddInfo.cellX = mPendingAddInfo.cellY = -1;
        mPendingAddInfo.spanX = mPendingAddInfo.spanY = -1;
        mPendingAddInfo.minSpanX = mPendingAddInfo.minSpanY = -1;
        mPendingAddInfo.dropPos = null;
    }

    void addAppWidgetImpl(final int appWidgetId, ItemInfo info, AppWidgetHostView boundWidget, AppWidgetProviderInfo appWidgetInfo) {
        if (appWidgetInfo.configure != null) {
            mPendingAddWidgetInfo = appWidgetInfo;

            // Launch over to configure widget, if needed
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            startActivityForResultSafely(intent, REQUEST_CREATE_APPWIDGET);
        } else {
            // Otherwise just add it
            completeAddAppWidget(appWidgetId, info.container, info.screen, boundWidget, appWidgetInfo);
            // Exit spring loaded mode if necessary after adding the widget
            exitSpringLoadedDragModeDelayed(true, false, null);
        }
    }

    /**
     * Process a shortcut drop.
     *
     * @param componentName The name of the component
     * @param screen        The screen where it should be added
     * @param cell          The cell it should be added to, optional
     *                      The location on the screen where it was dropped, optional
     */
    void processShortcutFromDrop(ComponentName componentName, long container, int screen, int[] cell, int[] loc) {
        resetAddInfo();
        mPendingAddInfo.container = container;
        mPendingAddInfo.screen = screen;
        mPendingAddInfo.dropPos = loc;

        if (cell != null) {
            mPendingAddInfo.cellX = cell[0];
            mPendingAddInfo.cellY = cell[1];
        }

        Intent createShortcutIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        createShortcutIntent.setComponent(componentName);
        processShortcut(createShortcutIntent);
    }

    /**
     * Process a widget drop.
     *
     * @param info   The PendingAppWidgetInfo of the widget being added.
     * @param screen The screen where it should be added
     * @param cell   The cell it should be added to, optional
     *               The location on the screen where it was dropped, optional
     */
    void addAppWidgetFromDrop(PendingAddWidgetInfo info, long container, int screen, int[] cell, int[] span, int[] loc) {
        resetAddInfo();
        mPendingAddInfo.container = info.container = container;
        mPendingAddInfo.screen = info.screen = screen;
        mPendingAddInfo.dropPos = loc;
        mPendingAddInfo.minSpanX = info.minSpanX;
        mPendingAddInfo.minSpanY = info.minSpanY;

        if (cell != null) {
            mPendingAddInfo.cellX = cell[0];
            mPendingAddInfo.cellY = cell[1];
        }
        if (span != null) {
            mPendingAddInfo.spanX = span[0];
            mPendingAddInfo.spanY = span[1];
        }

        AppWidgetHostView hostView = info.boundWidget;
        int appWidgetId;
        if (hostView != null) {
            appWidgetId = hostView.getAppWidgetId();
            addAppWidgetImpl(appWidgetId, info, hostView, info.info);
        } else {
            // In this case, we either need to start an activity to get
            // permission to bind
            // the widget, or we need to start an activity to configure the
            // widget, or both.
            appWidgetId = getAppWidgetHost().allocateAppWidgetId();
            Bundle options = info.bindOptions;

            boolean success = false;
            if (options != null) {
                // success =
                // mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId,
                // info.componentName, options);
            } else {
                success = mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.componentName);
            }
            if (success) {
                addAppWidgetImpl(appWidgetId, info, null, info.info);
            } else {
                mPendingAddWidgetInfo = info.info;
                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.componentName);
                // TODO: we need to make sure that this accounts for the options
                // bundle.
                // intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS,
                // options);
                startActivityForResult(intent, REQUEST_BIND_APPWIDGET);
            }
        }
    }

    void processShortcut(Intent intent) {
        // Handle case where user selected "Applications"
        String applicationName = getResources().getString(R.string.group_applications);
        String shortcutName = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);

        if (applicationName != null && applicationName.equals(shortcutName)) {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
            pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
            pickIntent.putExtra(Intent.EXTRA_TITLE, getText(R.string.title_select_application));
            startActivityForResultSafely(pickIntent, REQUEST_PICK_APPLICATION);
        } else {
            startActivityForResultSafely(intent, REQUEST_CREATE_SHORTCUT);
        }
    }

    void processWallpaper(Intent intent) {
        startActivityForResult(intent, REQUEST_PICK_WALLPAPER);
    }

    FolderIcon addFolder(CellLayout layout, long container, final int screen, int cellX, int cellY) {
        final FolderInfo folderInfo = new FolderInfo();
        folderInfo.title = getText(R.string.folder_name);

        // Update the model
        LauncherModel.addItemToDatabase(Launcher.this, folderInfo, container, screen, cellX, cellY, false);
        sFolders.put(folderInfo.id, folderInfo);

        // Create the view
        FolderIcon newFolder = FolderIcon.fromXml(R.layout.folder_icon, this, layout, folderInfo, mIconCache);
        mWorkspace.addInScreen(newFolder, container, screen, cellX, cellY, 1, 1, isWorkspaceLocked());
        return newFolder;
    }

    void removeFolder(FolderInfo folder) {
        sFolders.remove(folder.id);
    }

    private void startWallpaper() {
//        showWorkspace(true);
        Intent pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
        List<ResolveInfo> lists = getPackageManager().queryIntentActivities(pickWallpaper, 0);
        List<Intent> listIntent = new ArrayList<>();
        pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
        pickWallpaper.setPackage(getPackageName());
        listIntent.add(pickWallpaper);
        for (ResolveInfo info : lists) {
            String pkgName = info.activityInfo.packageName;
            Log.d(TAG, pkgName);
            if (!pkgName.contains("launcher")) {
                pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
                pickWallpaper.setPackage(pkgName);
                listIntent.add(pickWallpaper);
            }
        }
        Intent chooser = Intent.createChooser(new Intent(), getText(R.string.chooser_wallpaper));
        // NOTE: Adds a configure option to the chooser if the wallpaper supports Removed in Eclair MR1
        // WallpaperManager wm = (WallpaperManager) getSystemService(Context.WALLPAPER_SERVICE);
        // WallpaperInfo wi = wm.getWallpaperInfo();
        // if (wi != null && wi.getSettingsActivity() != null) {
        // LabeledIntent li = new LabeledIntent(getPackageName(),
        // R.string.configure_wallpaper, 0);
        // li.setClassName(wi.getPackageName(), wi.getSettingsActivity());
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, listIntent.toArray(new Parcelable[]{}));
        // }
        startActivityForResult(chooser, REQUEST_PICK_WALLPAPER);

    }

    /**
     * Registers various content observers. The current implementation registers
     * only a favorites observer to keep track of the favorites applications.
     */
    private void registerContentObservers() {
        ContentResolver resolver = getContentResolver();
        resolver.registerContentObserver(LauncherProvider.CONTENT_APPWIDGET_RESET_URI, true, mWidgetObserver);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_HOME:
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    if (doesFileExist(DUMP_STATE_PROPERTY)) {
                        dumpState();
                        return true;
                    }
                    break;
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_HOME:
                    return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (isAllAppsVisible()) {
            showWorkspace(true);
        } else if (mWorkspace.getOpenFolder() != null) {
            Folder openFolder = mWorkspace.getOpenFolder();
            if (openFolder.isEditingName()) {
                openFolder.dismissEditingName();
            } else {
                closeFolder();
            }
        } else {
            mWorkspace.exitWidgetResizeMode();

            // Back button is a no-op here, but give at least some feedback for
            // the button press
            mWorkspace.showOutlinesTemporarily();
        }
    }

    /**
     * Re-listen when widgets are reset.
     */
    private void onAppWidgetReset() {
        if (mAppWidgetHost != null) {
            mAppWidgetHost.startListening();
        }
    }

    /**
     * 生成指定长度的随机频谱数据（字符串数组）
     *
     * @param length             数据长度（≥16）
     * @param minValue           最小值（含）
     * @param maxValue           最大值（含）
     * @param nonZeroProbability 非零值的概率（0~1）
     * @return 随机数据数组
     */
    public static String[] generateRandomSpectrumData(int length, double minValue, double maxValue, double nonZeroProbability) {
        Random random = new Random();
        String[] data = new String[length];
        double range = maxValue - minValue;

        for (int i = 0; i < length; i++) {
            // 生成基础随机值
            double value = minValue + random.nextDouble() * range;

            // 按概率决定是否强制非零（避免全零）
            if (random.nextDouble() < nonZeroProbability) {
                value = Math.max(value, minValue + 1e-9); // 确保非零
            }

            data[i] = String.valueOf(value);
        }
        return data;
    }

    /**
     * Launches the intent referred by the clicked shortcut.
     *
     * @param v The view representing the clicked shortcut.
     */
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.hotset_setting:
                startActivity("com.awell.carsetting", "com.awell.carsetting.MainActivity");
                break;
            case R.id.hotset_bluetooth:
                startActivity("com.awell.bluetooth", "com.awell.bluetooth.MainActivity");
                break;
            case R.id.hotset_navi:
            case R.id.rl_car:
            case R.id.iv_main_xiaodeng:
                startActivity("com.awell.navigation", "com.awell.navigation.MainActivity");
                break;
            case R.id.hotset_allapp: // allapp
                canbus_set_dis = getCanBusState();
                showAllApps(true);
                setSettingOrAndroidPage(true);
                mModel.startLoader(true, -1);
                break;
            case R.id.hotset_dianhua:
                startActivity("com.android.dialer", "com.android.dialer.app.DialtactsActivity");
                break;
            case R.id.hotset_video:
                startActivity("com.awell.localvideo", "com.awell.localvideo.activity.VideoListActivity");
                break;
            case R.id.hotset_moshi:
                int value = Settings.System.getInt(getContentResolver(), "launcherTypeDN", 0);
                value = value == 1 ? 0 : 1;
                Settings.System.putInt(getContentResolver(), "launcherTypeDN", value);
                dayNightImages(value);
                handler.removeMessages(SWTWALLPAPER);
                handler.obtainMessage(SWTWALLPAPER, value, 0).sendToTarget();
                break;
            case R.id.tv_radio_freq:
            case R.id.waveformView:
                startActivity("com.awell.radio", "com.awell.radio.AwellFmActivity");
                break;
            case R.id.iv_radio_pre:
                //String[] exampleData = generateRandomSpectrumData(16, 0.0, 1.0, 0.95); // 95%概率非零
                mediaLibrary.setDataEvent(AwellTool.RADIO.PREVIOUS);
                break;
            case R.id.iv_radio_next:
                mediaLibrary.setDataEvent(AwellTool.RADIO.NEXT);
                break;
            case R.id.tv_radio_am_fm:
            case R.id.iv_radio_setFM:
                mediaLibrary.setDataEvent(AwellTool.RADIO.SET_FMAM);
                break;
            case R.id.iv_radio_setSearch:
                mediaLibrary.setDataEvent(AwellTool.RADIO.AUTO_SCAN);
                break;
            case R.id.iv_radio_setYC:
                mediaLibrary.setDataEvent(AwellTool.RADIO.SET_LocDX);
                break;
        }

        for (int i = 0; i < time_img_id.length; i++) {
            if (v.getId() == time_img_id[i]) {
                setTimeWidgetVisible(i);
                setTimeWidgetIndex(i);
                break;
            }
        }
        // Make sure that rogue clicks don't get through while allapps is
        // launching, or after the
        // view has detached (it's possible for this to happen if the view is
        // removed mid touch).
        if (v.getWindowToken() == null) {
            return;
        }

        if (!mWorkspace.isFinishedSwitchingState()) {
            return;
        }

        Object tag = v.getTag();
        if (tag instanceof ShortcutInfo) {
            // Open shortcut
            final Intent intent = ((ShortcutInfo) tag).intent;
            int[] pos = new int[2];
            v.getLocationOnScreen(pos);
            intent.setSourceBounds(new Rect(pos[0], pos[1], pos[0] + v.getWidth(), pos[1] + v.getHeight()));

            boolean success = startActivitySafely(v, intent, tag);

            if (success && v instanceof BubbleTextView) {
                mWaitingForResume = (BubbleTextView) v;
                mWaitingForResume.setStayPressed(true);
            }
        } else if (tag instanceof FolderInfo) {
            if (v instanceof FolderIcon) {
                FolderIcon fi = (FolderIcon) v;
                handleFolderClick(fi);
            }
        } else if (v == mAllAppsButton) {
            if (isAllAppsVisible()) {
                showWorkspace(true);
            } else {
                // onClickAllAppsButton(v);
            }
        }
    }

    /**
     * 鎵撳紑鏂伴椈鐨刟Activity
     */
    private void startWeatherActivity(String packName, String className) {
        Intent intent = new Intent();
        intent.setClassName(packName, className);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public boolean onTouch(View v, MotionEvent event) {
        // this is an intercepted event being forwarded from mWorkspace;
        // clicking anywhere on the workspace causes the customization drawer to
        // slide down
        showWorkspace(true);
        return false;
    }

    /**
     * Event handler for the search button
     *
     * @param v The view that was clicked.
     */
    public void onClickSearchButton(View v) {
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        onSearchRequested();
    }

    /**
     * Event handler for the voice button
     *
     * @param v The view that was clicked.
     */
    public void onClickVoiceButton(View v) {
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        try {
            final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            ComponentName activityName = searchManager.getGlobalSearchActivity();
            Intent intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (activityName != null) {
                intent.setPackage(activityName.getPackageName());
            }
            startActivity(null, intent, "onClickVoiceButton");
        } catch (ActivityNotFoundException e) {
            Intent intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivitySafely(null, intent, "onClickVoiceButton");
        }
    }

    /**
     * Event handler for the "grid" button that appears on the home screen,
     * which enters all apps mode.
     *
     * @param v The view that was clicked.
     */
    public void onClickAllAppsButton(View v) {
        // showAllApps(true);
        // mWorkspace.setVisibility(View.GONE);
        // isWorkSpaceOrAPPSCUSTOMIZE=false;
        // 娌℃湁杩欏彞璇濇樉绀轰笉鍑烘暟鎹�
        canbus_set_dis = getCanBusState();
        showAllApps(true);
        // 璁剧疆涓簍rue,鏄剧ず搴旂敤鐣岄潰;璁剧疆涓篺alse鏃舵樉绀轰负璁剧疆鐣岄潰
        setSettingOrAndroidPage(true);
        // 鏄剧ず涓嬫柟缈婚〉鐨勫皬鍦嗙偣?
        updateGotoPageButtonStatus();
        mModel.startLoader(true, -1);
    }

    public void onClickSettingsButton(View v) {
        // showAllApps(true);
        // mWorkspace.setVisibility(View.GONE);
        // isWorkSpaceOrAPPSCUSTOMIZE=false;
        // 娌℃湁杩欏彞璇濇樉绀轰笉鍑烘暟鎹�
        canbus_set_dis = getCanBusState();
        showAllApps(true);
        // 璁剧疆涓簍rue,鏄剧ず搴旂敤鐣岄潰;璁剧疆涓篺alse鏃舵樉绀轰负璁剧疆鐣岄潰
        setSettingOrAndroidPage(false);
        // 鏄剧ず涓嬫柟缈婚〉鐨勫皬鍦嗙偣?
        // updateGotoPageButtonStatus();
        mModel.startLoader(true, -1);
    }

    public void onClickAutoNaviButton(View v) {
        startActivity("com.autonavi.xmgd.navigator", "com.autonavi.xmgd.navigator.Warn");
    }

    public void onTouchDownAllAppsButton(View v) {
        // Provide the same haptic feedback that the system offers for virtual
        // keys.
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    public void onClickAppMarketButton(View v) {
        if (mAppMarketIntent != null) {
            startActivitySafely(v, mAppMarketIntent, "app market");
        } else {
            Log.e(TAG, "Invalid app market intent.");
        }
    }

    void startApplicationDetailsActivity(ComponentName componentName) {
        String packageName = componentName.getPackageName();
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivitySafely(null, intent, "startApplicationDetailsActivity");
    }

    void startApplicationUninstallActivity(ApplicationInfo appInfo) {
        if ((appInfo.flags & ApplicationInfo.DOWNLOADED_FLAG) == 0) {
            // System applications cannot be installed. For now, show a toast
            // explaining that.
            // We may give them the option of disabling apps this way.
            int messageId = R.string.uninstall_system_app_text;
            Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
        } else {
            String packageName = appInfo.componentName.getPackageName();
            String className = appInfo.componentName.getClassName();
            Intent intent = new Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, className));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(intent);
        }
    }

    boolean startActivity(View v, Intent intent, Object tag) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            // Only launch using the new animation if the shortcut has not opted
            // out (this is a
            // private contract between launcher and may be ignored in the
            // future).
            boolean useLaunchAnimation = (v != null) && !intent.hasExtra(INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION);
            if (useLaunchAnimation) {
                ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());

                startActivity(intent, opts.toBundle());
            } else {
                startActivity(intent);
            }
            return true;
        } catch (SecurityException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Launcher does not have the permission to launch " + intent + ". Make sure to create a MAIN intent-filter for the corresponding activity " + "or use the exported attribute for this activity. " + "tag=" + tag + " intent=" + intent, e);
        }
        return false;
    }

    boolean startActivitySafely(View v, Intent intent, Object tag) {
        boolean success = false;
        try {
            success = startActivity(v, intent, tag);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Unable to launch. tag=" + tag + " intent=" + intent, e);
        }
        return success;
    }

    void startActivityForResultSafely(Intent intent, int requestCode) {
        try {
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Launcher does not have the permission to launch " + intent + ". Make sure to create a MAIN intent-filter for the corresponding activity " + "or use the exported attribute for this activity.", e);
        }
    }

    private void handleFolderClick(FolderIcon folderIcon) {
        final FolderInfo info = folderIcon.getFolderInfo();
        Folder openFolder = mWorkspace.getFolderForTag(info);

        // If the folder info reports that the associated folder is open, then
        // verify that
        // it is actually opened. There have been a few instances where this
        // gets out of sync.
        if (info.opened && openFolder == null) {
            Log.d(TAG, "Folder info marked as open, but associated folder is not open. Screen: " + info.screen + " (" + info.cellX + ", " + info.cellY + ")");
            info.opened = false;
        }

        if (!info.opened && !folderIcon.getFolder().isDestroyed()) {
            // Close any open folder
            closeFolder();
            // Open the requested folder
            openFolder(folderIcon);
        } else {
            // Find the open folder...
            int folderScreen;
            if (openFolder != null) {
                folderScreen = mWorkspace.getPageForView(openFolder);
                // .. and close it
                closeFolder(openFolder);
                if (folderScreen != mWorkspace.getCurrentPage()) {
                    // Close any folder open on the current screen
                    closeFolder();
                    // Pull the folder onto this screen
                    openFolder(folderIcon);
                }
            }
        }
    }

    /**
     * This method draws the FolderIcon to an ImageView and then adds and
     * positions that ImageView in the DragLayer in the exact absolute location
     * of the original FolderIcon.
     */
    private void copyFolderIconToImage(FolderIcon fi) {
        final int width = fi.getMeasuredWidth();
        final int height = fi.getMeasuredHeight();

        // Lazy load ImageView, Bitmap and Canvas
        if (mFolderIconImageView == null) {
            mFolderIconImageView = new ImageView(this);
        }
        if (mFolderIconBitmap == null || mFolderIconBitmap.getWidth() != width || mFolderIconBitmap.getHeight() != height) {
            mFolderIconBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mFolderIconCanvas = new Canvas(mFolderIconBitmap);
        }

        DragLayer.LayoutParams lp;
        if (mFolderIconImageView.getLayoutParams() instanceof DragLayer.LayoutParams) {
            lp = (DragLayer.LayoutParams) mFolderIconImageView.getLayoutParams();
        } else {
            lp = new DragLayer.LayoutParams(width, height);
        }

        // The layout from which the folder is being opened may be scaled,
        // adjust the starting
        // view size by this scale factor.
        float scale = mDragLayer.getDescendantRectRelativeToSelf(fi, mRectForFolderAnimation);
        lp.customPosition = true;
        lp.x = mRectForFolderAnimation.left;
        lp.y = mRectForFolderAnimation.top;
        lp.width = (int) (scale * width);
        lp.height = (int) (scale * height);

        mFolderIconCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        fi.draw(mFolderIconCanvas);
        mFolderIconImageView.setImageBitmap(mFolderIconBitmap);
        if (fi.getFolder() != null) {
            mFolderIconImageView.setPivotX(fi.getFolder().getPivotXForIconAnimation());
            mFolderIconImageView.setPivotY(fi.getFolder().getPivotYForIconAnimation());
        }
        // Just in case this image view is still in the drag layer from a
        // previous animation,
        // we remove it and re-add it.
        if (mDragLayer.indexOfChild(mFolderIconImageView) != -1) {
            mDragLayer.removeView(mFolderIconImageView);
        }
        mDragLayer.addView(mFolderIconImageView, lp);
        if (fi.getFolder() != null) {
            fi.getFolder().bringToFront();
        }
    }

    private void growAndFadeOutFolderIcon(FolderIcon fi) {
        if (fi == null) return;
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0);
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1.5f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1.5f);

        FolderInfo info = (FolderInfo) fi.getTag();
        if (info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            CellLayout cl = (CellLayout) fi.getParent().getParent();
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) fi.getLayoutParams();
            cl.setFolderLeaveBehindCell(lp.cellX, lp.cellY);
        }

        // Push an ImageView copy of the FolderIcon into the DragLayer and hide
        // the original
        copyFolderIconToImage(fi);
        fi.setVisibility(View.INVISIBLE);

        ObjectAnimator oa = LauncherAnimUtils.ofPropertyValuesHolder(mFolderIconImageView, alpha, scaleX, scaleY);
        oa.setDuration(getResources().getInteger(R.integer.config_folderAnimDuration));
        oa.start();
    }

    private void shrinkAndFadeInFolderIcon(final FolderIcon fi) {
        if (fi == null) return;
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 1.0f);
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1.0f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1.0f);

        final CellLayout cl = (CellLayout) fi.getParent().getParent();

        // We remove and re-draw the FolderIcon in-case it has changed
        mDragLayer.removeView(mFolderIconImageView);
        copyFolderIconToImage(fi);
        ObjectAnimator oa = LauncherAnimUtils.ofPropertyValuesHolder(mFolderIconImageView, alpha, scaleX, scaleY);
        oa.setDuration(getResources().getInteger(R.integer.config_folderAnimDuration));
        oa.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (cl != null) {
                    cl.clearFolderLeaveBehind();
                    // Remove the ImageView copy of the FolderIcon and make the
                    // original visible.
                    mDragLayer.removeView(mFolderIconImageView);
                    fi.setVisibility(View.VISIBLE);
                }
            }
        });
        oa.start();
    }

    /**
     * Opens the user folder described by the specified tag. The opening of the
     * folder is animated relative to the specified View. If the View is null,
     * no animation is played.
     *
     * @param folderIcon The FolderInfo describing the folder to open.
     */
    public void openFolder(FolderIcon folderIcon) {
        Folder folder = folderIcon.getFolder();
        FolderInfo info = folder.mInfo;

        info.opened = true;

        // Just verify that the folder hasn't already been added to the
        // DragLayer.
        // There was a one-off crash where the folder had a parent already.
        if (folder.getParent() == null) {
            mDragLayer.addView(folder);
            mDragController.addDropTarget((DropTarget) folder);
        } else {
            Log.w(TAG, "Opening folder (" + folder + ") which already has a parent (" + folder.getParent() + ").");
        }
        folder.animateOpen();
        growAndFadeOutFolderIcon(folderIcon);
    }

    public void closeFolder() {
        Folder folder = mWorkspace.getOpenFolder();
        if (folder != null) {
            if (folder.isEditingName()) {
                folder.dismissEditingName();
            }
            closeFolder(folder);

            // Dismiss the folder cling
            dismissFolderCling(null);
        }
    }

    void closeFolder(Folder folder) {
        folder.getInfo().opened = false;

        ViewGroup parent = (ViewGroup) folder.getParent().getParent();
        if (parent != null) {
            FolderIcon fi = (FolderIcon) mWorkspace.getViewForTag(folder.mInfo);
            shrinkAndFadeInFolderIcon(fi);
        }
        folder.animateClosed();
    }

    public boolean onLongClick(View v) {
        if (v.getId() == R.id.time_widget_change) {
            //showPopupTimeWidget();
        } else if (v.getId() == R.id.all_apps_cling) {

        } else {


            if (!isDraggingEnabled()) return false;
            if (isWorkspaceLocked()) return false;
            if (mState != State.WORKSPACE) return false;

            if (!(v instanceof CellLayout)) {
                v = (View) v.getParent().getParent();
            }

            resetAddInfo();
            CellLayout.CellInfo longClickCellInfo = (CellLayout.CellInfo) v.getTag();
            // This happens when long clicking an item with the dpad/trackball
            if (longClickCellInfo == null) {
                return true;
            }

            // The hotseat touch handling does not go through Workspace, and we
            // always allow long press
            // on hotseat items.
            final View itemUnderLongClick = longClickCellInfo.cell;
            boolean allowLongPress = isHotseatLayout(v) || mWorkspace.allowLongPress();
            if (allowLongPress && !mDragController.isDragging()) {
                if (itemUnderLongClick == null) {
                    // User long pressed on empty space
                    mWorkspace.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                    startWallpaper();
                } else {

                    if (!(itemUnderLongClick instanceof Folder)) {
                        // User long pressed on an item
                        // mWorkspace.startDrag(longClickCellInfo); //鍙栨秷闀挎寜鐐瑰嚮鏄剧ず鏇存崲澹佺焊
                    }
                }
            }
        }
        return true;
    }

    boolean isHotseatLayout(View layout) {
        return mHotseat != null && layout != null && (layout instanceof CellLayout) && (layout == mHotseat.getLayout());
    }

    Hotseat getHotseat() {
        return mHotseat;
    }

    SearchDropTargetBar getSearchBar() {
        return mSearchDropTargetBar;
    }

    /**
     * Returns the CellLayout of the specified container at the specified
     * screen.
     */
    CellLayout getCellLayout(long container, int screen) {
        if (container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            if (mHotseat != null) {
                return mHotseat.getLayout();
            } else {
                return null;
            }
        } else {
            return (CellLayout) mWorkspace.getChildAt(screen);
        }
    }

    Workspace getWorkspace() {
        return mWorkspace;
    }

    // Now a part of LauncherModel.Callbacks. Used to reorder loading steps.
    public boolean isAllAppsVisible() {
        return (mState == State.APPS_CUSTOMIZE) || (mOnResumeState == State.APPS_CUSTOMIZE);
    }

    public boolean isAllAppsButtonRank(int rank) {
        return mHotseat.isAllAppsButtonRank(rank);
    }

    /**
     * Helper method for the cameraZoomIn/cameraZoomOut animations
     *
     * @param view        The view being animated
     * @param scaleFactor The scale factor used for the zoom
     */
    private void setPivotsForZoom(View view, float scaleFactor) {
        view.setPivotX(view.getWidth() / 2.0f);
        view.setPivotY(view.getHeight() / 2.0f);
    }

    void disableWallpaperIfInAllApps() {
        // Only disable it if we are in all apps
        if (isAllAppsVisible()) {
            if (mAppsCustomizeTabHost != null && !mAppsCustomizeTabHost.isTransitioning()) {
                // updateWallpaperVisibility(false);
                updateWallpaperVisibility(true);
            }
        }
    }

    void updateWallpaperVisibility(boolean visible) {
        int wpflags = visible ? WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER : 0;
        int curflags = getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
        if (wpflags != curflags) {
            getWindow().setFlags(wpflags, WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        }
    }

    private void dispatchOnLauncherTransitionPrepare(View v, boolean animated, boolean toWorkspace) {
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionPrepare(this, animated, toWorkspace);
        }
    }

    private void dispatchOnLauncherTransitionStart(View v, boolean animated, boolean toWorkspace) {
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionStart(this, animated, toWorkspace);
        }

        // Update the workspace transition step as well
        dispatchOnLauncherTransitionStep(v, 0f);
    }

    private void dispatchOnLauncherTransitionStep(View v, float t) {
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionStep(this, t);
        }
    }

    private void dispatchOnLauncherTransitionEnd(View v, boolean animated, boolean toWorkspace) {
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionEnd(this, animated, toWorkspace);
        }

        // Update the workspace transition step as well
        dispatchOnLauncherTransitionStep(v, 1f);
    }

    /**
     * Things to test when changing the following seven functions. - Home from
     * workspace - from center screen - from other screens - Home from all apps
     * - from center screen - from other screens - Back from all apps - from
     * center screen - from other screens - Launch app from workspace and quit -
     * with back - with home - Launch app from all apps and quit - with back -
     * with home - Go to a screen that's not the default, then all apps, and
     * launch and app, and go back - with back -with home - On workspace, long
     * press power and go back - with back - with home - On all apps, long press
     * power and go back - with back - with home - On workspace, power off - On
     * all apps, power off - Launch an app and turn off the screen while in that
     * app - Go back with home key - Go back with back key TODO: make this not
     * go to workspace - From all apps - From workspace - Enter and exit car
     * mode (becuase it causes an extra configuration changed) - From all apps -
     * From the center workspace - From another workspace
     */

    /**
     * Zoom the camera out from the workspace to reveal 'toView'. Assumes that
     * the view to show is anchored at either the very top or very bottom of the
     * screen.
     */
    private void showAppsCustomizeHelper(final boolean animated, final boolean springLoaded) {
        if (mStateAnimation != null) {
            mStateAnimation.cancel();
            mStateAnimation = null;
        }
        final Resources res = getResources();

        final int duration = res.getInteger(R.integer.config_appsCustomizeZoomInTime_8217);
        final int fadeDuration = res.getInteger(R.integer.config_appsCustomizeFadeInTime);
        final float scale = (float) res.getInteger(R.integer.config_appsCustomizeZoomScaleFactor);
        final View fromView = mWorkspace;
        final AppsCustomizeTabHost toView = mAppsCustomizeTabHost;
        final int startDelay = res.getInteger(R.integer.config_workspaceAppsCustomizeAnimationStagger);
        AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);// 0.0f锟斤拷1.0f为锟斤拷全透锟斤拷锟斤拷锟斤拷全锟斤拷透锟斤拷
        animation.setDuration(80);// 锟斤拷锟矫讹拷锟斤拷锟斤拷锟斤拷时锟斤拷为4锟斤拷
        // toView.setAnimation(animation);
        setPivotsForZoom(toView, scale);

        // Shrink workspaces away if going to AppsCustomize from workspace
        Animator workspaceAnim = mWorkspace.getChangeStateAnimation(Workspace.State.SMALL, animated);

        if (animated) {
            toView.setScaleX(scale);
            toView.setScaleY(scale);

            final LauncherViewPropertyAnimator scaleAnim = new LauncherViewPropertyAnimator(toView);
            scaleAnim.scaleX(1f).scaleY(1f).setDuration(duration).setInterpolator(new Workspace.ZoomOutInterpolator());

            toView.setVisibility(View.GONE);
            toView.setAlpha(0f);
            final ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(toView, "alpha", 0f, 1f).setDuration(fadeDuration);
            alphaAnim.setInterpolator(new DecelerateInterpolator(1.5f));
            alphaAnim.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (animation == null) {
                        throw new RuntimeException("animation is null");
                    }
                    float t = (Float) animation.getAnimatedValue();
                    dispatchOnLauncherTransitionStep(fromView, t);
                    dispatchOnLauncherTransitionStep(toView, t);
                }
            });

            // toView should appear right at the end of the workspace shrink
            // animation
            mStateAnimation = LauncherAnimUtils.createAnimatorSet();
            mStateAnimation.play(scaleAnim).after(startDelay);
            mStateAnimation.play(alphaAnim).after(startDelay);

            mStateAnimation.addListener(new AnimatorListenerAdapter() {
                boolean animationCancelled = false;

                @Override
                public void onAnimationStart(Animator animation) {
                    updateWallpaperVisibility(true);
                    // Prepare the position
                    toView.setTranslationX(0.0f);
                    toView.setTranslationY(0.0f);
                    toView.setVisibility(View.VISIBLE);
                    toView.bringToFront();
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    dispatchOnLauncherTransitionEnd(fromView, animated, false);
                    dispatchOnLauncherTransitionEnd(toView, animated, false);

                    if (mWorkspace != null && !springLoaded && !LauncherApplication.isScreenLarge()) {
                        // Hide the workspace scrollbar
                        mWorkspace.hideScrollingIndicator(true);
                        hideDockDivider();
                    }
                    if (!animationCancelled) {
                        // updateWallpaperVisibility(false); //by jxy
                        // allapp锟斤拷纸锟斤拷锟斤拷锟斤拷一锟斤拷
                        updateWallpaperVisibility(true);
                    }

                    // Hide the search bar
                    if (mSearchDropTargetBar != null) {
                        mSearchDropTargetBar.hideSearchBar(false);
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    animationCancelled = true;
                }
            });

            if (workspaceAnim != null) {
                mStateAnimation.play(workspaceAnim);
            }

            boolean delayAnim = false;
            final ViewTreeObserver observer;

            dispatchOnLauncherTransitionPrepare(fromView, animated, false);
            dispatchOnLauncherTransitionPrepare(toView, animated, false);

            // If any of the objects being animated haven't been measured/laid
            // out
            // yet, delay the animation until we get a layout pass
            if ((((LauncherTransitionable) toView).getContent().getMeasuredWidth() == 0) || (mWorkspace.getMeasuredWidth() == 0) || (toView.getMeasuredWidth() == 0)) {
                observer = mWorkspace.getViewTreeObserver();
                delayAnim = true;
            } else {
                observer = null;
            }

            final AnimatorSet stateAnimation = mStateAnimation;
            final Runnable startAnimRunnable = new Runnable() {
                public void run() {
                    // Check that mStateAnimation hasn't changed while
                    // we waited for a layout/draw pass
                    if (mStateAnimation != stateAnimation) return;
                    setPivotsForZoom(toView, scale);
                    dispatchOnLauncherTransitionStart(fromView, animated, false);
                    dispatchOnLauncherTransitionStart(toView, animated, false);
                    toView.post(new Runnable() {
                        public void run() {
                            // Check that mStateAnimation hasn't changed while
                            // we waited for a layout/draw pass
                            if (mStateAnimation != stateAnimation) return;
                            mStateAnimation.start();
                        }
                    });
                }
            };
            if (delayAnim) {
                final OnGlobalLayoutListener delayedStart = new OnGlobalLayoutListener() {
                    public void onGlobalLayout() {
                        toView.post(startAnimRunnable);
                        observer.removeOnGlobalLayoutListener(this);
                    }
                };
                observer.addOnGlobalLayoutListener(delayedStart);
            } else {
                startAnimRunnable.run();
            }
        } else {
            toView.setTranslationX(0.0f);
            toView.setTranslationY(0.0f);
            toView.setScaleX(1.0f);
            toView.setScaleY(1.0f);
            toView.setVisibility(View.VISIBLE);
            toView.bringToFront();

            if (!springLoaded && !LauncherApplication.isScreenLarge()) {
                // Hide the workspace scrollbar
                mWorkspace.hideScrollingIndicator(true);
                hideDockDivider();

                // Hide the search bar
                if (mSearchDropTargetBar != null) {
                    mSearchDropTargetBar.hideSearchBar(false);
                }
            }
            dispatchOnLauncherTransitionPrepare(fromView, animated, false);
            dispatchOnLauncherTransitionStart(fromView, animated, false);
            dispatchOnLauncherTransitionEnd(fromView, animated, false);
            dispatchOnLauncherTransitionPrepare(toView, animated, false);
            dispatchOnLauncherTransitionStart(toView, animated, false);
            dispatchOnLauncherTransitionEnd(toView, animated, false);
            // updateWallpaperVisibility(false); //by jxy allapp锟斤拷纸锟斤拷锟斤拷锟斤拷一锟斤拷
            updateWallpaperVisibility(true);
        }
    }

    /**
     * Zoom the camera back into the workspace, hiding 'fromView'. This is the
     * opposite of showAppsCustomizeHelper.
     *
     * @param animated If true, the transition will be animated.
     */
    private void hideAppsCustomizeHelper(State toState, final boolean animated, final boolean springLoaded, final Runnable onCompleteRunnable) {

        if (mStateAnimation != null) {
            mStateAnimation.cancel();
            mStateAnimation = null;
        }
        Resources res = getResources();

        final int duration = res.getInteger(R.integer.config_appsCustomizeZoomOutTime_8217);
        final int fadeOutDuration = res.getInteger(R.integer.config_appsCustomizeFadeOutTime);
        final float scaleFactor = (float) res.getInteger(R.integer.config_appsCustomizeZoomScaleFactor);
        final View fromView = mAppsCustomizeTabHost;
        final View toView = mWorkspace;
        Animator workspaceAnim = null;
        /*
         * AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);//
         * 0.0f锟斤拷1.0f为锟斤拷全透锟斤拷锟斤拷锟斤拷全锟斤拷透锟斤拷 animation.setDuration(80);//
         * 锟斤拷锟矫讹拷锟斤拷锟斤拷锟斤拷时锟斤拷为4锟斤拷 mWorkspace.setAnimation(animation);
         */
        if (toState == State.WORKSPACE) {
            int stagger = res.getInteger(R.integer.config_appsCustomizeWorkspaceAnimationStagger);
            workspaceAnim = mWorkspace.getChangeStateAnimation(Workspace.State.NORMAL, false, stagger);
            // workspaceAnim = mWorkspace.getChangeStateAnimation(
            // Workspace.State.NORMAL, animated, stagger);
        } else if (toState == State.APPS_CUSTOMIZE_SPRING_LOADED) {
            workspaceAnim = mWorkspace.getChangeStateAnimation(Workspace.State.SPRING_LOADED, animated);
        }

        setPivotsForZoom(fromView, scaleFactor);
        updateWallpaperVisibility(true);
        showHotseat(animated);
        if (animated) {
            final LauncherViewPropertyAnimator scaleAnim = new LauncherViewPropertyAnimator(fromView);
            scaleAnim.scaleX(scaleFactor).scaleY(scaleFactor).setDuration(duration).setInterpolator(new Workspace.ZoomInInterpolator());

            final ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(fromView, "alpha", 1f, 0f).setDuration(fadeOutDuration);
            alphaAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            alphaAnim.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float t = 1f - (Float) animation.getAnimatedValue();
                    dispatchOnLauncherTransitionStep(fromView, t);
                    dispatchOnLauncherTransitionStep(toView, t);
                }
            });

            mStateAnimation = LauncherAnimUtils.createAnimatorSet();

            dispatchOnLauncherTransitionPrepare(fromView, animated, true);
            dispatchOnLauncherTransitionPrepare(toView, animated, true);
            mAppsCustomizeContent.pauseScrolling();

            mStateAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    updateWallpaperVisibility(true);
                    fromView.setVisibility(View.GONE);
                    dispatchOnLauncherTransitionEnd(fromView, animated, true);
                    dispatchOnLauncherTransitionEnd(toView, animated, true);
                    if (mWorkspace != null) {
                        mWorkspace.hideScrollingIndicator(false);
                    }
                    if (onCompleteRunnable != null) {
                        onCompleteRunnable.run();
                    }
                    mAppsCustomizeContent.updateCurrentPageScroll();
                    mAppsCustomizeContent.resumeScrolling();
                }
            });

            mStateAnimation.playTogether(scaleAnim, alphaAnim);
            if (workspaceAnim != null) {
                mStateAnimation.play(workspaceAnim);
            }
            dispatchOnLauncherTransitionStart(fromView, animated, true);
            dispatchOnLauncherTransitionStart(toView, animated, true);
            final Animator stateAnimation = mStateAnimation;
            mWorkspace.post(new Runnable() {
                public void run() {
                    if (stateAnimation != mStateAnimation) return;
                    mStateAnimation.start();
                }
            });
        } else {
            fromView.setVisibility(View.GONE);
            dispatchOnLauncherTransitionPrepare(fromView, animated, true);
            dispatchOnLauncherTransitionStart(fromView, animated, true);
            dispatchOnLauncherTransitionEnd(fromView, animated, true);
            dispatchOnLauncherTransitionPrepare(toView, animated, true);
            dispatchOnLauncherTransitionStart(toView, animated, true);
            dispatchOnLauncherTransitionEnd(toView, animated, true);
            mWorkspace.hideScrollingIndicator(false);
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            mAppsCustomizeTabHost.onTrimMemory();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (!hasFocus) {
            // When another window occludes launcher (like the notification
            // shade, or recents),
            // ensure that we enable the wallpaper flag so that transitions are
            // done correctly.
            updateWallpaperVisibility(true);
        } else {
            // When launcher has focus again, disable the wallpaper if we are in
            // AllApps
            mWorkspace.postDelayed(new Runnable() {
                @Override
                public void run() {
                    disableWallpaperIfInAllApps();
                }
            }, 500);
        }
    }

    void showWorkspace(boolean animated) {
        mAppsCustomizeContent.setVisibility(View.GONE); // add by jxy
        showWorkspace(animated, null);
        updateGotoPageButtonStatus();
    }

    void showWorkspace(boolean animated, Runnable onCompleteRunnable) {
        if (mState != State.WORKSPACE) {
            boolean wasInSpringLoadedMode = (mState == State.APPS_CUSTOMIZE_SPRING_LOADED);
            mWorkspace.setVisibility(View.VISIBLE);
            hideAppsCustomizeHelper(State.WORKSPACE, animated, false, onCompleteRunnable);

            // Show the search bar (only animate if we were showing the drop
            // target bar in spring
            // loaded mode)
            if (mSearchDropTargetBar != null) {
                mSearchDropTargetBar.showSearchBar(wasInSpringLoadedMode);
            }

            // We only need to animate in the dock divider if we're going from
            // spring loaded mode
            showDockDivider(animated && wasInSpringLoadedMode);

            // Set focus to the AppsCustomize button
            if (mAllAppsButton != null) {
                mAllAppsButton.requestFocus();
            }
        }

        mWorkspace.flashScrollingIndicator(animated);

        // Change the state *after* we've called all the transition code
        mState = State.WORKSPACE;

        // Resume the auto-advance of widgets
        mUserPresent = true;
        updateRunning();

        // Send an accessibility event to announce the context change
        getWindow().getDecorView().sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void showAllApps(boolean animated) {
        DragController.setUnInstallState(false); // by jxy
        mAppsCustomizeContent.setVisibility(View.VISIBLE); // by jxy
        if (mState != State.WORKSPACE)
            return;

        showAppsCustomizeHelper(animated, false);
        mAppsCustomizeTabHost.requestFocus();

        // Change the state *after* we've called all the transition code
        mState = State.APPS_CUSTOMIZE;

        // Pause the auto-advance of widgets until we are out of AllApps
        mUserPresent = false;
        updateRunning();
        closeFolder();

        // Send an accessibility event to announce the context change
        getWindow().getDecorView().sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void enterSpringLoadedDragMode() {
        if (isAllAppsVisible()) {
            hideAppsCustomizeHelper(State.APPS_CUSTOMIZE_SPRING_LOADED, true, true, null);
            hideDockDivider();
            mState = State.APPS_CUSTOMIZE_SPRING_LOADED;
        }
    }

    void exitSpringLoadedDragModeDelayed(final boolean successfulDrop, boolean extendedDelay, final Runnable onCompleteRunnable) {
        if (mState != State.APPS_CUSTOMIZE_SPRING_LOADED) return;

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (successfulDrop) {
                    // Before we show workspace, hide all apps again because
                    // exitSpringLoadedDragMode made it visible. This is a bit
                    // hacky; we should
                    // clean up our state transition functions
                    mAppsCustomizeTabHost.setVisibility(View.GONE);
                    showWorkspace(true, onCompleteRunnable);
                } else {
                    exitSpringLoadedDragMode();
                }
            }
        }, (extendedDelay ? EXIT_SPRINGLOADED_MODE_LONG_TIMEOUT : EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT));
    }

    void exitSpringLoadedDragMode() {
        if (mState == State.APPS_CUSTOMIZE_SPRING_LOADED) {
            final boolean animated = true;
            final boolean springLoaded = true;
            showAppsCustomizeHelper(animated, springLoaded);
            mState = State.APPS_CUSTOMIZE;
        }
        // Otherwise, we are not in spring loaded mode, so don't do anything.
    }

    void hideDockDivider() {
        if (mQsbDivider != null && mDockDivider != null) {
            /*
             * mQsbDivider.setVisibility(View.INVISIBLE); // by jxy
             * mDockDivider.setVisibility(View.INVISIBLE);
             */
            mQsbDivider.setVisibility(View.GONE);
            mDockDivider.setVisibility(View.GONE);
        }
    }

    void showDockDivider(boolean animated) {
        if (mQsbDivider != null && mDockDivider != null) {
            /*
             * mQsbDivider.setVisibility(View.VISIBLE); by jxy
             * mDockDivider.setVisibility(View.VISIBLE);
             */
            mQsbDivider.setVisibility(View.GONE);
            mDockDivider.setVisibility(View.GONE);
            if (mDividerAnimator != null) {
                mDividerAnimator.cancel();
                mQsbDivider.setAlpha(1f);
                mDockDivider.setAlpha(1f);
                mDividerAnimator = null;
            }
            if (animated) {
                mDividerAnimator = LauncherAnimUtils.createAnimatorSet();
                mDividerAnimator.playTogether(LauncherAnimUtils.ofFloat(mQsbDivider, "alpha", 1f), LauncherAnimUtils.ofFloat(mDockDivider, "alpha", 1f));
                int duration = 0;
                if (mSearchDropTargetBar != null) {
                    duration = mSearchDropTargetBar.getTransitionInDuration();
                }
                mDividerAnimator.setDuration(duration);
                mDividerAnimator.start();
            }
        }
    }

    void lockAllApps() {
        // TODO
    }

    void unlockAllApps() {
        // TODO
    }

    /**
     * Shows the hotseat area.
     */
    void showHotseat(boolean animated) {
        if (!LauncherApplication.isScreenLarge()) {
            if (animated) {
                if (mHotseat.getAlpha() != 1f) {
                    int duration = 0;
                    if (mSearchDropTargetBar != null) {
                        duration = mSearchDropTargetBar.getTransitionInDuration();
                    }
                    mHotseat.animate().alpha(1f).setDuration(duration);
                }
            } else {
                mHotseat.setAlpha(1f);
            }
        }
    }

    /**
     * Hides the hotseat area.
     */
    void hideHotseat(boolean animated) {
        if (!LauncherApplication.isScreenLarge()) {
            if (animated) {
                if (mHotseat.getAlpha() != 0f) {
                    int duration = 0;
                    if (mSearchDropTargetBar != null) {
                        duration = mSearchDropTargetBar.getTransitionOutDuration();
                    }
                    mHotseat.animate().alpha(0f).setDuration(duration);
                }
            } else {
                mHotseat.setAlpha(0f);
            }
        }
    }

    /**
     * Add an item from all apps or customize onto the given workspace screen.
     * If layout is null, add to the current screen.
     */
    void addExternalItemToScreen(ItemInfo itemInfo, final CellLayout layout) {
        if (!mWorkspace.addExternalItemToScreen(itemInfo, layout)) {
            showOutOfSpaceMessage(isHotseatLayout(layout));
        }
    }

    /**
     * Maps the current orientation to an index for referencing orientation
     * correct global icons
     */
    private int getCurrentOrientationIndexForGlobalIcons() {
        // default - 0, landscape - 1
        switch (getResources().getConfiguration().orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                return 1;
            default:
                return 0;
        }
    }

    private Drawable getExternalPackageToolbarIcon(ComponentName activityName, String resourceName) {
        try {
            PackageManager packageManager = getPackageManager();
            // Look for the toolbar icon specified in the activity meta-data
            Bundle metaData = packageManager.getActivityInfo(activityName, PackageManager.GET_META_DATA).metaData;
            if (metaData != null) {
                int iconResId = metaData.getInt(resourceName);
                if (iconResId != 0) {
                    Resources res = packageManager.getResourcesForActivity(activityName);
                    return res.getDrawable(iconResId);
                }
            }
        } catch (NameNotFoundException e) {
            // This can happen if the activity defines an invalid drawable
            Log.w(TAG, "Failed to load toolbar icon; " + activityName.flattenToShortString() + " not found", e);
        } catch (Resources.NotFoundException nfe) {
            // This can happen if the activity defines an invalid drawable
            Log.w(TAG, "Failed to load toolbar icon from " + activityName.flattenToShortString(), nfe);
        }
        return null;
    }

    // if successful in getting icon, return it; otherwise, set button to use
    // default drawable
    private Drawable.ConstantState updateTextButtonWithIconFromExternalActivity(int buttonId, ComponentName activityName, int fallbackDrawableId, String toolbarResourceName) {
        Drawable toolbarIcon = getExternalPackageToolbarIcon(activityName, toolbarResourceName);
        Resources r = getResources();
        int w = r.getDimensionPixelSize(R.dimen.toolbar_external_icon_width);
        int h = r.getDimensionPixelSize(R.dimen.toolbar_external_icon_height);

        TextView button = (TextView) findViewById(buttonId);
        // If we were unable to find the icon via the meta-data, use a generic
        // one
        if (toolbarIcon == null) {
            toolbarIcon = r.getDrawable(fallbackDrawableId);
            toolbarIcon.setBounds(0, 0, w, h);
            if (button != null) {
                button.setCompoundDrawables(toolbarIcon, null, null, null);
            }
            return null;
        } else {
            toolbarIcon.setBounds(0, 0, w, h);
            if (button != null) {
                button.setCompoundDrawables(toolbarIcon, null, null, null);
            }
            return toolbarIcon.getConstantState();
        }
    }

    // if successful in getting icon, return it; otherwise, set button to use
    // default drawable
    private Drawable.ConstantState updateButtonWithIconFromExternalActivity(int buttonId, ComponentName activityName, int fallbackDrawableId, String toolbarResourceName) {
        ImageView button = (ImageView) findViewById(buttonId);
        Drawable toolbarIcon = getExternalPackageToolbarIcon(activityName, toolbarResourceName);

        if (button != null) {
            // If we were unable to find the icon via the meta-data, use a
            // generic one
            if (toolbarIcon == null) {
                button.setImageResource(fallbackDrawableId);
            } else {
                button.setImageDrawable(toolbarIcon);
            }
        }

        return toolbarIcon != null ? toolbarIcon.getConstantState() : null;

    }

    private void updateTextButtonWithDrawable(int buttonId, Drawable d) {
        TextView button = (TextView) findViewById(buttonId);
        button.setCompoundDrawables(d, null, null, null);
    }

    private void updateButtonWithDrawable(int buttonId, Drawable.ConstantState d) {
        ImageView button = (ImageView) findViewById(buttonId);
        button.setImageDrawable(d.newDrawable(getResources()));
    }

    private void invalidatePressedFocusedStates(View container, View button) {
        if (container instanceof HolographicLinearLayout) {
            HolographicLinearLayout layout = (HolographicLinearLayout) container;
            layout.invalidatePressedFocusedStates();
        } else if (button instanceof HolographicImageView) {
            HolographicImageView view = (HolographicImageView) button;
            view.invalidatePressedFocusedStates();
        }
    }

    private boolean updateGlobalSearchIcon() {
        if (true)
            return false;

        final ImageView searchButton = (ImageView) findViewById(R.id.search_button);
        final View voiceButton = findViewById(R.id.voice_button);
        final View voiceButtonProxy = findViewById(R.id.voice_button_proxy);

        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        ComponentName activityName = searchManager.getGlobalSearchActivity();
        if (activityName != null) {
            int coi = getCurrentOrientationIndexForGlobalIcons();
            sGlobalSearchIcon[coi] = updateButtonWithIconFromExternalActivity(R.id.search_button, activityName, R.drawable.ic_home_search_normal_holo, TOOLBAR_SEARCH_ICON_METADATA_NAME);
            if (sGlobalSearchIcon[coi] == null) {
                sGlobalSearchIcon[coi] = updateButtonWithIconFromExternalActivity(R.id.search_button, activityName, R.drawable.ic_home_search_normal_holo, TOOLBAR_ICON_METADATA_NAME);
            }


            searchButton.setVisibility(View.VISIBLE);

            return true;
        } else {
            // We disable both search and voice search when there is no global
            // search provider
            searchButton.setVisibility(View.GONE);
            voiceButton.setVisibility(View.GONE);
            if (voiceButtonProxy != null) {
                voiceButtonProxy.setVisibility(View.GONE);
            }
            return false;
        }
    }

    private void updateGlobalSearchIcon(Drawable.ConstantState d) {
        final View searchButton = (ImageView) findViewById(R.id.search_button);
        updateButtonWithDrawable(R.id.search_button, d);
    }

    private boolean updateVoiceSearchIcon(boolean searchVisible) {

        final View voiceButton = findViewById(R.id.voice_button);
        final View voiceButtonProxy = findViewById(R.id.voice_button_proxy);

        // We only show/update the voice search icon if the search icon is
        // enabled as well
        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        ComponentName globalSearchActivity = searchManager.getGlobalSearchActivity();

        ComponentName activityName = null;
        if (globalSearchActivity != null) {
            // Check if the global search activity handles voice search
            Intent intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
            intent.setPackage(globalSearchActivity.getPackageName());
            activityName = intent.resolveActivity(getPackageManager());
        }

        if (activityName == null) {
            // Fallback: check if an activity other than the global search
            // activity
            // resolves this
            Intent intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
            activityName = intent.resolveActivity(getPackageManager());
        }
        if (searchVisible && activityName != null) {
            int coi = getCurrentOrientationIndexForGlobalIcons();
            sVoiceSearchIcon[coi] = updateButtonWithIconFromExternalActivity(R.id.voice_button, activityName, R.drawable.ic_home_voice_search_holo, TOOLBAR_VOICE_SEARCH_ICON_METADATA_NAME);
            if (sVoiceSearchIcon[coi] == null) {
                sVoiceSearchIcon[coi] = updateButtonWithIconFromExternalActivity(R.id.voice_button, activityName, R.drawable.ic_home_voice_search_holo, TOOLBAR_ICON_METADATA_NAME);
            }

            voiceButton.setVisibility(View.VISIBLE);
            if (voiceButtonProxy != null) {
                voiceButtonProxy.setVisibility(View.VISIBLE);
            }

            return true;
        } else {

            voiceButton.setVisibility(View.GONE);
            if (voiceButtonProxy != null) {
                voiceButtonProxy.setVisibility(View.GONE);
            }
            return false;
        }
    }

    private void updateVoiceSearchIcon(Drawable.ConstantState d) {

        final View voiceButton = findViewById(R.id.voice_button);
        updateButtonWithDrawable(R.id.voice_button, d);

    }

    /**
     * Sets the app market icon
     */
    private void updateAppMarketIcon() {
        final View marketButton = findViewById(R.id.market_button);
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MARKET);
        // Find the app market activity by resolving an intent.
        // (If multiple app markets are installed, it will return the
        // ResolverActivity.)
        ComponentName activityName = intent.resolveActivity(getPackageManager());
        if (activityName != null) {
            int coi = getCurrentOrientationIndexForGlobalIcons();
            mAppMarketIntent = intent;
            sAppMarketIcon[coi] = updateTextButtonWithIconFromExternalActivity(R.id.market_button, activityName, R.drawable.ic_launcher_market_holo, TOOLBAR_ICON_METADATA_NAME);
            marketButton.setVisibility(View.VISIBLE);
        } else {
            // We should hide and disable the view so that we don't try and
            // restore the visibility
            // of it when we swap between drag & normal states from
            // IconDropTarget subclasses.
            marketButton.setVisibility(View.GONE);
            marketButton.setEnabled(false);
        }
    }

    private void updateAppMarketIcon(Drawable.ConstantState d) {
        // Ensure that the new drawable we are creating has the approprate
        // toolbar icon bounds
        Resources r = getResources();
        Drawable marketIconDrawable = d.newDrawable(r);
        int w = r.getDimensionPixelSize(R.dimen.toolbar_external_icon_width);
        int h = r.getDimensionPixelSize(R.dimen.toolbar_external_icon_height);
        marketIconDrawable.setBounds(0, 0, w, h);

        updateTextButtonWithDrawable(R.id.market_button, marketIconDrawable);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        final boolean result = super.dispatchPopulateAccessibilityEvent(event);
        final List<CharSequence> text = event.getText();
        text.clear();
        // Populate event with a fake title based on the current state.
        if (mState == State.APPS_CUSTOMIZE) {
            text.add(getString(R.string.all_apps_button_label));
        } else {
            text.add(getString(R.string.all_apps_home_button_label));
        }
        return result;
    }

    /**
     * Receives notifications when system dialogs are to be closed.
     */
    private class CloseSystemDialogsIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            closeSystemDialogs();
        }
    }

    /**
     * Receives notifications whenever the appwidgets are reset.
     */
    private class AppWidgetResetObserver extends ContentObserver {
        public AppWidgetResetObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            onAppWidgetReset();
        }
    }

    /**
     * If the activity is currently paused, signal that we need to re-run the
     * loader in onResume.
     * <p>
     * This needs to be called from incoming places where resources might have
     * been loaded while we are paused. That is becaues the Configuration might
     * be wrong when we're not running, and if it comes back to what it was when
     * we were paused, we are not restarted.
     * <p>
     * Implementation of the method from LauncherModel.Callbacks.
     *
     * @return true if we are currently paused. The caller might be able to skip
     * some work in that case since we will come back again.
     */
    public boolean setLoadOnResume() {
        if (mPaused) {
            Log.i(TAG, "setLoadOnResume");
            mOnResumeNeedsLoad = true;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public int getCurrentWorkspaceScreen() {
        if (mWorkspace != null) {
            return mWorkspace.getCurrentPage();
        } else {
            return SCREEN_COUNT / 2;
        }
    }

    /**
     * Refreshes the shortcuts shown on the workspace.
     * <p>
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void startBinding() {
        final Workspace workspace = mWorkspace;

        mNewShortcutAnimatePage = -1;
        mNewShortcutAnimateViews.clear();
        mWorkspace.clearDropTargets();
        int count = workspace.getChildCount();
        for (int i = 0; i < count; i++) {
            // Use removeAllViewsInLayout() to avoid an extra requestLayout()
            // and invalidate().
            final CellLayout layoutParent = (CellLayout) workspace.getChildAt(i);
            layoutParent.removeAllViewsInLayout();
        }
        mWidgetsToAdvance.clear();
        if (mHotseat != null) {
            mHotseat.resetLayout();
        }
    }

    /**
     * Bind the items start-end from the list.
     * <p>
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindItems(ArrayList<ItemInfo> shortcuts, int start, int end) {
        setLoadOnResume();

        // Get the list of added shortcuts and intersect them with the set of
        // shortcuts here
        Set<String> newApps = new HashSet<String>();
        newApps = mSharedPrefs.getStringSet(InstallShortcutReceiver.NEW_APPS_LIST_KEY, newApps);

        Workspace workspace = mWorkspace;
        for (int i = start; i < end; i++) {
            final ItemInfo item = shortcuts.get(i);

            // Short circuit if we are loading dock items for a configuration
            // which has no dock
            if (item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT && mHotseat == null) {
                continue;
            }

            switch (item.itemType) {
                case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                    ShortcutInfo info = (ShortcutInfo) item;
                    String uri = info.intent.toUri(0).toString();
                    View shortcut = createShortcut(info);
                    workspace.addInScreen(shortcut, item.container, item.screen, item.cellX, item.cellY, 1, 1, false);
                    boolean animateIconUp = false;
                    synchronized (newApps) {
                        if (newApps.contains(uri)) {
                            animateIconUp = newApps.remove(uri);
                        }
                    }
                    if (animateIconUp) {
                        // Prepare the view to be animated up
                        shortcut.setAlpha(0f);
                        shortcut.setScaleX(0f);
                        shortcut.setScaleY(0f);
                        mNewShortcutAnimatePage = item.screen;
                        if (!mNewShortcutAnimateViews.contains(shortcut)) {
                            mNewShortcutAnimateViews.add(shortcut);
                        }
                    }
                    break;
                case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                    FolderIcon newFolder = FolderIcon.fromXml(R.layout.folder_icon, this, (ViewGroup) workspace.getChildAt(workspace.getCurrentPage()), (FolderInfo) item, mIconCache);
                    workspace.addInScreen(newFolder, item.container, item.screen, item.cellX, item.cellY, 1, 1, false);
                    break;
            }
        }

        workspace.requestLayout();
//        View view = LayoutInflater.from(this).inflate(R.layout.homepage_one, null);
//        workspace.addInScreen(view, LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET, 0, 0, 0, 6, 2, false);
    }

    /**
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindFolders(HashMap<Long, FolderInfo> folders) {
        setLoadOnResume();
        sFolders.clear();
        sFolders.putAll(folders);
    }

    /**
     * Add the views for a widget to the workspace.
     * <p>
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppWidget(LauncherAppWidgetInfo item) {
        setLoadOnResume();

        final long start = DEBUG_WIDGETS ? SystemClock.uptimeMillis() : 0;
        if (DEBUG_WIDGETS) {
            Log.d(TAG, "bindAppWidget: " + item);
        }
        final Workspace workspace = mWorkspace;

        final int appWidgetId = item.appWidgetId;
        final AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (DEBUG_WIDGETS) {
            Log.d(TAG, "bindAppWidget: id=" + item.appWidgetId + " belongs to component " + appWidgetInfo.provider);
        }

        item.hostView = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);

        item.hostView.setTag(item);
        item.onBindAppWidget(this);

        workspace.addInScreen(item.hostView, item.container, item.screen, item.cellX, item.cellY, item.spanX, item.spanY, false);
        addWidgetToAutoAdvanceIfNeeded(item.hostView, appWidgetInfo);

        workspace.requestLayout();

        if (DEBUG_WIDGETS) {
            Log.d(TAG, "bound widget id=" + item.appWidgetId + " in " + (SystemClock.uptimeMillis() - start) + "ms");
        }
    }

    public void onPageBoundSynchronously(int page) {
        mSynchronouslyBoundPages.add(page);
    }

    /**
     * Callback saying that there aren't any more items to bind.
     * <p>
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void finishBindingItems() {
        setLoadOnResume();

        if (mSavedState != null) {
            if (!mWorkspace.hasFocus()) {
                mWorkspace.getChildAt(mWorkspace.getCurrentPage()).requestFocus();
            }
            mSavedState = null;
        }

        mWorkspace.restoreInstanceStateForRemainingPages();

        // If we received the result of any pending adds while the loader was
        // running (e.g. the
        // widget configuration forced an orientation change), process them now.
        for (int i = 0; i < sPendingAddList.size(); i++) {
            completeAdd(sPendingAddList.get(i));
        }
        sPendingAddList.clear();

        // Update the market app icon as necessary (the other icons will be
        // managed in response to
        // package changes in bindSearchablesChanged()
        updateAppMarketIcon();

        // Animate up any icons as necessary
        if (mVisible || mWorkspaceLoading) {
            Runnable newAppsRunnable = new Runnable() {
                @Override
                public void run() {
                    runNewAppsAnimation(false);
                }
            };

            boolean willSnapPage = mNewShortcutAnimatePage > -1 && mNewShortcutAnimatePage != mWorkspace.getCurrentPage();
            if (canRunNewAppsAnimation()) {
                // If the user has not interacted recently, then either snap to
                // the new page to show
                // the new-apps animation or just run them if they are to appear
                // on the current page
                if (willSnapPage) {
                    mWorkspace.snapToPage(mNewShortcutAnimatePage, newAppsRunnable);
                } else {
                    runNewAppsAnimation(false);
                }
            } else {
                // If the user has interacted recently, then just add the items
                // in place if they
                // are on another page (or just normally if they are added to
                // the current page)
                runNewAppsAnimation(willSnapPage);
            }
        }

        mWorkspaceLoading = false;
    }

    private boolean canRunNewAppsAnimation() {
        long diff = System.currentTimeMillis() - mDragController.getLastGestureUpTime();
        return diff > (NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS * 1000);
    }

    /**
     * Runs a new animation that scales up icons that were added while Launcher
     * was in the background.
     *
     * @param immediate whether to run the animation or show the results immediately
     */
    private void runNewAppsAnimation(boolean immediate) {
        AnimatorSet anim = LauncherAnimUtils.createAnimatorSet();
        Collection<Animator> bounceAnims = new ArrayList<Animator>();

        // Order these new views spatially so that they animate in order
        Collections.sort(mNewShortcutAnimateViews, new Comparator<View>() {
            @Override
            public int compare(View a, View b) {
                CellLayout.LayoutParams alp = (CellLayout.LayoutParams) a.getLayoutParams();
                CellLayout.LayoutParams blp = (CellLayout.LayoutParams) b.getLayoutParams();
                int cellCountX = LauncherModel.getCellCountX();
                return (alp.cellY * cellCountX + alp.cellX) - (blp.cellY * cellCountX + blp.cellX);
            }
        });

        // Animate each of the views in place (or show them immediately if
        // requested)
        if (immediate) {
            for (View v : mNewShortcutAnimateViews) {
                v.setAlpha(1f);
                v.setScaleX(1f);
                v.setScaleY(1f);
            }
        } else {
            for (int i = 0; i < mNewShortcutAnimateViews.size(); ++i) {
                View v = mNewShortcutAnimateViews.get(i);
                ValueAnimator bounceAnim = LauncherAnimUtils.ofPropertyValuesHolder(v, PropertyValuesHolder.ofFloat("alpha", 1f), PropertyValuesHolder.ofFloat("scaleX", 1f), PropertyValuesHolder.ofFloat("scaleY", 1f));
                bounceAnim.setDuration(InstallShortcutReceiver.NEW_SHORTCUT_BOUNCE_DURATION);
                bounceAnim.setStartDelay(i * InstallShortcutReceiver.NEW_SHORTCUT_STAGGER_DELAY);
                bounceAnim.setInterpolator(new SmoothPagedView.OvershootInterpolator());
                bounceAnims.add(bounceAnim);
            }
            anim.playTogether(bounceAnims);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mWorkspace != null) {
                        mWorkspace.postDelayed(mBuildLayersRunnable, 500);
                    }
                }
            });
            anim.start();
        }

        // Clean up
        mNewShortcutAnimatePage = -1;
        mNewShortcutAnimateViews.clear();
        new Thread("clearNewAppsThread") {
            public void run() {
                mSharedPrefs.edit().putInt(InstallShortcutReceiver.NEW_APPS_PAGE_KEY, -1).putStringSet(InstallShortcutReceiver.NEW_APPS_LIST_KEY, null).commit();
            }
        }.start();
    }

    @Override
    public void bindSearchablesChanged() {
        boolean searchVisible = updateGlobalSearchIcon();
        boolean voiceVisible = updateVoiceSearchIcon(searchVisible);
        if (mSearchDropTargetBar != null) {
            mSearchDropTargetBar.onSearchPackagesChanged(searchVisible, voiceVisible);
        }
    }

    /**
     * Add the icons for all apps.
     * <p>
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAllApplications(final ArrayList<ApplicationInfo> apps) {
        Runnable setAllAppsRunnable = new Runnable() {
            public void run() {
                if (mAppsCustomizeContent != null) {
                    mAppsCustomizeContent.setApps(apps);
                }
            }
        };

        // Remove the progress bar entirely; we could also make it GONE
        // but better to remove it since we know it's not going to be used
        View progressBar = mAppsCustomizeTabHost.findViewById(R.id.apps_customize_progress_bar);
        if (progressBar != null) {
            ((ViewGroup) progressBar.getParent()).removeView(progressBar);

            // We just post the call to setApps so the user sees the progress
            // bar
            // disappear-- otherwise, it just looks like the progress bar froze
            // which doesn't look great
            mAppsCustomizeTabHost.post(setAllAppsRunnable);
        } else {
            // If we did not initialize the spinner in onCreate, then we can
            // directly set the
            // list of applications without waiting for any progress bars views
            // to be hidden.
            setAllAppsRunnable.run();
        }
    }

    /**
     * A package was installed.
     * <p>
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppsAdded(ArrayList<ApplicationInfo> apps) {
        setLoadOnResume();

        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.addApps(apps);
        }
    }

    /**
     * A package was updated.
     * <p>
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppsUpdated(ArrayList<ApplicationInfo> apps) {

        setLoadOnResume();
        if (mWorkspace != null) {
            mWorkspace.updateShortcuts(apps);
        }

        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.updateApps(apps);
        }
    }

    /**
     * A package was uninstalled.
     * <p>
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppsRemoved(ArrayList<String> packageNames, boolean permanent) {
        if (permanent) {
            mWorkspace.removeItems(packageNames);
        }

        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.removeApps(packageNames);
        }

        // Notify the drag controller
        mDragController.onAppsRemoved(packageNames, this);
    }

    /**
     * A number of packages were updated.
     */
    public void bindPackagesUpdated() {
        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.onPackagesUpdated();
        }
    }

    private int mapConfigurationOriActivityInfoOri(int configOri) {
        final Display d = getWindowManager().getDefaultDisplay();
        int naturalOri = Configuration.ORIENTATION_LANDSCAPE;
        switch (d.getRotation()) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                // We are currently in the same basic orientation as the natural
                // orientation
                naturalOri = configOri;
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                // We are currently in the other basic orientation to the natural
                // orientation
                naturalOri = (configOri == Configuration.ORIENTATION_LANDSCAPE) ? Configuration.ORIENTATION_PORTRAIT : Configuration.ORIENTATION_LANDSCAPE;
                break;
        }

        int[] oriMap = {ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT, ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE};
        // Since the map starts at portrait, we need to offset if this device's
        // natural orientation
        // is landscape.
        int indexOffset = 0;
        if (naturalOri == Configuration.ORIENTATION_LANDSCAPE) {
            indexOffset = 1;
        }
        return oriMap[(d.getRotation() + indexOffset) % 4];
    }

    public boolean isRotationEnabled() {
        boolean forceEnableRotation = doesFileExist(FORCE_ENABLE_ROTATION_PROPERTY);
        boolean enableRotation = forceEnableRotation || getResources().getBoolean(R.bool.allow_rotation);
        return enableRotation;
    }

    public void lockScreenOrientation() {
        if (isRotationEnabled()) {
            setRequestedOrientation(mapConfigurationOriActivityInfoOri(getResources().getConfiguration().orientation));
        }
    }

    public void unlockScreenOrientation(boolean immediate) {
        if (isRotationEnabled()) {
            if (immediate) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            } else {
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    }
                }, mRestoreScreenOrientationDelay);
            }
        }
    }

    /* Cling related */
    private boolean isClingsEnabled() {
        // disable clings when running in a test harness
        if (ActivityManager.isRunningInTestHarness()) return false;

        return true;
    }

    private Cling initCling(int clingId, int[] positionData, boolean animate, int delay) {
        final Cling cling = (Cling) findViewById(clingId);
        if (cling != null) {
            cling.init(this, positionData);
            cling.setVisibility(View.VISIBLE);
            cling.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            if (animate) {
                cling.buildLayer();
                cling.setAlpha(0f);
                cling.animate().alpha(1f).setInterpolator(new AccelerateInterpolator()).setDuration(SHOW_CLING_DURATION).setStartDelay(delay).start();
            } else {
                cling.setAlpha(1f);
            }
            cling.setFocusableInTouchMode(true);
            cling.post(new Runnable() {
                public void run() {
                    cling.setFocusable(true);
                    cling.requestFocus();
                }
            });
            mHideFromAccessibilityHelper.setImportantForAccessibilityToNo(mDragLayer, clingId == R.id.all_apps_cling);
        }
        return cling;
    }

    private void dismissCling(final Cling cling, final String flag, int duration) {
        if (cling != null && cling.getVisibility() == View.VISIBLE) {
            ObjectAnimator anim = LauncherAnimUtils.ofFloat(cling, "alpha", 0f);
            anim.setDuration(duration);
            anim.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    cling.setVisibility(View.GONE);
                    cling.cleanup();
                    // We should update the shared preferences on a background
                    // thread
                    new Thread("dismissClingThread") {
                        public void run() {
                            SharedPreferences.Editor editor = mSharedPrefs.edit();
                            editor.putBoolean(flag, true);
                            editor.commit();
                        }
                    }.start();
                }

                ;
            });
            anim.start();
            mHideFromAccessibilityHelper.restoreImportantForAccessibility(mDragLayer);
        }
    }

    private void removeCling(int id) {
        final View cling = findViewById(id);
        if (cling != null) {
            final ViewGroup parent = (ViewGroup) cling.getParent();
            parent.post(new Runnable() {
                @Override
                public void run() {
                    parent.removeView(cling);
                }
            });
            mHideFromAccessibilityHelper.restoreImportantForAccessibility(mDragLayer);
        }
    }

    private boolean skipCustomClingIfNoAccounts() {
        Cling cling = (Cling) findViewById(R.id.workspace_cling);
        boolean customCling = cling.getDrawIdentifier().equals("workspace_custom");
        if (customCling) {
            AccountManager am = AccountManager.get(this);
            Account[] accounts = am.getAccountsByType("com.google");
            return accounts.length == 0;
        }
        return false;
    }

    public void showFirstRunWorkspaceCling() {
        // Enable the clings only if they have not been dismissed before
        /*
         * if (isClingsEnabled() &&
         * !mSharedPrefs.getBoolean(Cling.WORKSPACE_CLING_DISMISSED_KEY, false)
         * && !skipCustomClingIfNoAccounts() ) { // If we're not using the
         * default workspace layout, replace workspace cling // with a custom
         * workspace cling (usually specified in an overlay) // For now, only do
         * this on tablets if
         * (mSharedPrefs.getInt(LauncherProvider.DEFAULT_WORKSPACE_RESOURCE_ID,
         * 0) != 0 && getResources().getBoolean(R.bool.config_useCustomClings))
         * { // Use a custom cling View cling =
         * findViewById(R.id.workspace_cling); ViewGroup clingParent =
         * (ViewGroup) cling.getParent(); int clingIndex =
         * clingParent.indexOfChild(cling);
         * clingParent.removeViewAt(clingIndex); View customCling =
         * mInflater.inflate(R.layout.custom_workspace_cling, clingParent,
         * false); clingParent.addView(customCling, clingIndex);
         * customCling.setId(R.id.workspace_cling); }
         * initCling(R.id.workspace_cling, null, false, 0); } else {
         * removeCling(R.id.workspace_cling); }
         */
    }

    public void showFirstRunAllAppsCling(int[] position) {
        // Enable the clings only if they have not been dismissed before
        /*
         * if (isClingsEnabled() &&
         * !mSharedPrefs.getBoolean(Cling.ALLAPPS_CLING_DISMISSED_KEY, false)) {
         * initCling(R.id.all_apps_cling, position, true, 0); } else {
         * removeCling(R.id.all_apps_cling); }
         */
    }

    public Cling showFirstRunFoldersCling() {
        // Enable the clings only if they have not been dismissed before
        if (isClingsEnabled() && !mSharedPrefs.getBoolean(Cling.FOLDER_CLING_DISMISSED_KEY, false)) {
            return initCling(R.id.folder_cling, null, true, 0);
        } else {
            removeCling(R.id.folder_cling);
            return null;
        }
    }

    public boolean isFolderClingVisible() {
        Cling cling = (Cling) findViewById(R.id.folder_cling);
        if (cling != null) {
            return cling.getVisibility() == View.VISIBLE;
        }
        return false;
    }

    public void dismissWorkspaceCling(View v) {
        Cling cling = (Cling) findViewById(R.id.workspace_cling);
        dismissCling(cling, Cling.WORKSPACE_CLING_DISMISSED_KEY, DISMISS_CLING_DURATION);
    }

    public void dismissAllAppsCling(View v) {
        Cling cling = (Cling) findViewById(R.id.all_apps_cling);
        dismissCling(cling, Cling.ALLAPPS_CLING_DISMISSED_KEY, DISMISS_CLING_DURATION);
    }

    public void dismissFolderCling(View v) {
        Cling cling = (Cling) findViewById(R.id.folder_cling);
        dismissCling(cling, Cling.FOLDER_CLING_DISMISSED_KEY, DISMISS_CLING_DURATION);
    }

    /**
     * Prints out out state for debugging.
     */
    public void dumpState() {
        mModel.dumpState();
        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.dumpState();
        }
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        writer.println(" ");
        writer.println("Debug logs: ");
        for (int i = 0; i < sDumpLogs.size(); i++) {
            writer.println("  " + sDumpLogs.get(i));
        }
    }

    public void dumpDebugLogsToConsole() {
        for (int i = 0; i < sDumpLogs.size(); i++) {
            Log.d(TAG, "  " + sDumpLogs.get(i));
        }

    }

    private void updateSpeedUnitText() {
        byte[] unit = new byte[1];
        CommonData.readDataToMeta(unit, 0x84);
        int unitData = unit[0];
        Log.e(TAG, "unit Data = " + unitData);
        if (tvGPSSpeedUnit != null) {
            if (unitData == 0) {
                tvGPSSpeedUnit.setText("KM/h");
            } else if (unitData == 1) {
                tvGPSSpeedUnit.setText("mph");
            }
        }
    }

    public void updateGotoPageButtonStatus() {
        if (mState == State.WORKSPACE) {
            Log.i(TAG, "onResume()=====mState=" + mState + ",updateGotoPageButtonStatus");
        } else if (mState == State.APPS_CUSTOMIZE) {

        }
    }

    private void initTimeReceiver() {
        IntentFilter filter = new IntentFilter();

        filter.addAction(CommonData.BROADCAST_LAMP_SWITCH);
        filter.addAction(CommonData.ACTION_ACC_ON);
        filter.addAction(CommonData.ACTION_ACC_OFF);
        filter.addAction("com.zjinnova.zlink");
        filter.addAction("android.launcher.show.allApp");
        filter.addAction(CommonData.BROADCAST_MEDIA_EXIT);
        filter.addAction("CANBUS_CHANGE_SPEED_Unit");
        filter.addAction("top_session_package_change");
        registerReceiver(mainReceiver, filter, null, null);
//        updateTime();
    }


    private BroadcastReceiver mainReceiver = new BroadcastReceiver() {
        String SYSTEM_REASON = "reason";
        String SYSTEM_HOME_KEY = "homekey";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "mainReceiver:" + action);
            if (action.equals(CommonData.BROADCAST_LAMP_SWITCH)) {
                if (intent.getIntExtra("lamplet_state", 0) == 1)
                    ivLampSwitchBg.setImageResource(R.drawable.open);
                else
                    ivLampSwitchBg.setImageResource(R.drawable.off);
            } else if (action.equals(CommonData.ACTION_ACC_ON)) {
                if (ivLampSwitchBg != null)
                    ivLampSwitchBg.postDelayed(() -> accRecor = false, 8 * 1000);
            } else if (action.equals(CommonData.ACTION_ACC_OFF)) {
                accRecor = true;
            } else if (action.equals(CommonData.BROADCAST_MEDIA_EXIT)) {
                String packge = intent.getStringExtra("package");
                if (packge != null && (packge.equals("cn.kuwo.kwmusiccar") || packge.equals("exitAll"))) {

                }
            } else if ("com.zjinnova.zlink".equals(action)) {
                String zlinStatus = intent.getStringExtra("status");
                String phoneMode = intent.getStringExtra("phoneMode");
                Log.d(TAG, "zlinStatus:" + zlinStatus);
                if (zlinStatus == null) {
                    return;
                }
                llMusic.getCarPlayData(zlinStatus, phoneMode);
            } else if (action.equals("android.launcher.show.allApp")) {
                Log.d(TAG, "mainReceiver:" + intent.getAction());
                canbus_set_dis = getCanBusState();
                showAllApps(true);
                setSettingOrAndroidPage(true);
                mModel.startLoader(true, -1);
            } else if (action.equals("CANBUS_CHANGE_SPEED_Unit")) {
                updateSpeedUnitText();
            } else if ("top_session_package_change".equals(action)) {
                String sessionTopPkg = intent.getStringExtra("top_package");
                handleMediaPlaybackResult(sessionTopPkg, "start", 3, 4);
                Log.d(TAG, "88888-top_session_package_change:" + sessionTopPkg);
            }

        }
    };

    /**
     * 鍚姩Activity
     *
     * @param packName
     * @param className
     */
    private void startActivity(String packName, String className) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packName);
        boolean isboot = true;
        if (intent != null) {
            for (int index = 0; index < IconCache.WorkSpacePackageName.length; index++) {
                Log.d(TAG, "packagename11=" + packName);
                if (!packName.equals(IconCache.WorkSpacePackageName[index])) {
                    isboot = false;
                    break;
                }
            }
            if (packName.contains("com.autonavi")) {
                if (isboot)
                    android.provider.Settings.System.putString(getContentResolver(), "boot_apk1", packName);
            } else {
                if (isboot)
                    android.provider.Settings.System.putString(getContentResolver(), "boot_apk2", packName);
            }
            startActivity(intent);
        }

    }

   /* private void initWIFIState(Intent intent) {
        int wifistate = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                WifiManager.WIFI_STATE_DISABLED);
        if (wifistate == WifiManager.WIFI_STATE_DISABLED) {
            // wifi_icon.setVisibility(View.VISIBLE);
            wifi_icon.setVisibility(View.INVISIBLE);
        } else if (wifistate == WifiManager.WIFI_STATE_ENABLED) {
            wifi_icon.setBackgroundResource(R.drawable.stat_sys_wifi_signal_0);
            // wifi_icon.setVisibility(View.VISIBLE);
            wifi_icon.setVisibility(View.INVISIBLE);
        }
    }*/


    private WIFI_AP_STATE getWifiApState() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int tmp;
        try {
            Method method = wifiManager.getClass().getMethod("getWifiApState");
            tmp = ((Integer) method.invoke(wifiManager));
            // Fix for Android 4
            if (tmp > 10) {
                tmp = tmp - 10;
            }
            return WIFI_AP_STATE.class.getEnumConstants()[tmp];
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return WIFI_AP_STATE.WIFI_AP_STATE_FAILED;
        }
    }

    public enum WIFI_AP_STATE {
        WIFI_AP_STATE_DISABLING, WIFI_AP_STATE_DISABLED, WIFI_AP_STATE_ENABLING, WIFI_AP_STATE_ENABLED, WIFI_AP_STATE_FAILED
    }

    public int getCanBusState() {
        return android.provider.Settings.System.getInt(this.getContentResolver(), "canbus_set_dis", 1);
    }
}

interface LauncherTransitionable {
    View getContent();

    void onLauncherTransitionPrepare(Launcher l, boolean animated, boolean toWorkspace);

    void onLauncherTransitionStart(Launcher l, boolean animated, boolean toWorkspace);

    void onLauncherTransitionStep(Launcher l, float t);

    void onLauncherTransitionEnd(Launcher l, boolean animated, boolean toWorkspace);
}
