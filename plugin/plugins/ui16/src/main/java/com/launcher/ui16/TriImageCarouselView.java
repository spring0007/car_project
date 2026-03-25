package com.launcher.ui16;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TriImageCarouselView extends View {
    // 图片数据
    private List<Bitmap> bitmaps = new ArrayList<>();
    private int currentIndex = 0;

    // 尺寸(px) - 从XML读取后存储为px值
    private int sideSize;       // 侧边图片尺寸(px)
    private int centerSize;     // 中心图片尺寸(px)
    private int spacing;        // 侧边与中心的间距(px)
    private int cornerRadius;   // 圆角半径(px)

    // 绘制区域
    private Rect leftRect;
    private Rect centerRect;
    private Rect rightRect;

    // 触摸交互
    private float downX;
    private float currentOffsetX = 0;
    private boolean isDragging = false;
    private VelocityTracker velocityTracker;
    private ValueAnimator snapAnimator;

    // 滑动缩放效果
    private float centerScale = 1.0f;
    private static final float MAX_SCALE_DOWN = 0.7f;
    private static final float SWIPE_THRESHOLD = 0.3f;
    private int swipeThresholdPx;

    // 过渡动画相关
    private boolean isAnimating = false;
    private float transitionProgress = 0f;
    private int[] startIndices = new int[3];
    private int[] endIndices = new int[3];
    private Rect[] startRects = new Rect[3];
    private Rect[] endRects = new Rect[3];

    // 倒影相关
    private Bitmap reflectionBitmap;
    private Paint reflectionPaint;

    public TriImageCarouselView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        reflectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        reflectionPaint.setFilterBitmap(true);

        // 读取XML属性
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.TriImageCarouselView);
        sideSize = ta.getDimensionPixelSize(R.styleable.TriImageCarouselView_sideSize, dpToPx(169));
        centerSize = ta.getDimensionPixelSize(R.styleable.TriImageCarouselView_centerSize, dpToPx(190));
        spacing = ta.getDimensionPixelSize(R.styleable.TriImageCarouselView_spacing, dpToPx(20));
        cornerRadius = ta.getDimensionPixelSize(R.styleable.TriImageCarouselView_cornerRadius, dpToPx(20));
        ta.recycle();

        // 确保尺寸为正
        if (sideSize <= 0) sideSize = dpToPx(169);
        if (centerSize <= 0) centerSize = dpToPx(190);
        if (spacing <= 0) spacing = dpToPx(20);
        if (cornerRadius <= 0) cornerRadius = dpToPx(20);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 内容宽度 = 左右两张图片的最外侧距离
        int contentWidth = 2 * (sideSize + spacing);
        // 内容高度 = 中心图片高度 + 倒影高度（图片高度的1/3）
        int contentHeight = centerSize + centerSize / 3;
        int finalWidth = resolveSize(contentWidth, widthMeasureSpec);
        int finalHeight = resolveSize(contentHeight, heightMeasureSpec);
        setMeasuredDimension(finalWidth, finalHeight);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculateRects();
        swipeThresholdPx = (int) (getWidth() * SWIPE_THRESHOLD);
    }

    private void calculateRects() {
        int centerX = getWidth() / 2;
        int centerY = centerSize / 2;   // 让中心图片顶部对齐，为倒影留出空间

        // 中心区域
        int left = centerX - centerSize / 2;
        int top = centerY - centerSize / 2;
        centerRect = new Rect(left, top, left + centerSize, top + centerSize);

        // 左边区域
        int leftLeft = centerX - sideSize - spacing;
        int leftTop = centerY - sideSize / 2;
        leftRect = new Rect(leftLeft, leftTop, leftLeft + sideSize, leftTop + sideSize);

        // 右边区域
        int rightLeft = centerX + spacing;
        int rightTop = centerY - sideSize / 2;
        rightRect = new Rect(rightLeft, rightTop, rightLeft + sideSize, rightTop + sideSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bitmaps.isEmpty()) return;
        canvas.save();

        if (isAnimating) {
            drawTransition(canvas);
        } else {
            drawNormal(canvas);
        }

        canvas.restore();
    }

    // 正常绘制
    private void drawNormal(Canvas canvas) {
        // 左右图片
        int leftIndex = (currentIndex - 1 + bitmaps.size()) % bitmaps.size();
        drawRoundBitmap(canvas, bitmaps.get(leftIndex), leftRect);
        int rightIndex = (currentIndex + 1) % bitmaps.size();
        drawRoundBitmap(canvas, bitmaps.get(rightIndex), rightRect);

        // 中心图片（带滑动偏移和缩放）
        Bitmap centerBitmap = bitmaps.get(currentIndex);
        if (centerBitmap == null) return;
        float dx = currentOffsetX;
        float scale = centerScale;
        int scaledWidth = (int) (centerSize * scale);
        int scaledHeight = (int) (centerSize * scale);
        int left = centerRect.left + (int) dx - (scaledWidth - centerSize) / 2;
        int top = centerRect.top - (scaledHeight - centerSize) / 2;
        Rect destRect = new Rect(left, top, left + scaledWidth, top + scaledHeight);
        drawRoundBitmap(canvas, centerBitmap, destRect);

        // 倒影
        drawReflection(canvas, destRect);
    }

    // 过渡动画绘制
    private void drawTransition(Canvas canvas) {
        float progress = transitionProgress;
        for (int i = 0; i < 3; i++) {
            int idx = startIndices[i];
            if (idx < 0 || idx >= bitmaps.size()) continue;
            Bitmap bmp = bitmaps.get(idx);
            if (bmp == null) continue;

            Rect startRect = startRects[i];
            Rect endRect = endRects[i];
            Rect currentRect = interpolateRect(startRect, endRect, progress);
            drawRoundBitmap(canvas, bmp, currentRect);
        }
    }

    // 绘制圆角图片
    private void drawRoundBitmap(Canvas canvas, Bitmap bitmap, Rect rect) {
        if (bitmap == null || rect == null) return;
        canvas.save();
        Path path = new Path();
        path.addRoundRect(new RectF(rect), cornerRadius, cornerRadius, Path.Direction.CW);
        canvas.clipPath(path);
        canvas.drawBitmap(bitmap, null, rect, null);
        canvas.restore();
    }

    // 矩形插值
    private Rect interpolateRect(Rect start, Rect end, float fraction) {
        int left = (int) (start.left + (end.left - start.left) * fraction);
        int top = (int) (start.top + (end.top - start.top) * fraction);
        int right = (int) (start.right + (end.right - start.right) * fraction);
        int bottom = (int) (start.bottom + (end.bottom - start.bottom) * fraction);
        return new Rect(left, top, right, bottom);
    }

    // 倒影绘制
    private void drawReflection(Canvas canvas, Rect centerDestRect) {
        if (reflectionBitmap == null || reflectionBitmap.isRecycled() || centerDestRect == null) return;
        int reflectHeight = centerDestRect.height() / 3;
        if (reflectHeight <= 0) return;
        int left = centerDestRect.left;
        int top = centerDestRect.bottom;
        int width = centerDestRect.width();
        Rect destRect = new Rect(left, top, left + width, top + reflectHeight);
        drawRoundBitmap(canvas, reflectionBitmap, destRect);
    }

    // 生成半透明渐变倒影
    private void generateReflection(Bitmap sourceBitmap) {
        if (sourceBitmap == null) return;
        int reflectHeight = sourceBitmap.getHeight() / 3;
        if (reflectHeight <= 0) return;

        Bitmap reflection = Bitmap.createBitmap(sourceBitmap.getWidth(), reflectHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(reflection);

        // 垂直翻转镜像
        Matrix matrix = new Matrix();
        matrix.setScale(1, -1);
        matrix.postTranslate(0, reflectHeight);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawBitmap(sourceBitmap, matrix, paint);

        // 垂直渐变（从靠近原图的不透明到底部完全透明）
        LinearGradient gradient = new LinearGradient(0, 0, 0, reflectHeight,
                new int[]{0xCC000000, 0x00000000},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP);
        Paint gradientPaint = new Paint();
        gradientPaint.setShader(gradient);
        gradientPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawRect(0, 0, reflection.getWidth(), reflection.getHeight(), gradientPaint);

        if (reflectionBitmap != null && !reflectionBitmap.isRecycled()) {
            reflectionBitmap.recycle();
        }
        reflectionBitmap = reflection;
    }

    private void updateReflection() {
        if (bitmaps.isEmpty()) return;
        Bitmap centerBitmap = bitmaps.get(currentIndex);
        if (centerBitmap != null) generateReflection(centerBitmap);
    }

    // ---------- 触摸事件 ----------
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isAnimating) return true;
        if (velocityTracker == null) velocityTracker = VelocityTracker.obtain();
        velocityTracker.addMovement(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (snapAnimator != null && snapAnimator.isRunning()) snapAnimator.cancel();
                downX = event.getX();
                isDragging = true;
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getX() - downX;
                currentOffsetX = deltaX;
                float progress = Math.min(1.0f, Math.abs(currentOffsetX) / (float) swipeThresholdPx);
                centerScale = 1.0f - progress * (1 - MAX_SCALE_DOWN);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                isDragging = false;
                velocityTracker.computeCurrentVelocity(1000);
                float velocityX = velocityTracker.getXVelocity();
                boolean shouldSwitch = Math.abs(currentOffsetX) > swipeThresholdPx || Math.abs(velocityX) > 1000;
                if (shouldSwitch) {
                    if (currentOffsetX > 0) {
                        previous();
                    } else {
                        next();
                    }
                } else {
                    startSnapAnimation(0);
                }
                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                break;
        }
        return true;
    }

    private void startSnapAnimation(float targetOffset) {
        ValueAnimator animator = ValueAnimator.ofFloat(currentOffsetX, targetOffset);
        animator.setDuration(250);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            currentOffsetX = (float) animation.getAnimatedValue();
            centerScale = 1.0f - Math.min(1.0f, Math.abs(currentOffsetX) / swipeThresholdPx) * (1 - MAX_SCALE_DOWN);
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentOffsetX = 0;
                centerScale = 1.0f;
                invalidate();
            }
        });
        animator.start();
        snapAnimator = animator;
    }

    // ---------- 公开切换接口 ----------
    public void next() {
        if (isAnimating || bitmaps.isEmpty()) return;
        int newIndex = (currentIndex + 1) % bitmaps.size();
        if (newIndex == currentIndex) return;
        performSwitch(newIndex, true);
    }

    public void previous() {
        if (isAnimating || bitmaps.isEmpty()) return;
        int newIndex = (currentIndex - 1 + bitmaps.size()) % bitmaps.size();
        if (newIndex == currentIndex) return;
        performSwitch(newIndex, false);
    }

    private void performSwitch(int newIndex, boolean directionToRight) {
        // 起始三张图片索引（左、中、右）
        int leftIdx = (currentIndex - 1 + bitmaps.size()) % bitmaps.size();
        int centerIdx = currentIndex;
        int rightIdx = (currentIndex + 1) % bitmaps.size();

        // 结束三张图片索引
        int endLeftIdx, endCenterIdx, endRightIdx;
        if (directionToRight) { // 下一张：整体向左滚动
            endLeftIdx = centerIdx;
            endCenterIdx = rightIdx;
            endRightIdx = (rightIdx + 1) % bitmaps.size();
        } else { // 上一张：整体向右滚动
            endLeftIdx = (leftIdx - 1 + bitmaps.size()) % bitmaps.size();
            endCenterIdx = leftIdx;
            endRightIdx = centerIdx;
        }

        startIndices = new int[]{leftIdx, centerIdx, rightIdx};
        endIndices = new int[]{endLeftIdx, endCenterIdx, endRightIdx};
        startRects = new Rect[]{leftRect, centerRect, rightRect};
        endRects = new Rect[3];
        for (int i = 0; i < 3; i++) {
            if (i == 0) endRects[i] = leftRect;
            else if (i == 1) endRects[i] = centerRect;
            else endRects[i] = rightRect;
        }

        currentOffsetX = 0;
        centerScale = 1.0f;
        isAnimating = true;
        transitionProgress = 0f;

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            transitionProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimating = false;
                currentIndex = newIndex;
                updateReflection();
                invalidate();
            }
        });
        animator.start();
        snapAnimator = animator;
    }

    // 设置图片列表，并自动缩放至中心尺寸
    public void setBitmaps(List<Bitmap> bitmaps) {
        if (bitmaps == null) return;
        this.bitmaps.clear();
        for (Bitmap bmp : bitmaps) {
            Bitmap scaled = Bitmap.createScaledBitmap(bmp, centerSize, centerSize, true);
            this.bitmaps.add(scaled);
        }
        if (!this.bitmaps.isEmpty()) {
            currentIndex = 0;
            updateReflection();
            requestLayout();
            invalidate();
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (reflectionBitmap != null && !reflectionBitmap.isRecycled()) {
            reflectionBitmap.recycle();
        }
    }
}