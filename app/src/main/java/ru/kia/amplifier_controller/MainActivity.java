package ru.kia.amplifier_controller;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.google.android.material.slider.Slider;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Slider bassSlider, middleSlider, trebleSlider, volumeSlider, balanceSlider, faderSlider;
    int bassVol, midVol, trebVol, balanceVol, faderVol, volVol;
    private SharedPreferences sp;
    private Handler sendHandler;

    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB-COM готов", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "Права на использование USB-COM не получены", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "USB-COM не подключен", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB-COM отключен", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "Данный USB-COM не поддерживается", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private UsbService usbService;
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }

        @Override
        public void onBindingDied(ComponentName name) {
            usbService = null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        bassSlider = findViewById(R.id.bassSlider);
        bassSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                bassVol = Math.round(slider.getValue()) + 10;
                sp.edit().putInt("bas", bassVol).apply();
                chSett();
            }
        });

        middleSlider = findViewById(R.id.middleSlider);
        middleSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                midVol = Math.round(slider.getValue()) + 10;
                sp.edit().putInt("mid", midVol).apply();
                chSett();
            }
        });

        trebleSlider = findViewById(R.id.trebleSlider);
        trebleSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                trebVol = Math.round(slider.getValue()) + 10;
                sp.edit().putInt("tre", trebVol).apply();
                chSett();
            }
        });

        volumeSlider = findViewById(R.id.volumeSlider);
        volumeSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                volVol = Math.round(slider.getValue());
                sp.edit().putInt("vol", volVol).apply();
                chSett();
            }
        });

        balanceSlider = findViewById(R.id.balanceSlider);
        balanceSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                balanceVol = Math.round(slider.getValue()) + 10;
                sp.edit().putInt("bal", balanceVol).apply();
                chSett();
            }
        });

        faderSlider = findViewById(R.id.faderSlider);
        faderSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                faderVol = Math.round(slider.getValue()) + 10;
                sp.edit().putInt("fad", faderVol).apply();
                chSett();
            }
        });

        firstInit();

        sendHandler = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (usbService != null && !sp.getBoolean("oCanbus", false))
                    usbService.write((byte[]) msg.obj);
            }
        };
        chSett();

    }

    @Override
    protected void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(usbConnection); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    private void startService(ServiceConnection serviceConnection) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, UsbService.class);
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, UsbService.class);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    private void firstInit() {
        sp = PreferenceManager.getDefaultSharedPreferences(this);

        volVol = sp.getInt("vol", 17);
        volumeSlider.setValue(volVol);

        balanceVol = sp.getInt("bal", 10);
        balanceSlider.setValue(balanceVol - 10);

        faderVol = sp.getInt("fad", 10);
        faderSlider.setValue(faderVol - 10);

        bassVol = sp.getInt("bas", 10);
        bassSlider.setValue(bassVol - 10);

        midVol = sp.getInt("mid", 10);
        middleSlider.setValue(midVol - 10);

        trebVol = sp.getInt("tre", 10);
        trebleSlider.setValue(trebVol - 10);

    }

    private void chSett() {

        byte[] data = new byte[9];
        data[0] = (byte) (volVol & 255);
        data[1] = (byte) (balanceVol & 255);
        data[2] = (byte) (faderVol & 255);
        data[3] = (byte) (bassVol & 255);
        data[4] = (byte) (midVol & 255);
        data[5] = (byte) (trebVol & 255);
        data[6] = (byte) (255);
        data[7] = (byte) (255);

        int checkSumm = 0;
        for (int i = 0; i < 8; i++) {
            checkSumm = data[i] + checkSumm;
        }
        data[8] = (byte) ((checkSumm & 255) ^ 255);

        Message msg = new Message();
        msg.obj = data;
        sendHandler.sendMessageDelayed(msg, 100);
    }


}
