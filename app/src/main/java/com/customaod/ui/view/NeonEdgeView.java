package com.customaod.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Funtouch-style neon edge lighting.
 *
 * A thin glowing line follows the screen outline. A bright "comet" of colour slowly
 * travels around it, while the rest of the outline stays almost off.
 *
 * AMOLED / burn-in notes:
 *  - the line is drawn only while charging (the owner calls setActive);
 *  - the light is always moving, so no pixel stays lit for long;
 *  - when battery is low, animation is paused AND the line is hidden: a frozen
 *    bright outline would be the worst thing to leave on a panel.
 */
public class NeonEdgeView extends View {

    private static final long ROTATION_MS = 9000;

    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint core = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Matrix matrix = new Matrix();
    private SweepGradient gradient;

    private final float density;
    private ValueAnimator rotator;
    private float angle;

    private boolean active = false;     // charging + enabled in settings
    private boolean paused = false;     // low battery
    private int color = Color.parseColor("#00E5FF");

    public NeonEdgeView(Context c) { this(c, null); }

    public NeonEdgeView(Context c, AttributeSet a) {
        super(c, a);
        density = getResources().getDisplayMetrics().density;
        glow.setStyle(Paint.Style.STROKE);
        core.setStyle(Paint.Style.STROKE);
        glow.setStrokeWidth(10 * density);
        core.setStrokeWidth(2 * density);
        // Software blur is cheap here because the view redraws only while the comet moves.
        glow.setMaskFilter(new android.graphics.BlurMaskFilter(8 * density,
                android.graphics.BlurMaskFilter.Blur.NORMAL));
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        setAlpha(0f);
    }

    public void setNeonColor(int c) {
        color = c;
        buildGradient();
        invalidate();
    }

    /** Charging on/off. Fades the whole effect in or out. */
    public void setActive(boolean on) {
        active = on;
        update();
    }

    /** Pause everything (battery below 15%). */
    public void setPaused(boolean p) {
        paused = p;
        update();
    }

    private void update() {
        boolean show = active && !paused;
        if (show) {
            animate().alpha(0.9f).setDuration(900).start();
            startRotation();
        } else {
            animate().alpha(0f).setDuration(600).withEndAction(this::stopRotation).start();
        }
    }

    private void startRotation() {
        if (rotator != null && rotator.isRunning()) return;
        rotator = ValueAnimator.ofFloat(0f, 360f);
        rotator.setDuration(ROTATION_MS);
        rotator.setInterpolator(new LinearInterpolator());
        rotator.setRepeatCount(ValueAnimator.INFINITE);
        rotator.addUpdateListener(a -> {
            angle = (float) a.getAnimatedValue();
            invalidate();
        });
        rotator.start();
    }

    private void stopRotation() {
        if (rotator != null) {
            rotator.cancel();
            rotator = null;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        float inset = 3 * density;
        rect.set(inset, inset, w - inset, h - inset);
        buildGradient();
    }

    /** Mostly transparent gradient with one bright tail (the comet). */
    private void buildGradient() {
        if (rect.isEmpty()) return;
        int bright = color;
        int mid = Color.argb(110, Color.red(color), Color.green(color), Color.blue(color));
        int off = Color.argb(0, Color.red(color), Color.green(color), Color.blue(color));
        gradient = new SweepGradient(rect.centerX(), rect.centerY(),
                new int[]{off, off, mid, bright, off},
                new float[]{0f, 0.55f, 0.8f, 0.97f, 1f});
        glow.setShader(gradient);
        core.setShader(gradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (gradient == null) return;
        matrix.setRotate(angle, rect.centerX(), rect.centerY());
        gradient.setLocalMatrix(matrix);
        // Corner radius approximates a modern rounded display.
        float r = 36 * density;
        canvas.drawRoundRect(rect, r, r, glow);
        canvas.drawRoundRect(rect, r, r, core);
    }

    @Override
    protected void onDetachedFromWindow() {
        stopRotation();
        super.onDetachedFromWindow();
    }
}
