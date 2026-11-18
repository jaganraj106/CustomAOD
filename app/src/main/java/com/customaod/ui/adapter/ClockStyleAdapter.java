package com.customaod.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.customaod.data.ClockStyle;
import com.customaod.databinding.ItemClockStyleBinding;

import java.util.function.Consumer;

/** Horizontal list of clock style cards; the selected one gets a neon outline. */
public class ClockStyleAdapter extends RecyclerView.Adapter<ClockStyleAdapter.VH> {

    private final ClockStyle[] styles = ClockStyle.values();
    private final Consumer<ClockStyle> onPick;
    private ClockStyle selected;
    private int accent;

    public ClockStyleAdapter(ClockStyle selected, int accent, Consumer<ClockStyle> onPick) {
        this.selected = selected;
        this.accent = accent;
        this.onPick = onPick;
    }

    public void setSelected(ClockStyle s, int accent) {
        if (s == selected && accent == this.accent) return;
        selected = s;
        this.accent = accent;
        notifyDataSetChanged();   // only 3 items
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemClockStyleBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ClockStyle s = styles[position];
        h.b.tvStyleName.setText(s.labelRes);
        h.b.tvPreview.setText(s == ClockStyle.STACKED ? "12\n45" : "12:45");
        h.b.tvPreview.setTextColor(accent);
        h.b.cardStyle.setStrokeColor(s == selected ? accent : 0xFF26262E);
        h.b.cardStyle.setStrokeWidth(s == selected ? 3 : 1);
        h.b.cardStyle.setOnClickListener(v -> onPick.accept(s));
    }

    @Override
    public int getItemCount() {
        return styles.length;
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemClockStyleBinding b;

        VH(ItemClockStyleBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
