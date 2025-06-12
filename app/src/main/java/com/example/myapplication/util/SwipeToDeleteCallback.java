package com.example.myapplication.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

    private final Drawable deleteIcon;
    private final int intrinsicWidth;
    private final int intrinsicHeight;
    private final ColorDrawable background;
    private final int backgroundColor;
    private final Paint clearPaint;
    private final OnItemSwipeListener listener;
    private boolean swipeBack = false;
    private boolean isDeleteTriggered = false;
    private static final float SWIPE_THRESHOLD = 0.3f; // 滑动达到30%宽度就触发

    public interface OnItemSwipeListener {
        void onItemSwiped(int position);
    }

    public SwipeToDeleteCallback(Context context, OnItemSwipeListener listener) {
        super(0, ItemTouchHelper.LEFT);
        this.listener = listener;

        // 设置删除图标
        deleteIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_delete);
        intrinsicWidth = deleteIcon.getIntrinsicWidth();
        intrinsicHeight = deleteIcon.getIntrinsicHeight();

        // 设置背景
        background = new ColorDrawable();
        backgroundColor = Color.parseColor("#F44336"); // 红色背景

        // 设置清除画笔
        clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        // 我们不支持上下移动
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // 实际的删除动作将在滑动过程中触发，这里只是恢复视图
        int position = viewHolder.getAdapterPosition();
        if (!isDeleteTriggered) {
            listener.onItemSwiped(position);
            isDeleteTriggered = true;
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getHeight();
        int itemWidth = itemView.getWidth();
        float absX = Math.abs(dX);

        // 重置删除触发标志
        if (dX == 0f) {
            isDeleteTriggered = false;
        }

        // 判断是否达到删除阈值
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            // 计算正方形区域的宽度
            float squareWidth = itemHeight; // 使红色区域成为正方形

            // 如果滑动距离达到正方形宽度并且用户正在滑动且未触发删除
            if (absX >= squareWidth && isCurrentlyActive && !isDeleteTriggered) {
                swipeBack = true;
                isDeleteTriggered = true;

                // 触发删除确认对话框
                listener.onItemSwiped(viewHolder.getAdapterPosition());

                // 限制滑动距离
                dX = dX > 0 ? squareWidth : -squareWidth;
            }
        }

        // 如果不是主动滑动，清除画布并返回
        boolean isCanceled = dX == 0 && !isCurrentlyActive;
        if (isCanceled) {
            clearCanvas(c, itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            return;
        }

        // 限制最大滑动距离为正方形宽度
        float limitedDX = Math.max(dX, -itemHeight);

        // 绘制红色背景
        background.setColor(backgroundColor);
        background.setBounds(
                itemView.getRight() + (int) limitedDX,
                itemView.getTop(),
                itemView.getRight(),
                itemView.getBottom()
        );
        background.draw(c);

        // 计算删除图标的位置
        int iconTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
        int iconMargin = (itemHeight - intrinsicHeight) / 2;
        int iconLeft = itemView.getRight() - iconMargin - intrinsicWidth;
        int iconRight = itemView.getRight() - iconMargin;
        int iconBottom = iconTop + intrinsicHeight;

        // 绘制删除图标
        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
        deleteIcon.draw(c);

        super.onChildDraw(c, recyclerView, viewHolder, limitedDX, dY, actionState, isCurrentlyActive);
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        // 设置滑动阈值，当滑动超过这个阈值时会触发onSwiped
        return SWIPE_THRESHOLD;
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        // 清除滑动状态
        swipeBack = false;
        isDeleteTriggered = false;
    }

    private void clearCanvas(Canvas c, float left, float top, float right, float bottom) {
        c.drawRect(left, top, right, bottom, clearPaint);
    }
}