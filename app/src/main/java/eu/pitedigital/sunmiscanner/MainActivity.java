package eu.pitedigital.sunmiscanner;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

public class MainActivity extends Activity {

    private SharedPreferences prefs;
    private EditText hostEdit, portEdit;
    private TextView logView;
    private RadioButton rbTcp, rbBt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("bridge", MODE_PRIVATE);
        hostEdit = (EditText) findViewById(R.id.host);
        portEdit = (EditText) findViewById(R.id.port);
        logView = (TextView) findViewById(R.id.log);
        rbTcp = (RadioButton) findViewById(R.id.tcp);
        rbBt = (RadioButton) findViewById(R.id.bt);

        hostEdit.setText(prefs.getString("host", ""));
        portEdit.setText(String.valueOf(prefs.getInt("port", 8989)));
        if ("bt".equals(prefs.getString("transport", "tcp"))) {
            rbBt.setChecked(true);
        } else {
            rbTcp.setChecked(true);
        }

        Button start = (Button) findViewById(R.id.start);
        start.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                save();
                startService(new Intent(MainActivity.this, ScanService.class));
                ScanSender.logLine("Szolgaltatas elinditva, varja a scan-t");
                logView.setText(ScanSender.getLog());
            }
        });

        Button test = (Button) findViewById(R.id.test);
        test.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                save();
                startService(new Intent(MainActivity.this, ScanService.class));
                if (rbBt.isChecked()) {
                    ScanBtServer.send("TEST-123");
                } else {
                    ScanSender.send(MainActivity.this, "TEST-123");
                }
            }
        });

        Button pair = (Button) findViewById(R.id.pair);
        pair.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    Intent di = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    di.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                    startActivity(di);
                } catch (Exception e) {
                    ScanSender.logLine("BT hiba: " + e.getMessage());
                }
            }
        });

        startService(new Intent(this, ScanService.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (logView != null) logView.setText(ScanSender.getLog());
    }

    private void save() {
        int p = 8989;
        try { p = Integer.parseInt(portEdit.getText().toString().trim()); } catch (Exception ignored) {}
        SharedPreferences.Editor e = prefs.edit();
        e.putString("host", hostEdit.getText().toString().trim());
        e.putInt("port", p);
        e.putString("transport", rbBt.isChecked() ? "bt" : "tcp");
        e.apply();
        ScanService.reload(this);
    }
}
