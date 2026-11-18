package com.customaod.ui.adapter;

import android.content.pm.PackageManager;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.customaod.data.NotificationItem;
import com.customaod.databinding.ItemNotifIconBinding;

/**
 * Small row of app icons (one per app with notifications).
 * Icons are drawn in dim grayscale: colour pixels are expensive on AMOLED
 * and a colourful static icon is a burn-in risk.
 */
public class NotifIconAdapter extends ListAdapter<NotificationItem, NotifIconAdapter.VH> {

    private static final ColorMatrixColorFilter GRAY;
    static {
        ColorMatrix m = new ColorMatrix();
        m.setSaturation(0f);
        GRAY = new ColorMatrixColorFilter(m);
    }

    private final PackageManager pm;

    public NotifIconAdapter(PackageManager pm) {
        super(new DiffUtil.ItemCallback<NotificationItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull NotificationItem a, @NonNull NotificationItem b) {
                return a.packageName.equals(b.packageName);
            }

            @Override
            public boolean areContentsTheSame(@NonNull NotificationItem a, @NonNull NotificationItem b) {
                return a.count == b.count;
            }
        });
        this.pm = pm;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemNotifIconBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        try {
            Drawable icon = pm.getApplicationIcon(getItem(position).packageName);
            h.b.getRoot().setImageDrawable(icon);
            h.b.getRoot().setColorFilter(GRAY);
            h.b.getRoot().setAlpha(0.7f);
        } catch (PackageManager.NameNotFoundException e) {
            h.b.getRoot().setImageDrawable(null);
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemNotifIconBinding b;

        VH(ItemNotifIconBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
