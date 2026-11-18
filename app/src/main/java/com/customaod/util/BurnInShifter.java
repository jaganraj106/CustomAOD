package com.customaod.util;

import android.graphics.PointF;

import java.util.Random;

/**
 * Pure-math helper for burn-in protection (no Android views, easy to unit test).
 *
 * The AOD content is a single block that is centered by the layout. We never move
 * it with absolute coordinates; we only add a translation (dx, dy) relative to
 * that centered position. This helper picks the next (dx, dy) so that:
 *
 *   1. the block always stays at least `margin` pixels away from every screen edge;
 *   2. the new spot is clearly different from the previous one (min travel distance),
 *      otherwise a "random" shift could land on the same pixels and protect nothing.
 */
public final class BurnInShifter {

    private final Random random = new Random();

    /**
     * @param containerW  width of the screen / parent in px
     * @param containerH  height of the screen / parent in px
     * @param contentW    measured width of the content block in px
     * @param contentH    measured height of the content block in px
     * @param margin      safe margin from every edge in px
     * @param previous    previous offset (use 0,0 on first call)
     * @return next translation offset relative to the centered position
     */
    public PointF nextOffset(int containerW, int containerH, int contentW, int contentH,
                             int margin, PointF previous) {
        // Free space on each side of the centered block, after reserving the margin.
        // Math.max(0, ...) guards against content larger than the screen (never go negative).
        float rangeX = Math.max(0f, (containerW - contentW) / 2f - margin);
        float rangeY = Math.max(0f, (containerH - contentH) / 2f - margin);

        if (rangeX == 0f && rangeY == 0f) return new PointF(0f, 0f);

        // Require a minimum jump, scaled to the room we have. Try a handful of times.
        float minTravel = Math.min(rangeX, rangeY) * 0.35f;
        PointF best = new PointF(0f, 0f);
        for (int i = 0; i < 8; i++) {
            float dx = (random.nextFloat() * 2f - 1f) * rangeX;
            float dy = (random.nextFloat() * 2f - 1f) * rangeY;
            best.set(dx, dy);
            if (distance(best, previous) >= minTravel) break;
        }
        return best;
    }

    private static float distance(PointF a, PointF b) {
        float x = a.x - b.x;
        float y = a.y - b.y;
        return (float) Math.sqrt(x * x + y * y);
    }
}
