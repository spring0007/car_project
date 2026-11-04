package com.launcher.ui16;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.DecelerateInterpolator;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 三图循环轮播：
 * <pre>
 *    [左]   [中]   [右]
 * </pre>
 * 手指左右拖动时三张图跟随手指位移并做缩放，抬手后按「位移 / 速度」决定
 * 切换到下一张（向左滑）或上一张（向右滑），否则回弹归位。
 *
 * <p>性能约束：{@link #onDraw(Canvas)} 中不允许分配对象，也不使用 clipPath，
 * 所有圆角在 {@link #setBitmaps(List)} 时一次性烘焙到 Bitmap 上，
 * 绘制阶段只做纯 bitmap 贴图，保证车机低性能 GPU 下拖动仍然跟手。</p>
 */
public class TriImageCarouselView extends View {

    /** 三个绘制槽位，下标即槽位序号 */
    private static final int SLOT_LEFT = 0;
    private static final int SLOT_CENTER = 1;
    private static final int SLOT_RIGHT = 2;

    /** 拖动到最大位移时中间图片的缩放 */
    private static final float MAX_SCALE_DOWN = 0.7f;
    /** 最大拖拽位移相对于 View 宽度的比例（超过后进入阻尼区） */
    private static final float SWIPE_DISTANCE_RATIO = 0.3f;
    /** 位移达到 max 拖拽距离的多少即判定为切换 */
    private static final float SWITCH_DISTANCE_RATIO = 0.5f;
    /** 快速滑动判定速度（px/s） */
    private static final float FLING_VELOCITY = 1000f;
    /** 超出最大拖拽位移后的阻尼系数 */
    private static final float OVER_DRAG_FACTOR = 0.35f;
    /** 超拽位移上限（相对最大拖拽位移） */
    private static final float OVER_DRAG_MAX_RATIO = 1.25f;
    /** 切换动画时长 */
    private static final long SWITCH_DURATION = 300L;
    /** 回弹动画时长 */
    private static final long SNAP_DURATION = 250L;
    /** 切换动画最多绘制 4 张贴图（3 张 + 1 张循环补位图） */
    private static final int MAX_TRACKS = 4;

    // ---------- 图片数据 ----------
    /** 已经缩放到 centerSize 且圆角烘焙好的图片，与外部传入的列表一一对应 */
    private final List<Bitmap> bitmaps = new ArrayList<>();
    /** 与 bitmaps 一一对应的倒影图，切换时无需重新生成 */
    private final List<Bitmap> reflectionBitmaps = new ArrayList<>();
    private int currentIndex = 0;

    // ---------- 尺寸(px) ----------
    private int sideSize;
    private int centerSize;
    private int spacing;
    private int cornerRadius;

    // ---------- 三个槽位（固定位置） ----------
    private final Rect leftRect = new Rect();
    private final Rect centerRect = new Rect();
    private final Rect rightRect = new Rect();

    // ---------- onDraw 复用对象（避免每帧分配对象触发 GC） ----------
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint reflectionPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Rect srcRect = new Rect();
    private final Rect drawRect = new Rect();
    private final Rect reflectRect = new Rect();
    private final RectF tmpRectF = new RectF();
    private final Rect[] dragRects = {new Rect(), new Rect(), new Rect()};

    // ---------- 切换动画的轨道 ----------
    private final int[] trackIndices = new int[MAX_TRACKS];
    private final Rect[] trackStart = new Rect[MAX_TRACKS];
    private final Rect[] trackEnd = new Rect[MAX_TRACKS];
    private final Rect[] trackCurrent = new Rect[MAX_TRACKS];
    private int trackCount = 0;

    // ---------- 触摸交互 ----------
    private float downX;
    private float dragDistanceX;
    private float currentOffsetX = 0f;
    private int activePointerId = -1;
    private boolean isDragging = false;
    private VelocityTracker velocityTracker;
    private int swipeThresholdPx;

    // ---------- 拖动缩放 ----------
    private float centerScale = 1.0f;
    private float leftScale = 1.0f;
    private float rightScale = 1.0f;

    // ---------- 切换动画 ----------
    private boolean isAnimating = false;
    private float transitionProgress = 0f;
    private ValueAnimator switchAnimator;
    private ValueAnimator snapAnimator;

    public TriImageCarouselView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
        // 复用对象，避免动画/绘制过程中分配内存
        for (int i = 0; i < MAX_TRACKS; i++) {
            trackStart[i] = new Rect();
            trackEnd[i] = new Rect();
            trackCurrent[i] = new Rect();
        }
    }

    private void init(Context context, AttributeSet attrs) {
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
        swipeThresholdPx = (int) (getWidth() * SWIPE_DISTANCE_RATIO);
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

    // ---------- 绘制 ----------

    @Override
    protected void onDraw(Canvas canvas) {
        if (bitmaps.isEmpty()) return;

        // 保证滑出/滑入的图片不会画到 View 之外（矩形裁剪开销可忽略）
        canvas.clipRect(0, 0, getWidth(), getHeight());

        if (isAnimating) {
            drawTransition(canvas);
        } else {
            drawNormal(canvas);
        }
    }

    private void drawNormal(Canvas canvas) {
        int n = bitmaps.size();
        int leftIndex = (currentIndex - 1 + n) % n;
        int rightIndex = (currentIndex + 1) % n;

        // 左右图片（跟随手指位移 + 缩放）
        computeSlotRect(leftRect, sideSize, leftScale, currentOffsetX * 0.5f, drawRect);
        drawImage(canvas, bitmaps.get(leftIndex), drawRect);

        computeSlotRect(rightRect, sideSize, rightScale, currentOffsetX * 0.5f, drawRect);
        drawImage(canvas, bitmaps.get(rightIndex), drawRect);

        // 中心图片（最后绘制，压在最上层）
        computeSlotRect(centerRect, centerSize, centerScale, currentOffsetX, drawRect);
        drawImage(canvas, bitmaps.get(currentIndex), drawRect);

        // 倒影
        drawReflection(canvas, currentIndex, drawRect, 255);
    }

    private void drawTransition(Canvas canvas) {
        // 先画滑出/滑入的两条轨道，它们位于画面两侧，互不遮挡
        drawTrack(canvas, 0);
        drawTrack(canvas, 1);

        // 轨道 2 = 原中心图（移向侧边），轨道 3 = 新中心图。
        // 前半程让原中心图压在上层（与拖拽时层级一致），后半程让新中心图压在上层，
        // 使抬手瞬间和动画结束时都不会出现层级跳变。
        if (transitionProgress < 0.5f) {
            drawTrack(canvas, 3);
            drawTrack(canvas, 2);
        } else {
            drawTrack(canvas, 2);
            drawTrack(canvas, 3);
        }
    }

    private void drawTrack(Canvas canvas, int i) {
        if (i < 0 || i >= trackCount) return;

        Rect start = trackStart[i];
        Rect end = trackEnd[i];
        Rect current = trackCurrent[i];
        float p = transitionProgress;

        current.left = start.left + (int) ((end.left - start.left) * p);
        current.top = start.top + (int) ((end.top - start.top) * p);
        current.right = start.right + (int) ((end.right - start.right) * p);
        current.bottom = start.bottom + (int) ((end.bottom - start.bottom) * p);

        int index = trackIndices[i];
        if (index < 0 || index >= bitmaps.size()) return;

        drawImage(canvas, bitmaps.get(index), current);

        // 原中心图的倒影淡出，新中心图的倒影淡入，避免倒影瞬间消失
        if (i == 2) {
            drawReflection(canvas, index, current, (int) (255 * (1f - p) + 0.5f));
        } else if (i == 3) {
            drawReflection(canvas, index, current, (int) (255 * p + 0.5f));
        }
    }

    /**
     * 计算某个槽位图片在当前拖拽状态下的绘制区域。
     *
     * @param base     槽位基准矩形
     * @param baseSize 槽位基准边长
     * @param scale    当前缩放
     * @param dx       水平位移
     * @param out      输出矩形（复用对象）
     */
    private void computeSlotRect(Rect base, int baseSize, float scale, float dx, Rect out) {
        int size = (int) (baseSize * scale + 0.5f);
        int left = base.left + Math.round(dx) - (size - baseSize) / 2;
        int top = base.top - (size - baseSize) / 2;
        out.set(left, top, left + size, top + size);
    }

    private void drawImage(Canvas canvas, Bitmap bitmap, Rect dst) {
        if (bitmap == null || bitmap.isRecycled()) return;
        srcRect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
        canvas.drawBitmap(bitmap, srcRect, dst, bitmapPaint);
    }

    private void drawReflection(Canvas canvas, int index, Rect centerDst, int alpha) {
        if (alpha <= 0 || reflectionBitmaps.isEmpty()) return;
        if (index < 0 || index >= reflectionBitmaps.size()) return;

        Bitmap reflection = reflectionBitmaps.get(index);
        if (reflection == null || reflection.isRecycled()) return;

        int reflectHeight = centerDst.height() / 3;
        if (reflectHeight <= 0) return;

        reflectRect.set(centerDst.left, centerDst.bottom,
                centerDst.right, centerDst.bottom + reflectHeight);

        reflectionPaint.setAlpha(alpha);
        srcRect.set(0, 0, reflection.getWidth(), reflection.getHeight());
        canvas.drawBitmap(reflection, srcRect, reflectRect, reflectionPaint);
    }

    // ---------- 触摸事件 ----------

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (bitmaps.isEmpty()) return false;

        final int action = event.getActionMasked();

        // 切换动画过程中忽略手势，避免状态错乱
        if (isAnimating) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                isDragging = false;
                activePointerId = -1;
                recycleVelocityTracker();
                releaseParentIntercept();
            }
            return true;
        }

        if (velocityTracker == null) velocityTracker = VelocityTracker.obtain();
        velocityTracker.addMovement(event);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                if (snapAnimator != null && snapAnimator.isRunning()) snapAnimator.cancel();
                activePointerId = event.getPointerId(0);
                downX = event.getX(0);
                dragDistanceX = 0f;
                isDragging = true;
                // 避免被外层（Workspace 翻页等）抢走手势
                ViewParent parent = getParent();
                if (parent != null) parent.requestDisallowInterceptTouchEvent(true);
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                int index = event.getActionIndex();
                activePointerId = event.getPointerId(index);
                // 保持已产生的拖拽量连续，避免多指切换时画面跳变
                downX = event.getX(index) - dragDistanceX;
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                int index = event.findPointerIndex(activePointerId);
                if (index < 0) break;
                dragDistanceX = event.getX(index) - downX;
                updateDragState(dragDistanceX);
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                int index = event.getActionIndex();
                if (event.getPointerId(index) == activePointerId) {
                    int newIndex = (index == 0) ? 1 : 0;
                    if (newIndex < event.getPointerCount()) {
                        activePointerId = event.getPointerId(newIndex);
                        downX = event.getX(newIndex) - dragDistanceX;
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (!isDragging) {
                    recycleVelocityTracker();
                    break;
                }
                isDragging = false;
                int index = event.findPointerIndex(activePointerId);
                float velocityX = 0f;
                velocityTracker.computeCurrentVelocity(1000);
                if (index >= 0) {
                    velocityX = velocityTracker.getXVelocity(activePointerId);
                }
                handleRelease(velocityX, dragDistanceX);
                recycleVelocityTracker();
                activePointerId = -1;
                releaseParentIntercept();
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                isDragging = false;
                recycleVelocityTracker();
                activePointerId = -1;
                // 手势被父控件打断时也要回弹，否则会停在半路
                startSnapAnimation(0f);
                releaseParentIntercept();
                break;
            }
            default:
                break;
        }
        return true;
    }

    /** 根据手指位移更新绘制状态：位置带阻尼，缩放最多到 1 倍 */
    private void updateDragState(float rawOffset) {
        float absRaw = Math.abs(rawOffset);
        float absOffset;
        if (absRaw <= swipeThresholdPx) {
            absOffset = absRaw;
        } else {
            // 超过最大位移后进入阻尼区，手指继续移动仍有一点点反馈，避免「拖不动」的错觉
            absOffset = swipeThresholdPx + (absRaw - swipeThresholdPx) * OVER_DRAG_FACTOR;
            float maxOffset = swipeThresholdPx * OVER_DRAG_MAX_RATIO;
            if (absOffset > maxOffset) absOffset = maxOffset;
        }
        currentOffsetX = (rawOffset >= 0) ? absOffset : -absOffset;

        float progress = (swipeThresholdPx <= 0)
                ? 0f
                : Math.min(1f, absRaw / (float) swipeThresholdPx);
        applyDragScales(progress, currentOffsetX > 0);
        invalidate();
    }

    private void applyDragScales(float progress, boolean towardRight) {
        float centerDelta = progress * (1f - MAX_SCALE_DOWN);
        float sideDelta = progress * (1f - MAX_SCALE_DOWN) * 0.5f;
        centerScale = 1.0f - centerDelta;
        if (towardRight) {
            leftScale = 1.0f + sideDelta;
            rightScale = 1.0f - sideDelta;
        } else {
            leftScale = 1.0f - sideDelta;
            rightScale = 1.0f + sideDelta;
        }
    }

    private void handleRelease(float velocityX, float rawOffset) {
        if (swipeThresholdPx <= 0) {
            startSnapAnimation(0f);
            return;
        }

        // 快速滑动优先按速度方向判定
        if (Math.abs(velocityX) > FLING_VELOCITY) {
            if (velocityX < 0) {
                next();
            } else {
                previous();
            }
            return;
        }

        // 慢速拖动按位移判定
        float switchDistance = swipeThresholdPx * SWITCH_DISTANCE_RATIO;
        if (rawOffset <= -switchDistance) {
            next();
        } else if (rawOffset >= switchDistance) {
            previous();
        } else {
            startSnapAnimation(0f);
        }
    }

    private void startSnapAnimation(float targetOffset) {
        if (snapAnimator != null) snapAnimator.cancel();

        // 本来就在归位状态，无需动画
        if (Math.abs(currentOffsetX - targetOffset) < 0.5f) {
            currentOffsetX = targetOffset;
            applyDragScales(0f, true);
            invalidate();
            return;
        }

        ValueAnimator animator = ValueAnimator.ofFloat(currentOffsetX, targetOffset);
        animator.setDuration(SNAP_DURATION);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            currentOffsetX = (float) animation.getAnimatedValue();
            float progress = (swipeThresholdPx <= 0)
                    ? 0f
                    : Math.min(1f, Math.abs(currentOffsetX) / (float) swipeThresholdPx);
            applyDragScales(progress, currentOffsetX > 0);
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentOffsetX = 0f;
                applyDragScales(0f, true);
                invalidate();
            }
        });
        animator.start();
        snapAnimator = animator;
    }

    private void recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    private void releaseParentIntercept() {
        ViewParent parent = getParent();
        if (parent != null) parent.requestDisallowInterceptTouchEvent(false);
    }

    // ---------- 公开切换接口 ----------

    /** 切换到下一张：整体向左滚，右侧补位 */
    public void next() {
        performSwitch(true);
    }

    /** 切换到上一张：整体向右滚，左侧补位 */
    public void previous() {
        performSwitch(false);
    }

    private void performSwitch(boolean isNext) {
        if (isAnimating) return;
        int n = bitmaps.size();
        if (n <= 1) return;

        int leftIndex = (currentIndex - 1 + n) % n;
        int centerIndex = currentIndex;
        int rightIndex = (currentIndex + 1) % n;

        // 动画起点取当前拖拽状态，抬手瞬间不会跳回原位。
        // 必须在取消回弹动画之前取，否则 onAnimationEnd 会把拖拽状态清零。
        computeSlotRect(leftRect, sideSize, leftScale, currentOffsetX * 0.5f, dragRects[SLOT_LEFT]);
        computeSlotRect(centerRect, centerSize, centerScale, currentOffsetX, dragRects[SLOT_CENTER]);
        computeSlotRect(rightRect, sideSize, rightScale, currentOffsetX * 0.5f, dragRects[SLOT_RIGHT]);

        if (snapAnimator != null && snapAnimator.isRunning()) snapAnimator.cancel();

        // 完全移出屏幕所需的距离（左右槽位与 View 边缘对齐）
        final int exitDistance = sideSize;
        final int newCenterIndex;

        if (isNext) {
            newCenterIndex = rightIndex;

            // 0：左图滑出左侧屏幕
            trackIndices[0] = leftIndex;
            trackStart[0].set(dragRects[SLOT_LEFT]);
            trackEnd[0].set(leftRect);
            trackEnd[0].offset(-exitDistance, 0);

            // 1：同一张图（循环）从右侧屏幕外补入最右槽位
            trackIndices[1] = leftIndex;
            trackStart[1].set(rightRect);
            trackStart[1].offset(exitDistance, 0);
            trackEnd[1].set(rightRect);

            // 2：中图 → 左槽位
            trackIndices[2] = centerIndex;
            trackStart[2].set(dragRects[SLOT_CENTER]);
            trackEnd[2].set(leftRect);

            // 3：右图 → 中槽位（最后绘制，位于最上层）
            trackIndices[3] = rightIndex;
            trackStart[3].set(dragRects[SLOT_RIGHT]);
            trackEnd[3].set(centerRect);
        } else {
            newCenterIndex = leftIndex;

            // 0：右图滑出右侧屏幕
            trackIndices[0] = rightIndex;
            trackStart[0].set(dragRects[SLOT_RIGHT]);
            trackEnd[0].set(rightRect);
            trackEnd[0].offset(exitDistance, 0);

            // 1：同一张图（循环）从左侧屏幕外补入最左槽位
            trackIndices[1] = rightIndex;
            trackStart[1].set(leftRect);
            trackStart[1].offset(-exitDistance, 0);
            trackEnd[1].set(leftRect);

            // 2：中图 → 右槽位
            trackIndices[2] = centerIndex;
            trackStart[2].set(dragRects[SLOT_CENTER]);
            trackEnd[2].set(rightRect);

            // 3：左图 → 中槽位（最后绘制，位于最上层）
            trackIndices[3] = leftIndex;
            trackStart[3].set(dragRects[SLOT_LEFT]);
            trackEnd[3].set(centerRect);
        }
        trackCount = MAX_TRACKS;

        // 重置拖动状态
        currentOffsetX = 0f;
        centerScale = 1.0f;
        leftScale = 1.0f;
        rightScale = 1.0f;

        isAnimating = true;
        transitionProgress = 0f;

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(SWITCH_DURATION);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            transitionProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimating = false;
                currentIndex = newCenterIndex;
                invalidate();
            }
        });
        animator.start();
        switchAnimator = animator;
    }

    // ---------- 图片处理 ----------

    /**
     * 设置图片列表：内部会缩放到中心尺寸并把圆角烘焙进 Bitmap，
     * 绘制阶段直接贴图，避免每帧 clipPath。
     */
    public void setBitmaps(List<Bitmap> sources) {
        if (sources == null) return;

        if (switchAnimator != null) switchAnimator.cancel();
        if (snapAnimator != null) snapAnimator.cancel();
        isAnimating = false;
        releaseBitmaps();

        for (Bitmap source : sources) {
            if (source == null || source.isRecycled()) continue;
            Bitmap scaled = Bitmap.createScaledBitmap(source, centerSize, centerSize, true);
            Bitmap configSafe = scaled;
            if (configSafe.getConfig() == Bitmap.Config.HARDWARE) {
                // 硬件位图不能画到软件 Canvas 上，先转成 ARGB_8888
                configSafe = scaled.copy(Bitmap.Config.ARGB_8888, false);
            }
            Bitmap rounded = createRoundBitmap(configSafe);
            bitmaps.add(rounded);
            reflectionBitmaps.add(createReflection(rounded));
            // createScaledBitmap 在尺寸/配置一致时可能直接返回原图，不能误回收调用方的 Bitmap
            if (scaled != source) {
                scaled.recycle();
            }
            if (configSafe != scaled) {
                configSafe.recycle();
            }
        }

        currentIndex = 0;
        currentOffsetX = 0f;
        applyDragScales(0f, true);
        requestLayout();
        invalidate();
    }

    /** 一次性把圆角烘焙进 Bitmap（抗锯齿由 BitmapShader + drawRoundRect 保证） */
    private Bitmap createRoundBitmap(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        float radius = Math.min(cornerRadius, Math.min(width, height) / 2f);
        tmpRectF.set(0, 0, width, height);
        canvas.drawRoundRect(tmpRectF, radius, radius, paint);
        return output;
    }

    /** 生成倒影（只做一次，切换时直接取用，避免动画开始时的分配抖动） */
    private Bitmap createReflection(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int reflectHeight = height / 3;
        if (reflectHeight <= 0) return null;

        Bitmap bottomPart = Bitmap.createBitmap(source, 0, height - reflectHeight, width, reflectHeight);
        Bitmap reflection = Bitmap.createBitmap(width, reflectHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(reflection);
        Matrix matrix = new Matrix();
        matrix.setScale(1f, -1f);
        matrix.postTranslate(0, reflectHeight);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(bottomPart, matrix, paint);
        bottomPart.recycle();

        LinearGradient gradient = new LinearGradient(0, 0, 0, reflectHeight,
                0xCC000000, 0x00000000, Shader.TileMode.CLAMP);
        Paint gradientPaint = new Paint();
        gradientPaint.setShader(gradient);
        gradientPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawRect(0, 0, width, reflectHeight, gradientPaint);
        return reflection;
    }

    private void releaseBitmaps() {
        for (Bitmap bitmap : bitmaps) {
            if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        }
        bitmaps.clear();
        for (Bitmap bitmap : reflectionBitmaps) {
            if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        }
        reflectionBitmaps.clear();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (switchAnimator != null) {
            switchAnimator.cancel();
            switchAnimator = null;
        }
        if (snapAnimator != null) {
            snapAnimator.cancel();
            snapAnimator = null;
        }
        recycleVelocityTracker();
        // 回收所有 bitmap 资源（均为内部生成的副本）
        releaseBitmaps();
    }
}
