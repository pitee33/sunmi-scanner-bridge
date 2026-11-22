package eu.pitedigital.sunmiscanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ScanBtServer extends Thread {

    // Standard SPP (Serial Port Profile) UUID
    public static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static ScanBtServer instance;
    private volatile OutputStream out;

    public static synchronized boolean isRunning() {
        return instance != null && instance.isAlive();
    }

    public static synchronized void ensureStarted() {
        if (isRunning()) return;
        instance = new ScanBtServer();
        instance.start();
        ScanSender.logLine("BT: szerver indul");
    }

    public static void send(String code) {
        ScanBtServer i = instance;
        if (i == null || i.out == null) {
            ScanSender.logLine("BT: nincs aktiv kapcsolat a geprel");
            return;
        }
        try {
            i.out.write((code + "\n").getBytes("UTF-8"));
            i.out.flush();
            ScanSender.logLine("OK(BT) " + code);
        } catch (Exception e) {
            ScanSender.logLine("BT kuldes hiba: " + e.getMessage());
            i.drop();
        }
    }

    private void drop() {
        out = null;
    }

    @Override
    public void run() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            ScanSender.logLine("BT: nincs Bluetooth a keszuleken");
            return;
        }
        while (true) {
            BluetoothServerSocket server = null;
            try {
                if (!adapter.isEnabled()) {
                    sleep(3000);
                    continue;
                }
                server = adapter.listenUsingRfcommWithServiceRecord("L2 Scan Bridge", SPP_UUID);
                ScanSender.logLine("BT: var a gepre (parositas utan COM port)");
                BluetoothSocket s = server.accept();
                out = s.getOutputStream();
                ScanSender.logLine("BT: kapcsolat: " + s.getRemoteDevice().getName());
                try {
                    InputStream in = s.getInputStream();
                    byte[] buf = new byte[256];
                    while (in.read(buf) != -1) {
                        // a gep fele jovo adatot nem hasznaljuk
                    }
                } finally {
                    out = null;
                    try { s.close(); } catch (Exception ignored) {}
                }
            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                // kapcsolat bontas vagy listen hiba: ujraprobaljuk
            } finally {
                out = null;
                if (server != null) {
                    try { server.close(); } catch (Exception ignored) {}
                }
            }
            try {
                sleep(1500);
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
