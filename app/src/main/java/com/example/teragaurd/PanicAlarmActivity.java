package com.example.teragaurd;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class PanicAlarmActivity extends AppCompatActivity {

    private boolean isAlarmActive = false;
    private MaterialButton btnTriggerAlarm;
    private TextView lblStatus;

    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private Vibrator vibrator;
    private CameraManager cameraManager;
    private String cameraId;

    private Handler flashHandler = new Handler(Looper.getMainLooper());
    private boolean isFlashOn = false;
    private int originalVolume = -1;

    private final Runnable flashRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAlarmActive && cameraId != null && cameraManager != null) {
                try {
                    isFlashOn = !isFlashOn;
                    cameraManager.setTorchMode(cameraId, isFlashOn);
                    flashHandler.postDelayed(this, 300); // 300ms flash toggle
                } catch (CameraAccessException e) {
                    Log.e("PanicAlarm", "Failed to access camera", e);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_panic_alarm);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnTriggerAlarm = findViewById(R.id.btnTriggerAlarm);
        lblStatus = findViewById(R.id.lblStatus);

        btnBack.setOnClickListener(v -> finish());
        btnTriggerAlarm.setOnClickListener(v -> toggleAlarm());

        // Initialize Services
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        // Prep Camera for Flash
        try {
            if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) && cameraManager != null) {
                cameraId = cameraManager.getCameraIdList()[0];
            }
        } catch (CameraAccessException | ArrayIndexOutOfBoundsException e) {
            Log.e("PanicAlarm", "Failed to get camera ID", e);
            cameraId = null;
        }

        // Prep Media Player
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, alarmUri);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
        } catch (Exception e) {
            Log.e("PanicAlarm", "Failed to set up media player", e);
        }
    }

    private void toggleAlarm() {
        isAlarmActive = !isAlarmActive;
        if (isAlarmActive) {
            startAlarm();
        } else {
            stopAlarm();
        }
    }

    private void startAlarm() {
        lblStatus.setText("ALARM IS ACTIVE");
        lblStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light, null));

        // Max Volume
        if (audioManager != null) {
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0);
        }

        // Start Sound
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }

        // Start Vibrate
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 500, 500}, 0));
        }

        // Start Flash
        if (cameraId != null) {
            flashHandler.post(flashRunnable);
        } else {
            Toast.makeText(this, "Flashlight not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopAlarm() {
        lblStatus.setText("ALARM IS OFF");
        lblStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));

        // Restore Volume
        if (audioManager != null && originalVolume != -1) {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0);
        }

        // Stop Sound
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
        }

        // Stop Vibrate
        if (vibrator != null) {
            vibrator.cancel();
        }

        // Stop Flash
        flashHandler.removeCallbacks(flashRunnable);
        try {
            if (cameraId != null && cameraManager != null) {
                cameraManager.setTorchMode(cameraId, false);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        isFlashOn = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isAlarmActive) {
            stopAlarm();
            isAlarmActive = false;
        }
    }

    @Override
    protected void onDestroy() {
        if (isAlarmActive) {
            stopAlarm();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }
}
