package com.customaod.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Image helpers for the custom logo. */
public final class ImageUtils {

    private static final int MAX_LOGO_PX = 512;

    private ImageUtils() { }

    /**
     * Decodes the picked image at reduced size and stores it as PNG in app-private storage.
     * Copying (instead of keeping the content Uri) means no persistent permission is needed
     * and the logo still works if the original is deleted. Call from a background thread.
     */
    public static boolean saveScaledLogo(Context context, Uri source, File target) {
        try {
            // Pass 1: read only the dimensions.
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = context.getContentResolver().openInputStream(source)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            if (bounds.outWidth <= 0) return false;

            // Pass 2: decode with a power-of-two sample size to keep memory small.
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_LOGO_PX);
            Bitmap bmp;
            try (InputStream in = context.getContentResolver().openInputStream(source)) {
                bmp = BitmapFactory.decodeStream(in, null, opts);
            }
            if (bmp == null) return false;

            try (FileOutputStream out = new FileOutputStream(target)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            bmp.recycle();
            return true;
        } catch (IOException | SecurityException e) {
            return false;
        }
    }

    private static int sampleSize(int w, int h, int max) {
        int sample = 1;
        while (w / (sample * 2) >= max || h / (sample * 2) >= max) sample *= 2;
        return sample;
    }
}
