package com.checksac.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import java.util.Locale;

/**
 * Dich vu foreground cuc nhe:
 *  - Cam sac + man hinh bat  -> do moi 2 giay, hien "xxW" tren thanh trang thai (icon thong bao).
 *  - Rut sac hoac tat man hinh -> dung do hoan toan, an icon (khong ton CPU/pin).
 */
public class ChargeService extends Service {

    static final String PREFS = "cfg";
    static final String KEY_ENABLED = "enabled";

    private static final String CH_POWER = "power_v1";
    private static final String CH_IDLE = "idle_v1";
    private static final int NID = 1;
    private static final long POLL_MS = 2000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private NotificationManager nm;
    private PendingIntent openApp;
    private boolean screenOn = true;
    private boolean foregroundStarted = false;
    private String lastKey = "";

    private final Runnable tick = new Runnable() {
        @Override public void run() { refresh(); }
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String a = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(a)) screenOn = false;
            else if (Intent.ACTION_SCREEN_ON.equals(a)) screenOn = true;
            refresh();
        }
    };

    static void start(Context c) {
        c.startForegroundService(new Intent(c, ChargeService.class));
    }

    static void stop(Context c) {
        c.stopService(new Intent(c, ChargeService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createChannels();
        openApp = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        screenOn = pm == null || pm.isInteractive();

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        f.addAction(Intent.ACTION_POWER_CONNECTED);
        f.addAction(Intent.ACTION_POWER_DISCONNECTED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, f);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Bat buoc goi startForeground ngay sau startForegroundService
        if (!foregroundStarted) {
            Notification n = idleNotification();
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NID, n);
            }
            foregroundStarted = true;
            lastKey = "idle";
        }

        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!p.getBoolean(KEY_ENABLED, false)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        refresh();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tick);
        try { unregisterReceiver(receiver); } catch (Exception ignored) { }
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    // ------------------------------------------------------------------

    private void refresh() {
        handler.removeCallbacks(tick);
        PowerReader.Reading r = PowerReader.read(this);
        boolean active = r.plugged && screenOn;
        if (active) {
            showPower(r);
            handler.postDelayed(tick, POLL_MS);   // chi lap khi dang sac + man hinh bat
        } else {
            showIdle();                           // khong lap -> 0% CPU
        }
    }

    private void showPower(PowerReader.Reading r) {
        String key = r.valid ? String.format(Locale.US, "p%.1f", r.watts) : "p?";
        if (key.equals(lastKey)) return;
        lastKey = key;

        String label;
        String title;
        String text;
        if (r.valid) {
            label = Math.round(r.watts) + "W";
            title = String.format(Locale.US, "Đang sạc: %.1f W", r.watts);
            text = String.format(Locale.US, "%d mA · %.2f V", Math.round(r.milliAmps), r.volts);
        } else {
            label = "?W";
            title = "Đang sạc (không đọc được dòng sạc)";
            text = "Máy không cung cấp thông tin dòng sạc";
        }

        Notification n = new Notification.Builder(this, CH_POWER)
                .setSmallIcon(textIcon(label))
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .setShowWhen(false)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(openApp)
                .build();
        nm.notify(NID, n);
    }

    private void showIdle() {
        if ("idle".equals(lastKey)) return;
        lastKey = "idle";
        nm.notify(NID, idleNotification());
    }

    private Notification idleNotification() {
        return new Notification.Builder(this, CH_IDLE)
                .setSmallIcon(R.drawable.ic_bolt)
                .setContentTitle("Check Sạc đang chờ")
                .setContentText("Cắm sạc và bật màn hình để xem công suất")
                .setOngoing(true)
                .setShowWhen(false)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(openApp)
                .build();
    }

    private void createChannels() {
        NotificationChannel power = new NotificationChannel(
                CH_POWER, "Công suất sạc (hiện trên thanh trạng thái)",
                NotificationManager.IMPORTANCE_LOW);
        power.setShowBadge(false);
        power.setSound(null, null);
        power.enableVibration(false);
        power.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(power);

        NotificationChannel idle = new NotificationChannel(
                CH_IDLE, "Chờ cắm sạc (ẩn)",
                NotificationManager.IMPORTANCE_MIN);
        idle.setShowBadge(false);
        idle.setSound(null, null);
        idle.enableVibration(false);
        nm.createNotificationChannel(idle);
    }

    /** Ve chu (vi du "50W") thanh icon trang de hien tren thanh trang thai. */
    private static Icon textIcon(String text) {
        final int size = 96;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.WHITE);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setTextAlign(Paint.Align.CENTER);

        p.setTextSize(100f);
        float w = p.measureText(text);
        float fit = 100f * (size - 2f) / w;
        p.setTextSize(Math.min(fit, 78f));

        float y = size / 2f - (p.descent() + p.ascent()) / 2f;
        c.drawText(text, size / 2f, y, p);
        return Icon.createWithBitmap(bmp);
    }
}
