package com.checksac.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/** Khoi dong lai dich vu sau khi may khoi dong / cap nhat app, neu cong tac dang BAT. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences p = context.getSharedPreferences(ChargeService.PREFS, Context.MODE_PRIVATE);
        if (p.getBoolean(ChargeService.KEY_ENABLED, false)) {
            try {
                ChargeService.start(context);
            } catch (Exception ignored) {
            }
        }
    }
}
