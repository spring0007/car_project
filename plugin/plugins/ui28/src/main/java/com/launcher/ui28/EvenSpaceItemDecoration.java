package com.launcher.ui28;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * GridLayoutManager(VERTICAL) 的间隙平分装饰:
 * 水平方向 列数+1 个间隙、垂直方向 行数+1 个间隙,每个间隙大小相等,
 * 即左右两侧留白 = 列间距,上下两侧留白 = 行间距。
 *
 * 行数使用固定的网格行数(而不是按实际 item 数计算),
 * 这样不足一行/不足三列时,item 仍从第一行开始排,不会被垂直居中。
 *
 * 前提:item 宽度为 match_parent(由 GridLayoutManager 按 span 测量),
 * item 高度为固定值(图标高度)。
 */
public class EvenSpaceItemDecoration extends RecyclerView.ItemDecoration {

    private final int spanCount;   // 列数
    private final int rows;        // 固定行数
    private final int itemWidth;   // item 宽度(px),即图标宽度
    private final int itemHeight;  // item 高度(px)

    public EvenSpaceItemDecoration(int spanCount, int rows, int itemWidth, int itemHeight) {
        this.spanCount = Math.max(1, spanCount);
        this.rows = Math.max(1, rows);
        this.itemWidth = itemWidth;
        this.itemHeight = itemHeight;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
        if (!(lp instanceof GridLayoutManager.LayoutParams)) {
            super.getItemOffsets(outRect, view, parent, state);
            return;
        }

        int position = lp.getViewLayoutPosition();
        if (position == RecyclerView.NO_POSITION) {
            super.getItemOffsets(outRect, view, parent, state);
            return;
        }

        int itemCount = state.getItemCount();
        if (itemCount <= 0) {
            super.getItemOffsets(outRect, view, parent, state);
            return;
        }

        // item 满 span,列/行号可直接由 position 计算
        int column = position % spanCount;
        int row = Math.min(position / spanCount, rows - 1);

        int width = parent.getWidth() > 0 ? parent.getWidth() : parent.getMeasuredWidth();
        int height = parent.getHeight() > 0 ? parent.getHeight() : parent.getMeasuredHeight();
        width -= parent.getPaddingLeft() + parent.getPaddingRight();
        height -= parent.getPaddingTop() + parent.getPaddingBottom();

        // 水平:spanCount 列 → spanCount+1 个间隙平分剩余宽度
        int gapX = Math.max(0, (width - spanCount * itemWidth) / (spanCount + 1));
        outRect.left = gapX - column * gapX / spanCount;
        outRect.right = (column + 1) * gapX / spanCount;

        // 垂直:rows 行 → rows+1 个间隙平分剩余高度
        int gapY = Math.max(0, (height - rows * itemHeight) / (rows + 1));
        outRect.top = gapY - row * gapY / rows;
        outRect.bottom = (row + 1) * gapY / rows;
    }
}
