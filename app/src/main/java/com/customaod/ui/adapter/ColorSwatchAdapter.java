package com.customaod.ui.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.customaod.databinding.ItemColorSwatchBinding;

import java.util.function.IntConsumer;

/** Row of colour circles for the clock colour. The selected swatch is enlarged. */
public class ColorSwatchAdapter extends RecyclerView.Adapter<ColorSwatchAdapter.VH> {

    /** Neon-leaning palette that stays readable on pure black. */
    public static final int[] PALETTE = {
            0xFF00E5FF, 0xFF4D7CFF, 0xFFFF2BD6, 0xFFB6FF3B,
            0xFFFF9A3D, 0xFFFF4D5E, 0xFFFFFFFF, 0xFFB0B0B0
    };

    private final IntConsumer onPick;
    private int selected;

    public ColorSwatchAdapter(int selected, IntConsumer onPick) {
        this.selected = selected;
        this.onPick = onPick;
    }

    public void setSelected(int color) {
        if (color == selected) return;
        selected = color;
        notifyDataSetChanged();   // 8 items
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemColorSwatchBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        int color = PALETTE[position];
        h.b.getRoot().setBackgroundTintList(ColorStateList.valueOf(color));
        float scale = color == selected ? 1.3f : 1f;
        h.b.getRoot().animate().scaleX(scale).scaleY(scale).setDuration(180).start();
        h.b.getRoot().setOnClickListener(v -> onPick.accept(color));
    }

    @Override
    public int getItemCount() {
        return PALETTE.length;
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemColorSwatchBinding b;

        VH(ItemColorSwatchBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
