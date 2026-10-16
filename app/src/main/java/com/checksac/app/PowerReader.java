package com.checksac.app;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

/** Doc cong suat sac thuc te = dong sac (CURRENT_NOW) x dien ap pin. Rat nhe, khong giu tai nguyen. */
final class PowerReader {

    static final class Reading {
        boolean plugged;   // dang cam sac
        boolean valid;     // doc duoc dong + ap
        double watts;
        double milliAmps;
        double volts;
    }

    private PowerReader() {}

    static Reading read(Context ctx) {
        Reading r = new Reading();
        Intent b = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (b == null) return r;

        r.plugged = b.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;

        int mv = b.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
        if (mv > 0 && mv < 100) mv *= 1000; // mot so may bao theo V

        BatteryManager bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
        long cur = (bm != null)
                ? bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                : Long.MIN_VALUE;

        if (mv <= 0 || cur == Long.MIN_VALUE) return r;

        double ma = Math.abs((double) cur);
        // Da so may tra ve uA (vi du 5000000 = 5A). Neu gia tri nho thi coi la mA.
        if (ma >= 20000) ma /= 1000.0;

        r.milliAmps = ma;
        r.volts = mv / 1000.0;
        r.watts = (ma / 1000.0) * r.volts;
        r.valid = true;
        return r;
    }
}
