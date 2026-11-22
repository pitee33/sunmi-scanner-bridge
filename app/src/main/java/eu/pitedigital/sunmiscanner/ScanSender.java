package eu.pitedigital.sunmiscanner;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

public class ScanSender {

    public static volatile String host = "";
    public static volatile int port = 8989;

    private static final ArrayDeque<String> lines = new ArrayDeque<String>();
    private static final SimpleDateFormat ts = new SimpleDateFormat("HH:mm:ss", Locale.US);
    private static final Handler main = new Handler(Looper.getMainLooper());

    public static synchronized void logLine(String s) {
        lines.add(ts.format(new Date()) + "  " + s);
        while (lines.size() > 12) lines.removeFirst();
    }

    public static synchronized String getLog() {
        StringBuilder sb = new StringBuilder();
        for (String l : lines) sb.append(l).append('\n');
        return sb.toString();
    }

    public static void send(final Context ctx, final String code) {
        final String h = host;
        final int p = port;
        if (h == null || h.length() == 0) {
            logLine("HIBA: nincs gep IP beallitva");
            toast(ctx, "\u00c1ll\u00edtsd be a g\u00e9p IP-j\u00e9t!");
            return;
        }
        new Thread() {
            @Override public void run() {
                try {
                    Socket s = new Socket();
                    s.connect(new InetSocketAddress(h, p), 2500);
                    OutputStream o = s.getOutputStream();
                    o.write((code + "\n").getBytes("UTF-8"));
                    o.flush();
                    s.close();
                    logLine("OK   " + code);
                    toast(ctx, code);
                } catch (Exception e) {
                    logLine("HIBA " + e.getMessage());
                    toast(ctx, "Hiba: " + e.getMessage());
                }
            }
        }.start();
    }

    private static void toast(final Context ctx, final String msg) {
        main.post(new Runnable() {
            @Override public void run() {
                Toast.makeText(ctx.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
