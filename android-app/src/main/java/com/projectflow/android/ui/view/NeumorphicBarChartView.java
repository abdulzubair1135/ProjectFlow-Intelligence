package com.projectflow.android.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.projectflow.android.R;
import java.util.ArrayList;
import java.util.List;

public class NeumorphicBarChartView extends View {

    public static class BarItem {
        public String label;
        public int count;
        public int max;
        public int color;

        public BarItem(String label, int count, int max, int color) {
            this.label = label;
            this.count = count;
            this.max = max > 0 ? max : 1;
            this.color = color;
        }
    }

    private final List<BarItem> items = new ArrayList<>();
    private Paint barTrackPaint;
    private Paint barFillPaint;
    private Paint textLabelPaint;
    private Paint textValuePaint;
    private float animationProgress = 0f;
    private ValueAnimator animator;

    public NeumorphicBarChartView(Context context) {
        super(context);
        init();
    }

    public NeumorphicBarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NeumorphicBarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        barTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barTrackPaint.setStyle(Paint.Style.FILL);
        barTrackPaint.setColor(ContextCompat.getColor(getContext(), R.color.neu_surface_pressed));

        barFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barFillPaint.setStyle(Paint.Style.FILL);

        textLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textLabelPaint.setTextSize(getResources().getDisplayMetrics().density * 12f);
        textLabelPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));

        textValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textValuePaint.setTextSize(getResources().getDisplayMetrics().density * 12f);
        textValuePaint.setFakeBoldText(true);
        textValuePaint.setColor(ContextCompat.getColor(getContext(), R.color.text_main));
        textValuePaint.setTextAlign(Paint.Align.RIGHT);

        // Sample items
        setSampleData();
    }

    public void setSampleData() {
        items.clear();
        int max = 10;
        items.add(new BarItem("Core Architecture", 8, max, ContextCompat.getColor(getContext(), R.color.chart_done)));
        items.add(new BarItem("Admin Integration", 6, max, ContextCompat.getColor(getContext(), R.color.chart_progress)));
        items.add(new BarItem("Risk Radar Engine", 4, max, ContextCompat.getColor(getContext(), R.color.chart_review)));
        items.add(new BarItem("Team Collaboration", 7, max, ContextCompat.getColor(getContext(), R.color.chart_accent_cyan)));
        animateBars();
    }

    public void setData(List<BarItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        animateBars();
    }

    private void animateBars() {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(800);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            animationProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float density = getResources().getDisplayMetrics().density;
        int rowHeight = (int) (density * 36f);
        int totalHeight = Math.max(120, items.size() * rowHeight + (int) (density * 16f));
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec), totalHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (items.isEmpty()) return;

        barTrackPaint.setColor(ContextCompat.getColor(getContext(), R.color.neu_surface_pressed));
        textLabelPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        textValuePaint.setColor(ContextCompat.getColor(getContext(), R.color.text_main));

        float density = getResources().getDisplayMetrics().density;
        float startX = density * 8f;
        float endX = getWidth() - (density * 8f);
        float currentY = density * 18f;
        float barHeight = density * 10f;
        float radius = barHeight / 2f;

        for (BarItem item : items) {
            // Draw Label and Value
            canvas.drawText(item.label, startX, currentY - (density * 4f), textLabelPaint);
            canvas.drawText(item.count + " tasks", endX, currentY - (density * 4f), textValuePaint);

            // Draw Track (Debossed groove)
            RectF trackRect = new RectF(startX, currentY, endX, currentY + barHeight);
            canvas.drawRoundRect(trackRect, radius, radius, barTrackPaint);

            // Draw Fill (3D Raised progress)
            float fillWidth = (endX - startX) * ((float) item.count / item.max) * animationProgress;
            if (fillWidth > 0) {
                RectF fillRect = new RectF(startX, currentY, startX + Math.max(radius * 2f, fillWidth), currentY + barHeight);
                barFillPaint.setColor(item.color);
                canvas.drawRoundRect(fillRect, radius, radius, barFillPaint);
            }

            currentY += density * 34f;
        }
    }
}
