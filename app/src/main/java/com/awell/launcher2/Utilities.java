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

import java.util.Random;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.util.DisplayMetrics;

import com.awell.launcher.library.R;
import com.awell.utils.LogUtil;


/**
 * Various utilities shared amongst the Launcher's classes.
 */
public final class Utilities {
    @SuppressWarnings("unused")
    static Boolean FLAG = true;
    private static final String TAG = "Launcher.Utilities";

    private static int sIconWidth = -1;
    private static int sIconHeight = -1;
    private static int sIconTextureWidth = -1;
    private static int sIconTextureHeight = -1;

    private static final Paint sBlurPaint = new Paint();
    private static final Paint sGlowColorPressedPaint = new Paint();
    private static final Paint sGlowColorFocusedPaint = new Paint();
    private static final Paint sDisabledPaint = new Paint();
    private static final Rect sOldBounds = new Rect();
    private static final Canvas sCanvas = new Canvas();

    private static int sThemeMode = 0;
    private static boolean sIconSizeSame = false;

//    private static int[] sfOtherBG = {
//            R.drawable.sf_other_app1
//            , R.drawable.sf_other_app1
//            , R.drawable.sf_other_app3
//            , R.drawable.sf_other_app4
//            , R.drawable.sf_other_app5
//    };


    static {
        sCanvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG,
                Paint.FILTER_BITMAP_FLAG));
    }

    static int sColors[] = {0xffff0000, 0xff00ff00, 0xff0000ff};
    static int sColorIndex = 0;

    /**
     * Returns a bitmap suitable for the all apps view. Used to convert pre-ICS
     * icon bitmaps that are stored in the database (which were 74x74 pixels at hdpi size)
     * to the proper size (48dp)
     */
    static Bitmap createIconBitmap(Bitmap icon, Context context, String packageName) {
        int textureWidth = sIconTextureWidth;
        int textureHeight = sIconTextureHeight;
        int sourceWidth = icon.getWidth();
        int sourceHeight = icon.getHeight();
        if (sourceWidth > textureWidth && sourceHeight > textureHeight) {
            // Icon is bigger than it should be; clip it (solves the GB->ICS migration case)
            return Bitmap.createBitmap(icon,
                    (sourceWidth - textureWidth) / 2,
                    (sourceHeight - textureHeight) / 2,
                    textureWidth, textureHeight);
        } else if (sourceWidth == textureWidth && sourceHeight == textureHeight) {
            // Icon is the right size, no need to change it
            return icon;
        } else {
            // Icon is too small, render to a larger bitmap
            final Resources resources = context.getResources();
            return createIconBitmap(new BitmapDrawable(resources, icon), context, packageName);
        }
    }

    public static void setPluginThemeMode(int themeMode,  Context context) {
        setPluginThemeMode(themeMode, false, context);
    }


    public static void setPluginThemeMode(int themeMode, boolean iconSizeSame, Context context) {
        if (sThemeMode != themeMode) {
            sThemeMode = themeMode;
            LogUtil.d("setPluginThemeMode, themeMode=" + themeMode);
        }
        if (sIconSizeSame!= iconSizeSame) {
            sIconSizeSame = iconSizeSame;
        }
    }

    private static int getIconResource(int themeMode) {
        switch (themeMode) {
            case 0:
                return R.drawable.sf_other_app3_dark;
            //case 1:
            //    return R.drawable.sf_other_app3;
            case 2:
                return R.drawable.sf_other_app3_yellow;
            case 3:
                return R.drawable.sf_other_app3_circle;
            case 4:
            case 6:
                return R.drawable.ic_other_app3;
            case 5:
                return R.drawable.zy2_other_app3;
            case 100:
                return R.drawable.sf_other_yfd_2;
            case 0xff://插件包
                // 从 Plugin 获取默认背景资源 ID
                return PluginIconManager.getPluginDefaultBackgroundResId();
            default:
                return R.drawable.sf_other_app3;
        }
    }

    /**
     * Returns a bitmap suitable for the all apps view.
     */
    static Bitmap createIconBitmap(Drawable icon, Context context, String packageName) {

        int sourceWidth = icon.getIntrinsicWidth();
        int sourceHeight = icon.getIntrinsicHeight();

        synchronized (sCanvas) { // we share the statics :-(
            if (sIconWidth == -1) {
                initStatics(context);
            }

            for (int i = 0; i < IconCache.mHomePackageName_lehang.length; i++) {
                synchronized (packageName) {
                    if (packageName.equals(IconCache.mHomePackageName_lehang[i])) {
                        //BitmapDrawable bd = (BitmapDrawable) icon;
                        FLAG = false;
//    						return bd.getBitmap();
                        return drawableToBitmap(icon, sIconTextureWidth, sIconTextureHeight);
                    }
                }
            }


            int width = sIconWidth / 3;
            int height = sIconHeight / 3;

            if (icon instanceof PaintDrawable) {
                PaintDrawable painter = (PaintDrawable) icon;
                painter.setIntrinsicWidth(width);
                painter.setIntrinsicHeight(height);
            } else if (icon instanceof BitmapDrawable) {
                // Ensure the bitmap has a density.
                BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
                Bitmap bitmap = bitmapDrawable.getBitmap();
                if (bitmap.getDensity() == Bitmap.DENSITY_NONE) {
                    bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
                }
            }
           /* int sourceWidth = icon.getIntrinsicWidth();
            int sourceHeight = icon.getIntrinsicHeight();*/
           /* if (sourceWidth > 0 && sourceHeight > 0) {
                // There are intrinsic sizes.
                if (width < sourceWidth || height < sourceHeight) {
                    // It's too big, scale it down.
                    final float ratio = (float) sourceWidth / sourceHeight;
                    if (sourceWidth > sourceHeight) {
                        height = (int) (width / ratio);
                    } else if (sourceHeight > sourceWidth) {
                        width = (int) (height * ratio);
                    }
                } else if (sourceWidth < width && sourceHeight < height) {
                    // Don't scale up the icon
                    width = sourceWidth;
                    height = sourceHeight;
                }
            }*/

            // no intrinsic size --> use default size
            int textureWidth = sIconTextureWidth;
            int textureHeight = sIconTextureHeight;
            //LogUtil.i("sIconSizeSame=" + sIconSizeSame);
            if (!sIconSizeSame) {
                width = sIconTextureWidth / 3 * 2;
                height = sIconTextureHeight / 3 * 2;
            }else{
                width = sIconWidth;
                height = sIconHeight;
            }

            final Bitmap bitmap = Bitmap.createBitmap(textureWidth, textureHeight,
                    Bitmap.Config.ARGB_8888);
            final Canvas canvas = sCanvas;
            canvas.setBitmap(bitmap);
            int left = 0;
            int top = 0;
            if (!sIconSizeSame) {//不同图标大小,居中绘制
                left = (textureWidth - width) / 2;
                top = (textureHeight - height) / 2;
            }
//            final int left = (textureWidth - width) / 2;
//            final int top = (textureHeight - height) / 2;

            @SuppressWarnings("all") // suppress dead code warning
            final boolean debug = false;
            if (debug) {
                // draw a big box for the icon for debugging
                canvas.drawColor(sColors[sColorIndex]);
                if (++sColorIndex >= sColors.length) sColorIndex = 0;
                Paint debugPaint = new Paint();
                debugPaint.setColor(0xffcccc00);
                canvas.drawRect(left, top, left + width, top + height, debugPaint);
            }


            //增加图标背景图片 OWL - 仅在需要时加载当前主题的图片
            if (!sIconSizeSame) { //不同图标大小,绘制背景,

                int iconResId = getIconResource(sThemeMode);
                //LogUtil.i("iconResId=" + iconResId  + " sThemeMode=" + sThemeMode);
                if (iconResId != 0) {
                    Bitmap backBitmap=null;
                    
                    // 判断是否为 Plugin 资源（themeMode=0xff）
                    if (sThemeMode == 0xff) {
                        // Plugin 模式：使用插件的 Resources 加载
                        String pluginPkg = PluginIconManager.getPluginPackageName();
                        android.content.Context pluginContext = com.qihoo360.replugin.RePlugin.fetchContext(pluginPkg);
                        if (pluginContext != null) {
                            backBitmap = BitmapFactory.decodeResource(pluginContext.getResources(), iconResId);
                        }
                    } else {
                        // 普通模式：使用主应用的 Resources 加载
                        backBitmap = BitmapFactory.decodeResource(context.getResources(), iconResId);
                    }
                    
                    if (backBitmap != null) {
                        int backWidth = backBitmap.getWidth();
                        int backHeight = backBitmap.getHeight();
                        if (backWidth != sIconWidth || backHeight != sIconHeight) {
                            Matrix matrix = new Matrix();
                            matrix.postScale((float) sIconWidth / backWidth, (float) sIconHeight / backHeight);
                            canvas.drawBitmap(Bitmap.createBitmap(backBitmap, 0, 0, backWidth, backHeight, matrix, true),
                                    0.0f, 0.0f, null);
                        } else {
                            canvas.drawBitmap(backBitmap, 0.0f, 0.0f, null);
                        }
                        // 及时回收不需要的 bitmap 内存
                        if (!backBitmap.isRecycled()) {
                            backBitmap.recycle();
                        }
                    }
                }
            }

            sOldBounds.set(icon.getBounds());
            icon.setBounds(left, top, left + width, top + height);
            icon.draw(canvas);
            icon.setBounds(sOldBounds);
            canvas.setBitmap(null);

            return bitmap;
        }
    }

    static void drawSelectedAllAppsBitmap(Canvas dest, int destWidth, int destHeight,
                                          boolean pressed, Bitmap src) {
        synchronized (sCanvas) { // we share the statics :-(
            if (sIconWidth == -1) {
                // We can't have gotten to here without src being initialized, which
                // comes from this file already.  So just assert.
                //initStatics(context);
                throw new RuntimeException("Assertion failed: Utilities not initialized");
            }

            dest.drawColor(0, PorterDuff.Mode.CLEAR);

            int[] xy = new int[2];
            Bitmap mask = src.extractAlpha(sBlurPaint, xy);

            float px = (destWidth - src.getWidth()) / 2;
            float py = (destHeight - src.getHeight()) / 2;
            dest.drawBitmap(mask, px + xy[0], py + xy[1],
                    pressed ? sGlowColorPressedPaint : sGlowColorFocusedPaint);

            mask.recycle();
        }
    }

    /**
     * Returns a Bitmap representing the thumbnail of the specified Bitmap.
     * The size of the thumbnail is defined by the dimension
     * android.R.dimen.launcher_application_icon_size.
     *
     * @param bitmap  The bitmap to get a thumbnail of.
     * @param context The application's context.
     * @return A thumbnail for the specified bitmap or the bitmap itself if the
     * thumbnail could not be created.
     */
    static Bitmap resampleIconBitmap(Bitmap bitmap, Context context) {
        synchronized (sCanvas) { // we share the statics :-(
            if (sIconWidth == -1) {
                initStatics(context);
            }

            if (bitmap.getWidth() == sIconWidth && bitmap.getHeight() == sIconHeight) {
                return bitmap;
            } else {
                final Resources resources = context.getResources();
                return createIconBitmap(new BitmapDrawable(resources, bitmap), context, "");
            }
        }
    }

    static Bitmap drawDisabledBitmap(Bitmap bitmap, Context context) {
        synchronized (sCanvas) { // we share the statics :-(
            if (sIconWidth == -1) {
                initStatics(context);
            }
            final Bitmap disabled = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                    Bitmap.Config.ARGB_8888);
            final Canvas canvas = sCanvas;
            canvas.setBitmap(disabled);

            canvas.drawBitmap(bitmap, 0.0f, 0.0f, sDisabledPaint);

            canvas.setBitmap(null);

            return disabled;
        }
    }

    private static void initStatics(Context context) {
        final Resources resources = context.getResources();
        final DisplayMetrics metrics = resources.getDisplayMetrics();
        final float density = metrics.density;

        sIconWidth = sIconHeight = (int) resources.getDimension(R.dimen.app_icon_size);
        sIconTextureWidth = sIconTextureHeight = sIconWidth;

        sBlurPaint.setMaskFilter(new BlurMaskFilter(5 * density, BlurMaskFilter.Blur.NORMAL));
        sGlowColorPressedPaint.setColor(0xffffc300);
        sGlowColorFocusedPaint.setColor(0xffff8e00);

        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0.2f);
        sDisabledPaint.setColorFilter(new ColorMatrixColorFilter(cm));
        sDisabledPaint.setAlpha(0x88);
    }

    /**
     * Only works for positive numbers.
     */
    static int roundToPow2(int n) {
        int orig = n;
        n >>= 1;
        int mask = 0x8000000;
        while (mask != 0 && (n & mask) == 0) {
            mask >>= 1;
        }
        while (mask != 0) {
            n |= mask;
            mask >>= 1;
        }
        n += 1;
        if (n != orig) {
            n <<= 1;
        }
        return n;
    }

    static int generateRandomId() {
        return new Random(System.currentTimeMillis()).nextInt(1 << 24);
    }

    public static Bitmap drawableToBitmap(Drawable drawable, int Width, int Height) {

        Bitmap bitmap = Bitmap.createBitmap(Width, Height,
                drawable.getOpacity() != PixelFormat.OPAQUE ? android.graphics.Bitmap.Config.ARGB_8888
                        : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        //canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, Width, Height);
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * created 20210708
     *
     * @param src
     * @param dests
     * @return
     */
    static public int isContains(String src, String[] dests) {
        for (int i = 0; i < dests.length; i++) {
            if (dests[i].equals(src)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean issIconSizeSame() {
        return sIconSizeSame;
    }

    public static void setsIconSizeSame(boolean sIconSizeSame) {
        Utilities.sIconSizeSame = sIconSizeSame;
    }
}
