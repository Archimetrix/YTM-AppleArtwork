package dev.archimetrix.applemusic;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

/**
 * Full-screen settings popup, opened from the YouTube Music settings list.
 * It is a Dialog on purpose: root installs cannot register new activities in the manifest.
 */
public final class SettingsUi {
    static final String ACTION = "dev.archimetrix.applemusic.SETTINGS";
    private static Intent lastIntent;

    /** Injected at the start of SettingsCompatActivity.startActivity(Intent). */
    public static boolean handleStartActivity(Activity a, Intent i) {
        try {
            if (i != null && ACTION.equals(i.getAction())) {
                new SettingsUi(a).show();
                return true;
            }
        } catch (Throwable t) {
            DebugLog.err("settings open", t);
            try { Toast.makeText(a, "Apple artwork settings failed: " + t, Toast.LENGTH_LONG).show(); } catch (Throwable ignored) { }
            return true; // never let the unresolvable action reach the system
        }
        lastIntent = i;
        return false;
    }

    public static Intent takeIntent() {
        Intent i = lastIntent;
        lastIntent = null;
        return i;
    }

    // ------------------------------------------------------------------

    private final Activity ctx;
    private Dialog dialog;
    private TextView logView;
    private ScrollView logScroll;

    private SettingsUi(Activity a) { ctx = a; }

    private int dp(int v) { return (int) (v * ctx.getResources().getDisplayMetrics().density); }

    private void show() {
        DebugLog.init(ctx.getApplicationContext());
        dialog = new Dialog(ctx, android.R.style.Theme_Material_NoActionBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(build(), new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dialog.show();
        Window w = dialog.getWindow();
        if (w != null) w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        refreshLog();
    }

    private View build() {
        final int bg = Color.parseColor("#0F0F0F");
        final int fg = Color.WHITE;
        final int sub = Color.parseColor("#AAAAAA");
        final int logBg = Color.parseColor("#1C1C1C");

        final LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);
        root.setPadding(dp(16), dp(36), dp(16), dp(12));
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override public WindowInsets onApplyWindowInsets(View v, WindowInsets in) {
                v.setPadding(dp(16), dp(12) + in.getSystemWindowInsetTop(), dp(16), dp(12) + in.getSystemWindowInsetBottom());
                return in;
            }
        });

        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = new TextView(ctx);
        title.setText("Apple Music artwork");
        title.setTextSize(22);
        title.setTextColor(fg);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button close = button("Close", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); } });
        close.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        head.addView(close);
        root.addView(head);

        final Switch sw = new Switch(ctx);
        sw.setText("Show Apple Music animated artwork");
        sw.setTextColor(fg);
        sw.setTextSize(16);
        sw.setPadding(0, dp(16), 0, dp(4));
        sw.setChecked(AppleMusicArtwork.isEnabled(ctx));
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton v, boolean on) {
                ctx.getSharedPreferences(AppleMusicArtwork.PREFS, Context.MODE_PRIVATE).edit().putBoolean(AppleMusicArtwork.KEY_ENABLED, on).apply();
                DebugLog.log("Setting changed: enabled=" + on);
                refreshLog();
            }
        });
        root.addView(sw);

        TextView desc = new TextView(ctx);
        desc.setText("Plays Apple Music motion artwork over the player thumbnail when a song has one. Takes effect immediately.");
        desc.setTextColor(sub);
        desc.setTextSize(13);
        root.addView(desc);

        TextView by = new TextView(ctx);
        by.setText("Built with \u2764\uFE0F by Archimetrix");
        by.setTextColor(fg);
        by.setTextSize(14);
        by.setPadding(0, dp(14), 0, 0);
        root.addView(by);

        TextView link = new TextView(ctx);
        link.setText("github.com/Archimetrix");
        link.setTextColor(Color.parseColor("#3EA6FF"));
        link.setTextSize(14);
        link.setPadding(0, dp(2), 0, 0);
        link.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            try { ctx.startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/Archimetrix"))); }
            catch (Throwable t) { Toast.makeText(ctx, "https://github.com/Archimetrix", Toast.LENGTH_LONG).show(); }
        } });
        root.addView(link);

        TextView logTitle = new TextView(ctx);
        logTitle.setText("Log");
        logTitle.setTextSize(18);
        logTitle.setTextColor(fg);
        logTitle.setTypeface(Typeface.DEFAULT_BOLD);
        logTitle.setPadding(0, dp(20), 0, dp(6));
        root.addView(logTitle);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(button("Copy", new View.OnClickListener() { @Override public void onClick(View v) {
            ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("Apple artwork log", logView.getText()));
            Toast.makeText(ctx, "Log copied", Toast.LENGTH_SHORT).show();
        } }));
        row.addView(button("Share", new View.OnClickListener() { @Override public void onClick(View v) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, logView.getText().toString());
            ctx.startActivity(Intent.createChooser(i, "Share log"));
        } }));
        row.addView(button("Refresh", new View.OnClickListener() { @Override public void onClick(View v) { refreshLog(); } }));
        row.addView(button("Clear", new View.OnClickListener() { @Override public void onClick(View v) {
            DebugLog.clear(ctx);
            refreshLog();
        } }));
        root.addView(row);

        row = new LinearLayout(ctx);
        row.addView(button("Clear artwork cache", new View.OnClickListener() { @Override public void onClick(View v) {
            File[] fs = new File(ctx.getCacheDir(), "apple_artwork").listFiles();
            int n = 0;
            if (fs != null) for (File f : fs) if (f.delete()) n++;
            ctx.getSharedPreferences("apple_artwork", Context.MODE_PRIVATE).edit().clear().apply();
            DebugLog.log("Cache cleared by user (" + n + " files)");
            Toast.makeText(ctx, "Cleared " + n + " cached videos", Toast.LENGTH_SHORT).show();
            refreshLog();
        } }));
        root.addView(row);

        logScroll = new ScrollView(ctx);
        logScroll.setBackgroundColor(logBg);
        logView = new TextView(ctx);
        logView.setTextIsSelectable(true);
        logView.setTextSize(10);
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setTextColor(fg);
        logView.setPadding(dp(8), dp(8), dp(8), dp(8));
        logScroll.addView(logView);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        lp.topMargin = dp(8);
        root.addView(logScroll, lp);
        return root;
    }

    private Button button(String t, View.OnClickListener l) {
        Button bt = new Button(ctx);
        bt.setText(t);
        bt.setAllCaps(false);
        bt.setTextSize(12);
        bt.setOnClickListener(l);
        bt.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return bt;
    }

    private void refreshLog() {
        if (logView == null) return;
        logView.setText(DebugLog.read(ctx));
        logScroll.post(new Runnable() { @Override public void run() { logScroll.fullScroll(View.FOCUS_DOWN); } });
    }
}
