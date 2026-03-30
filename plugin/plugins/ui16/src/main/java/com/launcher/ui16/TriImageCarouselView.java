package com.launcher.ui16;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
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

    // 尺寸(px)
    private int sideSize;
    private int centerSize;
    private int spacing;
    private int cornerRadius;

    // 绘制区域（固定位置）
    private Rect leftRect = new Rect();
    private Rect centerRect = new Rect();
    private Rect rightRect = new Rect();

    // 触摸交互
    private float downX;
    private float currentOffsetX = 0;
    private boolean isDragging = false;
    private VelocityTracker velocityTracker;
    private ValueAnimator snapAnimator;

    // 滑动缩放效果
    private float centerScale = 1.0f;
    private float leftScale = 1.0f;  // 左侧图片缩放
    private float rightScale = 1.0f; // 右侧图片缩放
    private static final float MAX_SCALE_DOWN = 0.7f;
    private static final float SWIPE_THRESHOLD = 0.3f;
    private int swipeThresholdPx;

    // 过渡动画相关
    private boolean isAnimating = false;
    private float transitionProgress = 0f;
    private int[] startIndices = new int[3];
    private int[] endIndices = new int[3];
    private Rect[] startRects = new Rect[3];  // 复用对象
    private Rect[] endRects = new Rect[3];
    private Rect[] currentRects = new Rect[3]; // 用于动画中临时绘制
    private boolean isNextAnimation = false; // 记录动画方向

    // 倒影相关
    private Bitmap reflectionBitmap;
    private Paint reflectionPaint;

    // 用于绘制圆角的Path复用对象
    private Path roundRectPath = new Path();

    public TriImageCarouselView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
        // 初始化数组中的Rect对象，避免每帧创建
        for (int i = 0; i < 3; i++) {
            startRects[i] = new Rect();
            endRects[i] = new Rect();
            currentRects[i] = new Rect();
        }
    }

    private void init(Context context, AttributeSet attrs) {
        reflectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        reflectionPaint.setFilterBitmap(true);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.TriImageCarouselView);
        sideSize = ta.getDimensionPixelSize(R.styleable.TriImageCarouselView_sideSize, dpToPx(169));
        centerSize = ta.getDimensionPixelSize(R.styleable.TriImageCarouselView_centerSize, dpToPx(190));
        spacing = ta.getDimensionPixelSize(R.styleable.TriImageCarouselView_spacing, dpToPx(20));
        cornerRadius = ta.getDimensionPixelSize(R.styleable.TriImageCarouselView_cornerRadius, dpToPx(20));
        ta.recycle();

        if (sideSize <= 0) sideSize = dpToPx(169);
        if (centerSize <= 0) centerSize = dpToPx(190);
        if (spacing <= 0) spacing = dpToPx(20);
        if (cornerRadius <= 0) cornerRadius = dpToPx(20);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int contentWidth = 2 * (sideSize + spacing);
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
        int centerY = centerSize / 2;

        // 中心区域
        int left = centerX - centerSize / 2;
        int top = centerY - centerSize / 2;
        centerRect.set(left, top, left + centerSize, top + centerSize);

        // 左边区域
        int leftLeft = centerX - sideSize - spacing;
        int leftTop = centerY - sideSize / 2;
        leftRect.set(leftLeft, leftTop, leftLeft + sideSize, leftTop + sideSize);

        // 右边区域
        int rightLeft = centerX + spacing;
        int rightTop = centerY - sideSize / 2;
        rightRect.set(rightLeft, rightTop, rightLeft + sideSize, rightTop + sideSize);
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

    private void drawNormal(Canvas canvas) {
        // 左右图片（带缩放和位移效果）
        int leftIndex = (currentIndex - 1 + bitmaps.size()) % bitmaps.size();
        int rightIndex = (currentIndex + 1) % bitmaps.size();
        
        // 计算左侧图片的位置和缩放
        // 向右滑动时，左侧图片向右移动（向中间靠拢）；向左滑动时，左侧图片向左移动（远离中间）
        float leftDx = currentOffsetX * 0.5f;
        int leftScaledWidth = (int) (sideSize * leftScale);
        int leftScaledHeight = (int) (sideSize * leftScale);
        int leftLeft = leftRect.left + (int) leftDx - (leftScaledWidth - sideSize) / 2;
        int leftTop = leftRect.top - (leftScaledHeight - sideSize) / 2;
        Rect leftDestRect = new Rect(leftLeft, leftTop, leftLeft + leftScaledWidth, leftTop + leftScaledHeight);
        drawScaledRoundBitmap(canvas, bitmaps.get(leftIndex), leftDestRect, 1.0f);
        
        // 计算右侧图片的位置和缩放
        // 向左滑动时，右侧图片向左移动（向中间靠拢）；向右滑动时，右侧图片向右移动（远离中间）
        float rightDx = currentOffsetX * 0.5f;
        int rightScaledWidth = (int) (sideSize * rightScale);
        int rightScaledHeight = (int) (sideSize * rightScale);
        int rightLeft = rightRect.left + (int) rightDx - (rightScaledWidth - sideSize) / 2;
        int rightTop = rightRect.top - (rightScaledHeight - sideSize) / 2;
        Rect rightDestRect = new Rect(rightLeft, rightTop, rightLeft + rightScaledWidth, rightTop + rightScaledHeight);
        drawScaledRoundBitmap(canvas, bitmaps.get(rightIndex), rightDestRect, 1.0f);

        // 中心图片（带滑动偏移和缩放）
        Bitmap centerBitmap = bitmaps.get(currentIndex);
        if (centerBitmap == null) return;
        float dx = currentOffsetX;
        float scale = centerScale;
        int scaledWidth = (int) (centerSize * scale);
        int scaledHeight = (int) (centerSize * scale);
        int centerLeft = centerRect.left + (int) dx - (scaledWidth - centerSize) / 2;
        int centerTop = centerRect.top - (scaledHeight - centerSize) / 2;
        Rect destRect = new Rect(centerLeft, centerTop, centerLeft + scaledWidth, centerTop + scaledHeight);
        drawScaledRoundBitmap(canvas, centerBitmap, destRect, 1.0f);

        // 倒影
        drawReflection(canvas, destRect);
    }

    private void drawTransition(Canvas canvas) {
        float progress = transitionProgress;
        
        // 根据动画方向确定绘制顺序，避免遮挡问题
        // 绘制原则：先画在最底层的图片，后画在最上层的图片
        if (isNextAnimation) {
            // 下一张：右→左（底层），左→中（中层），中→右（上层）
            // 绘制顺序：位置 2(中→右), 位置 0(左→中), 位置 1(右→左)
            drawTransitionBitmap(canvas, 0, progress); //   中→右（最底层，最先画）
            drawTransitionBitmap(canvas, 2, progress); // 左→中（中层）
            drawTransitionBitmap(canvas, 1, progress); //  右→左（最底层，最先画）
        } else {
            // 上一张：左→右（底层），中→左（中层），右→中（上层）
            // 绘制顺序：位置 0(中→左), 位置 2(右→中), 位置 1(左→右)
            drawTransitionBitmap(canvas, 0, progress); // 中→左（最底层，最先画）
            drawTransitionBitmap(canvas, 2, progress); // 右→中（中层）
            drawTransitionBitmap(canvas, 1, progress); // 左→右（最上层，最后画）
        }
    }
    
    // 绘制过渡动画中的单张图片
    private void drawTransitionBitmap(Canvas canvas, int positionIndex, float progress) {
        int idx = startIndices[positionIndex];
        if (idx < 0 || idx >= bitmaps.size()) return;
        Bitmap bmp = bitmaps.get(idx);
        if (bmp == null) return;

        Rect start = startRects[positionIndex];
        Rect end = endRects[positionIndex];
        Rect current = currentRects[positionIndex];
        
        // 计算当前矩形位置
        current.left = start.left + (int) ((end.left - start.left) * progress);
        current.top = start.top + (int) ((end.top - start.top) * progress);
        current.right = start.right + (int) ((end.right - start.right) * progress);
        current.bottom = start.bottom + (int) ((end.bottom - start.bottom) * progress);
        
        // 根据目标位置计算缩放比例：中间位置为 1.0，两侧位置也为 1.0（因为 bitmap 已经缩放到 centerSize）
        // 实际绘制时使用 rect 本身的大小即可
        drawScaledRoundBitmap(canvas, bmp, current, 1.0f);
    }

    // 绘制圆角图片（复用 Path，支持缩放）
    private void drawScaledRoundBitmap(Canvas canvas, Bitmap bitmap, Rect rect, float scale) {
        if (bitmap == null || rect == null) return;
            
        canvas.save();
            
        // 计算缩放后的尺寸和位置
        int scaledWidth = (int) (rect.width() * scale);
        int scaledHeight = (int) (rect.height() * scale);
        int left = rect.left + (rect.width() - scaledWidth) / 2;
        int top = rect.top + (rect.height() - scaledHeight) / 2;
        Rect scaledRect = new Rect(left, top, left + scaledWidth, top + scaledHeight);
            
        roundRectPath.reset();
        roundRectPath.addRoundRect(new RectF(scaledRect), cornerRadius, cornerRadius, Path.Direction.CW);
        canvas.clipPath(roundRectPath);
        canvas.drawBitmap(bitmap, null, scaledRect, null);
        canvas.restore();
    }
        
    // 绘制圆角图片（复用 Path，默认不缩放）
    private void drawRoundBitmap(Canvas canvas, Bitmap bitmap, Rect rect) {
        drawScaledRoundBitmap(canvas, bitmap, rect, 1.0f);
    }

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

    private void generateReflection(Bitmap sourceBitmap) {
        if (sourceBitmap == null) return;
        int reflectHeight = sourceBitmap.getHeight() / 3;
        if (reflectHeight <= 0) return;

        // 从原图底部裁剪
        Bitmap bottomPart = Bitmap.createBitmap(sourceBitmap, 0, sourceBitmap.getHeight() - reflectHeight,
                sourceBitmap.getWidth(), reflectHeight);

        // 创建倒影bitmap并垂直翻转
        Bitmap reflection = Bitmap.createBitmap(bottomPart.getWidth(), reflectHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(reflection);
        Matrix matrix = new Matrix();
        matrix.setScale(1, -1);
        matrix.postTranslate(0, reflectHeight);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawBitmap(bottomPart, matrix, paint);

        // 释放临时bitmap
        bottomPart.recycle();

        // 垂直渐变
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
                // 限制最大移动距离为 swipeThresholdPx
                currentOffsetX = Math.max(-swipeThresholdPx, Math.min(swipeThresholdPx, deltaX));
                float progress = Math.min(1.0f, Math.abs(currentOffsetX) / (float) swipeThresholdPx);
                
                // 根据滑动方向计算三张图片的缩放比例
                float centerScaleDelta = progress * (1 - MAX_SCALE_DOWN);
                float sideScaleDelta = progress * (1 - MAX_SCALE_DOWN) * 0.5f; // 侧边图片缩放幅度较小
                
                if (currentOffsetX > 0) {
                    // 向右滑动：左->中，中->右，右->左
                    centerScale = 1.0f - centerScaleDelta;  // 中间图片缩小
                    leftScale = 1.0f + sideScaleDelta;      // 左侧图片向中间移动，放大
                    rightScale = 1.0f - sideScaleDelta;     // 右侧图片远离，缩小
                } else {
                    // 向左滑动：右->中，中->左，左->右
                    centerScale = 1.0f - centerScaleDelta;  // 中间图片缩小
                    leftScale = 1.0f - sideScaleDelta;      // 左侧图片远离，缩小
                    rightScale = 1.0f + sideScaleDelta;     // 右侧图片向中间移动，放大
                }
                
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
            float progress = Math.min(1.0f, Math.abs(currentOffsetX) / swipeThresholdPx);
            
            // 回弹过程中三张图片同步恢复缩放
            float centerScaleDelta = progress * (1 - MAX_SCALE_DOWN);
            float sideScaleDelta = progress * (1 - MAX_SCALE_DOWN) * 0.5f;
            
            if (currentOffsetX > 0) {
                centerScale = 1.0f - centerScaleDelta;
                leftScale = 1.0f + sideScaleDelta;
                rightScale = 1.0f - sideScaleDelta;
            } else {
                centerScale = 1.0f - centerScaleDelta;
                leftScale = 1.0f - sideScaleDelta;
                rightScale = 1.0f + sideScaleDelta;
            }
            
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentOffsetX = 0;
                centerScale = 1.0f;
                leftScale = 1.0f;
                rightScale = 1.0f;
                invalidate();
            }
        });
        animator.start();
        snapAnimator = animator;
    }

    // ---------- 公开切换接口 ----------
    public void next() {
        if (isAnimating || bitmaps.isEmpty()) return;
        //int newIndex = (currentIndex + 1) % bitmaps.size();
        //if (newIndex == currentIndex) return;
        // 下一张：新图片从右侧进入，当前图片向左退出 -> 方向参数为 true
        performSwitch( true);
    }

    public void previous() {
        if (isAnimating || bitmaps.isEmpty()) return;
        //int newIndex = (currentIndex - 1 + bitmaps.size()) % bitmaps.size();
        //if (newIndex == currentIndex) return;

        // 上一张：新图片从左侧进入，当前图片向右退出 -> 方向参数为 false
        performSwitch(false);
    }

    private void performSwitch(boolean isNext) {
        // 起始三张图片索引（左、中、右）
        int leftIdx = (currentIndex - 1 + bitmaps.size()) % bitmaps.size();
        int centerIdx = currentIndex;
        int rightIdx = (currentIndex + 1) % bitmaps.size();
    
        // 结束三张图片索引
        int endLeftIdx, endCenterIdx, endRightIdx;
        if (isNext) {
            // 下一张：索引向前滚动
            // 左侧图片 → 移动到中间
            // 中间图片 → 移动到右侧  
            // 右侧图片 → 移动到左侧（新图片进入）
            endLeftIdx = rightIdx;
            endCenterIdx = leftIdx;
            endRightIdx = centerIdx;
        } else {
            // 上一张：索引向后滚动
            // 左侧图片 → 移动到右侧
            // 中间图片 → 移动到左侧
            // 右侧图片 → 移动到中间
            endLeftIdx = centerIdx;
            endCenterIdx = rightIdx;
            endRightIdx = leftIdx;
        }
        //Log.i("TriImageCarouselView", "isNext=" + isNext + " start: [" + leftIdx + "," + centerIdx + "," + rightIdx + "] end: [" + endLeftIdx + "," + endCenterIdx + "," + endRightIdx + "]");
        startIndices[0] = leftIdx;
        startIndices[1] = centerIdx;
        startIndices[2] = rightIdx;
        endIndices[0] = endLeftIdx;
        endIndices[1] = endCenterIdx;
        endIndices[2] = endRightIdx;
    
        // 起始矩形（固定位置）
        startRects[0].set(leftRect);
        startRects[1].set(centerRect);
        startRects[2].set(rightRect);
    
        // 结束矩形（根据目标位置）
        // 位置 0：左边
        endRects[0].set(leftRect);
        // 位置 1：中间
        endRects[1].set(centerRect);
        // 位置 2：右边
        endRects[2].set(rightRect);
    
        // 重置滑动状态
        currentOffsetX = 0;
        centerScale = 1.0f;
        leftScale = 1.0f;
        rightScale = 1.0f;
        isAnimating = true;
        transitionProgress = 0f;
        isNextAnimation = isNext; // 记录动画方向
    
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
                currentIndex = endCenterIdx;
                updateReflection();
                invalidate();
            }
        });
        animator.start();
        snapAnimator = animator;
    }

    // 设置图片列表，自动缩放至中心尺寸
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
        // 回收所有 bitmap 资源
        for (Bitmap bmp : bitmaps) {
            if (bmp != null && !bmp.isRecycled()) {
                bmp.recycle();
            }
        }
        bitmaps.clear();
        if (reflectionBitmap != null && !reflectionBitmap.isRecycled()) {
            reflectionBitmap.recycle();
            reflectionBitmap = null;
        }
    }
}