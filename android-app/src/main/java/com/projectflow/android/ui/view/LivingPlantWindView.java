package com.projectflow.android.ui.view;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.projectflow.android.R;
import java.util.Random;

/**
 * Living Botanical Plant with Swaying Leaves & Flowers reacting to Wind.
 * Features organic stems, multi-petal blooming flowers, flutter physics,
 * periodic wind gusts, drifting pollen particles, and interactive touch response.
 */
public class LivingPlantWindView extends View {

    private final Paint stemPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint leafPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint leafVeinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint flowerPetalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint flowerCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint potPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint potRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint windPollenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path stemPath = new Path();
    private final Path branchPath = new Path();
    private final Path leafPath = new Path();
    private final Path potPath = new Path();
    private final Matrix transformMatrix = new Matrix();

    // Wind & Sway Physics
    private float baseSwayAngle = 0f;
    private float targetSwayAngle = 0f;
    private float swayVelocity = 0f;
    private float windGustStrength = 0f;
    private float nextGustTimer = 2.0f;
    private float timeElapsed = 0f;
    private long lastFrameTime = 0;
    private boolean isRunning = false;

    // Drifting Pollen Particles
    private static class Pollen {
        float x, y;
        float vx, vy;
        float radius;
        int alpha;
    }
    private static final int POLLEN_COUNT = 10;
    private final Pollen[] pollens = new Pollen[POLLEN_COUNT];
    private final Random random = new Random();

    // Interactive Touch
    private boolean isUserTouching = false;
    private float touchTargetX = 0f;

    public LivingPlantWindView(Context context) {
        super(context);
        init();
    }

    public LivingPlantWindView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LivingPlantWindView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        stemPaint.setStyle(Paint.Style.STROKE);
        stemPaint.setStrokeCap(Paint.Cap.ROUND);
        stemPaint.setColor(Color.parseColor("#445D40")); // Deep mineral stalk green

        leafPaint.setStyle(Paint.Style.FILL);
        leafPaint.setColor(Color.parseColor("#4E9B66")); // Emerald Jade

        leafVeinPaint.setStyle(Paint.Style.STROKE);
        leafVeinPaint.setStrokeWidth(2f);
        leafVeinPaint.setColor(Color.parseColor("#6CB884"));

        flowerPetalPaint.setStyle(Paint.Style.FILL);
        flowerPetalPaint.setColor(Color.parseColor("#E06A55")); // Terracotta Coral

        flowerCenterPaint.setStyle(Paint.Style.FILL);
        flowerCenterPaint.setColor(Color.parseColor("#FFC857")); // Warm pollen gold

        potPaint.setStyle(Paint.Style.FILL);
        potPaint.setColor(Color.parseColor("#382E25")); // Dark clay ceramic

        potRimPaint.setStyle(Paint.Style.FILL);
        potRimPaint.setColor(Color.parseColor("#4A3E33"));

        windPollenPaint.setStyle(Paint.Style.FILL);
        windPollenPaint.setColor(Color.parseColor("#FFEAA7"));

        for (int i = 0; i < POLLEN_COUNT; i++) {
            Pollen p = new Pollen();
            resetPollen(p, true);
            pollens[i] = p;
        }
    }

    private void resetPollen(Pollen p, boolean randomX) {
        p.x = randomX ? random.nextFloat() * (getWidth() > 0 ? getWidth() : 500) : -10f;
        p.y = random.nextFloat() * (getHeight() > 0 ? getHeight() : 500);
        p.vx = 20f + random.nextFloat() * 35f;
        p.vy = -10f + random.nextFloat() * 20f;
        p.radius = 1.5f + random.nextFloat() * 2.2f;
        p.alpha = 40 + random.nextInt(90);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        isRunning = true;
        lastFrameTime = System.currentTimeMillis();
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isRunning = false;
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        isRunning = (visibility == VISIBLE);
        if (isRunning) {
            lastFrameTime = System.currentTimeMillis();
            postInvalidateOnAnimation();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                isUserTouching = true;
                touchTargetX = event.getX();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isUserTouching = false;
                // Add release spring impulse
                swayVelocity = (random.nextBoolean() ? 1 : -1) * 15f;
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        long now = System.currentTimeMillis();
        float dt = (lastFrameTime > 0) ? (now - lastFrameTime) / 1000f : 0.016f;
        lastFrameTime = now;
        if (dt > 0.1f) dt = 0.016f;
        timeElapsed += dt;

        float density = getResources().getDisplayMetrics().density;
        stemPaint.setStrokeWidth(density * 4.2f);

        // 1. Wind & Gust Simulation
        nextGustTimer -= dt;
        if (nextGustTimer <= 0) {
            windGustStrength = 18f + random.nextFloat() * 26f; // Sudden breeze gust
            nextGustTimer = 2.5f + random.nextFloat() * 4.0f;
        }
        // Decay gust smoothly
        windGustStrength = Math.max(0, windGustStrength - (dt * 18f));

        // Natural continuous gentle breeze oscillation
        float ambientBreeze = (float) Math.sin(timeElapsed * 1.8f) * 6f + (float) Math.cos(timeElapsed * 3.4f) * 3f;
        float totalWind = ambientBreeze + windGustStrength;

        // Damped harmonic spring physics for plant stem sway
        if (isUserTouching) {
            float rootX = w * 0.5f;
            float desiredAngle = (touchTargetX - rootX) * 0.12f;
            baseSwayAngle += (desiredAngle - baseSwayAngle) * dt * 8f;
        } else {
            float springForce = (totalWind - baseSwayAngle) * 7.5f;
            swayVelocity += springForce * dt;
            swayVelocity *= (1.0f - (dt * 2.8f)); // damping
            baseSwayAngle += swayVelocity * dt;
        }

        // Clamp extreme angles
        baseSwayAngle = Math.max(-42f, Math.min(42f, baseSwayAngle));

        float rootX = w * 0.5f;
        float rootY = h - (density * 32f); // Sits on pot top
        float stemHeight = h * 0.62f;

        // 2. Draw Ceramic Plant Pot at the base
        drawPot(canvas, rootX, rootY, density);

        // 3. Draw Main Swaying Plant Stem
        float tipX = rootX + (baseSwayAngle * density * 1.6f);
        float tipY = rootY - stemHeight;
        float ctrlX = rootX + (baseSwayAngle * density * 0.7f);
        float ctrlY = rootY - (stemHeight * 0.55f);

        stemPath.reset();
        stemPath.moveTo(rootX, rootY);
        stemPath.quadTo(ctrlX, ctrlY, tipX, tipY);
        canvas.drawPath(stemPath, stemPaint);

        // 4. Draw Lateral Branches with Leaves & Flowers
        // Branch 1 (Left Lower)
        float b1RootX = getQuadPoint(rootX, ctrlX, tipX, 0.32f);
        float b1RootY = getQuadPoint(rootY, ctrlY, tipY, 0.32f);
        float b1Angle = -48f + (baseSwayAngle * 0.4f) + (float) Math.sin(timeElapsed * 4.0f) * 6f;
        drawBranch(canvas, b1RootX, b1RootY, density * 44f, b1Angle, density, false);

        // Branch 2 (Right Mid)
        float b2RootX = getQuadPoint(rootX, ctrlX, tipX, 0.54f);
        float b2RootY = getQuadPoint(rootY, ctrlY, tipY, 0.54f);
        float b2Angle = 44f + (baseSwayAngle * 0.6f) + (float) Math.cos(timeElapsed * 3.8f) * 6f;
        drawBranch(canvas, b2RootX, b2RootY, density * 52f, b2Angle, density, true);

        // Branch 3 (Left Upper)
        float b3RootX = getQuadPoint(rootX, ctrlX, tipX, 0.76f);
        float b3RootY = getQuadPoint(rootY, ctrlY, tipY, 0.76f);
        float b3Angle = -38f + (baseSwayAngle * 0.8f) + (float) Math.sin(timeElapsed * 4.4f) * 7f;
        drawBranch(canvas, b3RootX, b3RootY, density * 40f, b3Angle, density, true);

        // Top Main Flower (Apex of the Plant)
        float flowerFlutter = (float) Math.sin(timeElapsed * 5.0f) * 8f;
        drawBloomingFlower(canvas, tipX, tipY, baseSwayAngle + flowerFlutter, density * 14f, density);

        // 5. Draw Drifting Pollen Particles
        for (Pollen p : pollens) {
            p.x += (p.vx + windGustStrength * 1.5f) * dt;
            p.y += p.vy * dt;
            if (p.x > w + 20f || p.y < -20f || p.y > h + 20f) {
                resetPollen(p, false);
            }
            windPollenPaint.setAlpha(p.alpha);
            canvas.drawCircle(p.x, p.y, p.radius * density, windPollenPaint);
        }

        if (isRunning) {
            postInvalidateOnAnimation();
        }
    }

    private void drawBranch(Canvas canvas, float startX, float startY, float length, float angleDeg, float density, boolean hasFlower) {
        float rad = (float) Math.toRadians(angleDeg);
        float endX = startX + (float) Math.sin(rad) * length;
        float endY = startY - (float) Math.cos(rad) * length;

        float ctrlX = (startX + endX) / 2f + (density * 5f);
        float ctrlY = (startY + endY) / 2f - (density * 2f);

        branchPath.reset();
        branchPath.moveTo(startX, startY);
        branchPath.quadTo(ctrlX, ctrlY, endX, endY);
        stemPaint.setStrokeWidth(density * 2.8f);
        canvas.drawPath(branchPath, stemPaint);

        // Mid-branch leaf
        float midX = (startX + endX) / 2f;
        float midY = (startY + endY) / 2f;
        drawLeaf(canvas, midX, midY, angleDeg + 35f, density * 16f, density);

        // End of branch: Flower or second Leaf
        if (hasFlower) {
            drawBloomingFlower(canvas, endX, endY, angleDeg, density * 12f, density);
        } else {
            drawLeaf(canvas, endX, endY, angleDeg - 25f, density * 20f, density);
        }
    }

    private void drawLeaf(Canvas canvas, float x, float y, float angleDeg, float size, float density) {
        float leafFlutter = (float) Math.sin(timeElapsed * 6f + x) * 6f;
        float totalAngle = angleDeg + leafFlutter;

        leafPath.reset();
        float halfW = size * 0.36f;
        leafPath.moveTo(0, 0);
        leafPath.cubicTo(halfW, -size * 0.35f, halfW * 0.8f, -size * 0.85f, 0, -size);
        leafPath.cubicTo(-halfW * 0.8f, -size * 0.85f, -halfW, -size * 0.35f, 0, 0);
        leafPath.close();

        transformMatrix.reset();
        transformMatrix.postRotate(totalAngle);
        transformMatrix.postTranslate(x, y);
        leafPath.transform(transformMatrix);

        leafPaint.setColor(Color.parseColor("#4E9B66"));
        canvas.drawPath(leafPath, leafPaint);

        // Subtle central vein
        float rad = (float) Math.toRadians(totalAngle);
        float veinEndX = x - (float) Math.sin(rad) * (size * 0.75f);
        float veinEndY = y - (float) Math.cos(rad) * (size * 0.75f);
        canvas.drawLine(x, y, veinEndX, veinEndY, leafVeinPaint);
    }

    private void drawBloomingFlower(Canvas canvas, float centerX, float centerY, float angleDeg, float size, float density) {
        // Draw 5 overlapping petals
        int petalCount = 5;
        float petalRadius = size * 0.55f;
        float petalDistance = size * 0.42f;

        for (int i = 0; i < petalCount; i++) {
            float petalAngle = angleDeg + (i * (360f / petalCount)) + (timeElapsed * 8f);
            float rad = (float) Math.toRadians(petalAngle);
            float px = centerX + (float) Math.sin(rad) * petalDistance;
            float py = centerY - (float) Math.cos(rad) * petalDistance;

            canvas.drawCircle(px, py, petalRadius, flowerPetalPaint);
        }

        // Center Pistil with bright warm gold
        canvas.drawCircle(centerX, centerY, size * 0.38f, flowerCenterPaint);
        // Inner highlight dot
        flowerCenterPaint.setColor(Color.parseColor("#FFF3B0"));
        canvas.drawCircle(centerX - (density * 1.5f), centerY - (density * 1.5f), size * 0.15f, flowerCenterPaint);
        flowerCenterPaint.setColor(Color.parseColor("#FFC857")); // restore
    }

    private void drawPot(Canvas canvas, float centerX, float topY, float density) {
        float potTopW = density * 42f;
        float potBottomW = density * 30f;
        float potH = density * 34f;
        float rimH = density * 7f;

        // Pot Body
        potPath.reset();
        potPath.moveTo(centerX - potTopW / 2f, topY);
        potPath.lineTo(centerX + potTopW / 2f, topY);
        potPath.lineTo(centerX + potBottomW / 2f, topY + potH);
        potPath.lineTo(centerX - potBottomW / 2f, topY + potH);
        potPath.close();
        canvas.drawPath(potPath, potPaint);

        // Pot Rim (Elevated lip)
        RectF rimRect = new RectF(centerX - (potTopW / 2f) - (density * 4f),
                topY - (density * 2f),
                centerX + (potTopW / 2f) + (density * 4f),
                topY + rimH);
        canvas.drawRoundRect(rimRect, density * 4f, density * 4f, potRimPaint);

        // Base shadow
        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.parseColor("#15110E"));
        shadowPaint.setStyle(Paint.Style.FILL);
        RectF shadowRect = new RectF(centerX - potBottomW * 0.7f,
                topY + potH - (density * 2f),
                centerX + potBottomW * 0.7f,
                topY + potH + (density * 4f));
        canvas.drawOval(shadowRect, shadowPaint);
    }

    private float getQuadPoint(float p0, float p1, float p2, float t) {
        float oneMinusT = 1.0f - t;
        return (oneMinusT * oneMinusT * p0) + (2f * oneMinusT * t * p1) + (t * t * p2);
    }
}
