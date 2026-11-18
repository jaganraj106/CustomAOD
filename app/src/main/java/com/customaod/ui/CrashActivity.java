package com.customaod.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Shows a crash stack trace. Deliberately built in plain code with framework widgets only
 * (no Material theme, no ViewBinding), so it still works if the app's own theme/layouts
 * are what crashed. Runs in its own process (see manifest).
 */
public class CrashActivity extends Activity {

    public static final String EXTRA_TRACE = "trace";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String trace = getIntent().getStringExtra(EXTRA_TRACE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(32, 64, 32, 32);

        TextView title = new TextView(this);
        title.setText("CustomAOD crashed. Copy this and send it to the developer:");
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        root.addView(title);

        Button copy = new Button(this);
        copy.setText("Copy error");
        copy.setOnClickListener(v -> {
            ClipboardManager cm = getSystemService(ClipboardManager.class);
            cm.setPrimaryClip(ClipData.newPlainText("crash", trace));
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
        });
        root.addView(copy);

        ScrollView scroll = new ScrollView(this);
        TextView body = new TextView(this);
        body.setText(trace);
        body.setTextColor(Color.parseColor("#FF8A80"));
        body.setTextSize(11);
        body.setTextIsSelectable(true);
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }
}
