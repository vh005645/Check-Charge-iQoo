package com.checksac.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQ_NOTIF = 100;
    private static final int GREEN = 0xFF00C853;
    private static final int RED = 0xFFFF5252;

    private SharedPreferences prefs;
    private TextView toggle, tvState, tvPower, tvDetail, btnBattery;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable live = new Runnable() {
        @Override public void run() {
            updateLive();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(ChargeService.PREFS, MODE_PRIVATE);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xFF0D0F12);
        scroll.setOnApplyWindowInsetsListener((v, ins) -> {
            v.setPadding(ins.getSystemWindowInsetLeft(), ins.getSystemWindowInsetTop(),
                    ins.getSystemWindowInsetRight(), ins.getSystemWindowInsetBottom());
            return ins;
        });

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        scroll.addView(root, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        TextView title = text("⚡ Check Sạc", 30, 0xFFFFFFFF, true);
        root.addView(title, lp(-2, -2, 0));

        TextView sub = text("Đo công suất sạc thực tế", 14, 0xFF8A94A0, false);
        root.addView(sub, lp(-2, -2, dp(4)));

        tvPower = text("-- W", 48, 0xFFFFD60A, true);
        root.addView(tvPower, lp(-2, -2, dp(28)));

        tvDetail = text("", 14, 0xFF8A94A0, false);
        root.addView(tvDetail, lp(-2, -2, dp(2)));

        toggle = new TextView(this);
        toggle.setGravity(Gravity.CENTER);
        toggle.setTextSize(44);
        toggle.setTypeface(Typeface.DEFAULT_BOLD);
        toggle.setClickable(true);
        toggle.setOnClickListener(v -> onToggleClicked());
        root.addView(toggle, lp(dp(220), dp(220), dp(28)));

        tvState = text("", 16, 0xFFFFFFFF, true);
        root.addView(tvState, lp(-2, -2, dp(20)));

        TextView hint = text("Công suất hiện trên thanh trạng thái khi đang cắm sạc và màn hình bật.\n"
                + "Rút sạc hoặc tắt màn hình, ứng dụng tự nghỉ.", 13, 0xFF6B7580, false);
        root.addView(hint, lp(-2, -2, dp(10)));

        btnBattery = text("", 13, 0xFF9AA4AF, false);
        btnBattery.setPadding(dp(16), dp(10), dp(16), dp(10));
        GradientDrawable bd = new GradientDrawable();
        bd.setCornerRadius(dp(20));
        bd.setStroke(dp(1), 0xFF3A424B);
        btnBattery.setBackground(bd);
        btnBattery.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(this, "Không mở được cài đặt tối ưu pin", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(btnBattery, lp(-2, -2, dp(18)));

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean on = prefs.getBoolean(ChargeService.KEY_ENABLED, false);
        applyToggleUi(on);
        if (on) {
            try { ChargeService.start(this); } catch (Exception ignored) { }
        }
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean ignoring = pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        btnBattery.setText(ignoring
                ? "✓ Đã cho phép chạy nền ổn định"
                : "Cho phép chạy nền ổn định (tắt tối ưu pin)");
        handler.post(live);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(live);
        super.onPause();
    }

    private void onToggleClicked() {
        boolean now = !prefs.getBoolean(ChargeService.KEY_ENABLED, false);
        if (!now) {
            prefs.edit().putBoolean(ChargeService.KEY_ENABLED, false).apply();
            ChargeService.stop(this);
            applyToggleUi(false);
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
            return;
        }
        commitEnable();
    }

    private void commitEnable() {
        prefs.edit().putBoolean(ChargeService.KEY_ENABLED, true).apply();
        try { ChargeService.start(this); } catch (Exception ignored) { }
        applyToggleUi(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIF) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                commitEnable();
            } else {
                Toast.makeText(this,
                        "Cần cấp quyền thông báo để hiện công suất trên thanh trạng thái",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void applyToggleUi(boolean on) {
        String big = on ? "BẬT" : "TẮT";
        String small = on ? "\nĐang hoạt động\nNhấn để tắt" : "\nĐã vô hiệu hóa\nNhấn để bật";
        SpannableString ss = new SpannableString(big + small);
        ss.setSpan(new RelativeSizeSpan(0.34f), big.length(), ss.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        toggle.setText(ss);

        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        if (on) {
            g.setColor(GREEN);
            g.setStroke(dp(6), 0xFF69F0AE);
            toggle.setTextColor(0xFF04230F);
        } else {
            g.setColor(0xFF1B1F24);
            g.setStroke(dp(6), RED);
            toggle.setTextColor(0xFFFF8A80);
        }
        toggle.setBackground(g);

        tvState.setText(on ? "● Chức năng đang BẬT" : "● Chức năng đang TẮT");
        tvState.setTextColor(on ? GREEN : RED);
    }

    private void updateLive() {
        PowerReader.Reading r = PowerReader.read(this);
        if (!r.plugged) {
            tvPower.setText("-- W");
            tvDetail.setText("Chưa cắm sạc");
        } else if (!r.valid) {
            tvPower.setText("-- W");
            tvDetail.setText("Máy không cung cấp dòng sạc");
        } else {
            tvPower.setText(String.format(Locale.US, "%.1f W", r.watts));
            tvDetail.setText(String.format(Locale.US, "%d mA · %.2f V",
                    Math.round(r.milliAmps), r.volts));
        }
    }

    // ---- helpers ----
    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private LinearLayout.LayoutParams lp(int w, int h, int topMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.topMargin = topMargin;
        p.gravity = Gravity.CENTER_HORIZONTAL;
        return p;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
