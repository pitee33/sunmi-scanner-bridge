package eu.pitedigital.sunmiscanner;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;

public class ScanService extends Service {

    public static final String SCAN_ACTION = "com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED";
    private BroadcastReceiver receiver;

    @Override
    public void onCreate() {
        super.onCreate();

        Notification.Builder nb;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel ch = new NotificationChannel("bridge", "Scan bridge", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(ch);
            nb = new Notification.Builder(this, "bridge");
        } else {
            nb = new Notification.Builder(this);
        }
        nb.setContentTitle("L2 Scan Bridge akt\u00edv")
          .setContentText("Vonalk\u00f3dok k\u00fcld\u00e9se a g\u00e9pre")
          .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
          .setOngoing(true);
        Notification n = (Build.VERSION.SDK_INT >= 26) ? nb.build() : nb.getNotification();
        startForeground(1, n);

        receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                String code = i.getStringExtra("data");
                if (code != null && !code.isEmpty()) {
                    ScanSender.logLine("SCAN " + code);
                    if ("bt".equals(ScanSender.transport)) {
                        ScanBtServer.send(code);
                    } else {
                        ScanSender.send(ScanService.this, code);
                    }
                }
            }
        };
        registerReceiver(receiver, new IntentFilter(SCAN_ACTION));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        reload(this);
        return START_STICKY;
    }

    public static void reload(Context c) {
        SharedPreferences p = c.getSharedPreferences("bridge", MODE_PRIVATE);
        ScanSender.host = p.getString("host", "");
        ScanSender.port = p.getInt("port", 8989);
        ScanSender.transport = p.getString("transport", "tcp");
        if ("bt".equals(ScanSender.transport)) {
            ScanBtServer.ensureStarted();
        }
    }

    @Override
    public void onDestroy() {
        if (receiver != null) unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
