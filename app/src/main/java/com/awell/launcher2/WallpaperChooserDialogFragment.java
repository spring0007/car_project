/*
 * Copyright (C) 2010 The Android Open Source Project
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

import static com.awell.utils.Utils.getPackageInfo;
import static com.awell.utils.Utils.getPluginApkFilePath;
import static com.awell.utils.Utils.getPluginWallPaperID;
import static com.awell.utils.Utils.getPluginResources;
import static com.awell.utils.Utils.isHexStartWith7e;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SpinnerAdapter;

import com.awell.launcher.library.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class WallpaperChooserDialogFragment extends DialogFragment implements
        AdapterView.OnItemSelectedListener, AdapterView.OnItemClickListener {

    private static final String TAG = "Launcher.WallpaperChooserDialogFragment";
    private static final String EMBEDDED_KEY = "com.awell.launcher2."
            + "WallpaperChooserDialogFragment.EMBEDDED_KEY";

    private boolean mEmbedded;
    private Bitmap mBitmap = null;

    private ArrayList<Integer> mThumbs;
    private ArrayList<Integer> mImages;
    private WallpaperLoader mLoader;
    private WallpaperDrawable mWallpaperDrawable = new WallpaperDrawable();
    private Bitmap tmpBitmap;

    private ImageAdapter adapter = null;

    public static WallpaperChooserDialogFragment newInstance() {
        WallpaperChooserDialogFragment fragment = new WallpaperChooserDialogFragment();
        fragment.setCancelable(true);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null
                && savedInstanceState.containsKey(EMBEDDED_KEY)) {
            mEmbedded = savedInstanceState.getBoolean(EMBEDDED_KEY);
        } else {
            mEmbedded = isInLayout();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(EMBEDDED_KEY, mEmbedded);
    }

    private void cancelLoader() {
        if (mLoader != null
                && mLoader.getStatus() != WallpaperLoader.Status.FINISHED) {
            mLoader.cancel(true);
            mLoader = null;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        cancelLoader();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        cancelLoader();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        /*
         * On orientation changes, the dialog is effectively "dismissed" so this
         * is called when the activity is no longer associated with this dying
         * dialog fragment. We should just safely ignore this case by checking
         * if getActivity() returns null
         */
        Activity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }
    }

    /*
     * This will only be called when in XLarge mode, since this Fragment is
     * invoked like a dialog in that mode
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mThumbs = new ArrayList<Integer>(24);
        mImages = new ArrayList<Integer>(24);
        //findWallpapers();

        return null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mThumbs = new ArrayList<Integer>(24);
        mImages = new ArrayList<Integer>(24);
        //findWallpapers();

        /*
         * If this fragment is embedded in the layout of this activity, then we
         * should generate a view to display. Otherwise, a dialog will be
         * created in onCreateDialog()
         */
        if (mEmbedded) {
            View view = inflater.inflate(R.layout.wallpaper_chooser, container,
                    false);
            view.setBackground(mWallpaperDrawable);

            final Gallery gallery = (Gallery) view.findViewById(R.id.gallery);
            gallery.setCallbackDuringFling(false);
            gallery.setOnItemSelectedListener(this);
            adapter = new ImageAdapter(getActivity());
            gallery.setAdapter(adapter);

            View setButton = view.findViewById(R.id.set);
            setButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectWallpaper(gallery.getSelectedItemPosition());
                }
            });
            return view;
        }
        return null;
    }

    private void selectWallpaper(int position) {
        try {
            Log.i(TAG, "==========setWallpaper=============");
            WallpaperManager wpm = (WallpaperManager) getActivity()
                    .getSystemService(Context.WALLPAPER_SERVICE);
            if (mImages.size() == 0) {
                getActivity().finish();
                return;
            }
//			wpm.setResource(mImages.get(position));

            // 获取屏幕分辨率宽度
            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
            wm.getDefaultDisplay().getRealMetrics(metrics);
            int mScreenWidth = metrics.widthPixels;
            int mScreenHeight = metrics.heightPixels;

            //根据分辨率，设置壁纸大小
            Bitmap bitmap = Bitmap.createBitmap(mScreenWidth, mScreenHeight, Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Bitmap parper = null;
            int resourcesId = mImages.get(position);
            if (isHexStartWith7e(resourcesId)) {
                //插件
                parper = BitmapFactory.decodeResource(getPluginResources(new File(getPluginApkFilePath())), resourcesId);

            } else {
                parper = BitmapFactory.decodeResource(getResources(), resourcesId);
            }


            canvas.drawBitmap(parper, 88, 100, null);
            wpm.suggestDesiredDimensions(mScreenWidth, mScreenHeight);
            wpm.setBitmap(mBitmap);

            Activity activity = getActivity();
            activity.setResult(Activity.RESULT_OK);
            activity.finish();
        } catch (IOException e) {
            Log.e(TAG, "Failed to set wallpaper: " + e);
        }
    }

    // Click handler for the Dialog's GridView
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        selectWallpaper(position);
    }

    // Selection handler for the embedded Gallery view
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position,
                               long id) {
        if (mLoader != null
                && mLoader.getStatus() != WallpaperLoader.Status.FINISHED) {
            mLoader.cancel();
        }
        mLoader = (WallpaperLoader) new WallpaperLoader().execute(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    public void findWallpapers() {

        final Resources resources = getResources();
        // Context.getPackageName() may return the "original" package name,
        // com.awell.launcher2; Resources needs the real package name,
        // com.awell.launcher. So we ask Resources for what it thinks the
        // package name should be.
        final String packageName = resources
                .getResourcePackageName(R.array.wallpapers);

        addWallpapers(resources, packageName, R.array.wallpapers);
        addWallpapers(resources, packageName, R.array.extra_wallpapers);


    }

    /**
     * 加载插件中的壁纸
     */
    public void loadPluginApkWallpaper() {
        if (getPluginApkFilePath() != null) {
            File file = new File(getPluginApkFilePath());
            if (file.exists() && getPackageInfo(file) != null) {
                addWallpapers(Objects.requireNonNull(getPluginResources(file)),
                        Objects.requireNonNull(getPackageInfo(file)).packageName,
                        getPluginWallPaperID(getPluginApkFilePath()));
            }
        }
    }

    private void addWallpapers(Resources resources, String packageName, int list) {
        final String[] extras = resources.getStringArray(list);
        for (String extra : extras) {
            int res = resources.getIdentifier(extra, "drawable", packageName);
            if (res != 0) {
                final int thumbRes = resources.getIdentifier(extra + "_small",
                        "drawable", packageName);
                if (thumbRes != 0) {
                    mThumbs.add(thumbRes);
                    mImages.add(res);
                    // Log.d(TAG, "add: [" + packageName + "]: " + extra + " ("
                    // + res + ")");
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private class ImageAdapter extends BaseAdapter implements ListAdapter,
            SpinnerAdapter {
        private LayoutInflater mLayoutInflater;

        ImageAdapter(Activity activity) {
            mLayoutInflater = activity.getLayoutInflater();
        }

        public int getCount() {
            return mThumbs.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View view;

            if (convertView == null) {
                view = mLayoutInflater.inflate(R.layout.wallpaper_item, parent,
                        false);
            } else {
                view = convertView;
            }

            ImageView image = (ImageView) view
                    .findViewById(R.id.wallpaper_image);
            Drawable thumbDrawable = null;
            int thumbRes = mThumbs.get(position);
            if (isHexStartWith7e(thumbRes)) {
                //插件
                Bitmap paper = BitmapFactory.decodeResource(getPluginResources(new File(getPluginApkFilePath())), thumbRes);
                image.setImageBitmap(paper);
            } else {
                image.setImageResource(thumbRes);
            }
            thumbDrawable = image.getDrawable();

            if (thumbDrawable != null) {
                thumbDrawable.setDither(true);
            } else {
                Log.e(TAG, "Error decoding thumbnail resId=" + thumbRes
                        + " for wallpaper #" + position);
            }

            return view;
        }
    }

    class WallpaperLoader extends AsyncTask<Integer, Void, Bitmap> {
        BitmapFactory.Options mOptions;

        WallpaperLoader() {
            mOptions = new BitmapFactory.Options();
            mOptions.inDither = false;
            mOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        }

        @Override
        protected Bitmap doInBackground(Integer... params) {
            if (isCancelled())
                return null;
            try {

                Bitmap bitmap = null;
                int resourcesId = mImages.get(params[0]);
                if (isHexStartWith7e(resourcesId)) {
                    //插件
                    bitmap = BitmapFactory.decodeResource(getPluginResources(new File(getPluginApkFilePath())), resourcesId, mOptions);
                } else {
                    bitmap = BitmapFactory.decodeResource(getResources(), resourcesId, mOptions);
                }

                return bitmap;
            } catch (OutOfMemoryError e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap b) {
            if (b == null)
                return;

            if (!isCancelled() && !mOptions.mCancel) {
                // Help the GC
                if (mBitmap != null) {
                    mBitmap.recycle();
                }

                View v = getView();
                if (v != null) {
                    mBitmap = b;
                    Log.i(TAG, "b.getWidth()=" + b.getWidth()
                            + ",b.getHeight()=" + b.getHeight());
                    mWallpaperDrawable.setBitmap(b);
                    v.postInvalidate();
                } else {
                    mBitmap = null;
                    mWallpaperDrawable.setBitmap(null);
                }
                mLoader = null;
            } else {
                b.recycle();
            }
        }

        void cancel() {
            mOptions.requestCancelDecode();
            super.cancel(true);
        }
    }

    /**
     * Custom drawable that centers the bitmap fed to it.
     */
    static class WallpaperDrawable extends Drawable {

        Bitmap mBitmap;
        int mIntrinsicWidth;
        int mIntrinsicHeight;

        /* package */void setBitmap(Bitmap bitmap) {
            mBitmap = bitmap;
            if (mBitmap == null)
                return;
            mIntrinsicWidth = mBitmap.getWidth();
            mIntrinsicHeight = mBitmap.getHeight();
        }

        @Override
        public void draw(Canvas canvas) {
            if (mBitmap == null)
                return;
            int width = canvas.getWidth();
            int height = canvas.getHeight();
            // int width = 1024;
            // int height = 600;

            // / M: scale up the bitmap to make it cover the entire area
            float scalew = width / (float) mIntrinsicWidth;
            float scaleh = height / (float) mIntrinsicHeight;
            Log.e("@@@", "mBitmeap,x=" + mIntrinsicWidth + "        ,y="
                    + mIntrinsicHeight + "        ,mx=" + width
                    + "        ,my=" + height);

            if (scalew > 1.0 || scaleh > 1.0) {
                float scale = scalew > scaleh ? scalew : scaleh;
                int scaledWidth = (int) (mIntrinsicWidth * scale);
                int scaledHeight = (int) (mIntrinsicHeight * scale);
                int x = (width - scaledWidth) / 2;
                int y = (height - scaledHeight) / 2;
                // int x = 0;
                // int y = 0;

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(mBitmap,
                        scaledWidth, scaledHeight, true);
                // Bitmap scaledBitmap = Bitmap.createScaledBitmap(mBitmap,
                // 1024, 600, true);
                canvas.setDrawFilter(new PaintFlagsDrawFilter(0,
                        Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
                canvas.drawBitmap(scaledBitmap, x, y, null);
                scaledBitmap.recycle();
                scaledBitmap = null;
            } else {
                int x = (width - mIntrinsicWidth) / 2;
                int y = (height - mIntrinsicHeight) / 2;
                Log.i(TAG, "mBitmeap,x=" + x + ",y=" + y + ",width=" + width
                        + ",height=" + height + ",mIntrinsicWidth="
                        + mIntrinsicWidth + ",mIntrinsicHeight="
                        + mIntrinsicHeight);
                canvas.drawBitmap(mBitmap, x, y, null);

            }
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.OPAQUE;
        }

        @Override
        public void setAlpha(int alpha) {
            // Ignore
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            // Ignore
        }
    }
}
