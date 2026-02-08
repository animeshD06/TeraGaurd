package com.example.teragaurd;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

/**
 * Helper class for emergency mode settings
 * Manages flashlight, volume, and vibration for SOS features
 */
public class EmergencySettingsHelper {

    private static final String TAG = "EmergencySettingsHelper";
    
    private Context context;
    private CameraManager cameraManager;
    private String cameraId;
    private AudioManager audioManager;
    private Vibrator vibrator;
    
    private boolean isFlashlightOn = false;
    private boolean isEmergencyModeActive = false;
    private int originalVolume = -1;
    
    // SOS Pattern: ... --- ... (3 short, 3 long, 3 short)
    private static final long[] SOS_PATTERN = {
        0,    // Start immediately
        200, 200, 200, 200, 200, 200,  // 3 short (dot)
        500, 200, 500, 200, 500, 200,  // 3 long (dash)
        200, 200, 200, 200, 200, 0     // 3 short (dot)
    };

    public EmergencySettingsHelper(Context context) {
        this.context = context;
        initializeComponents();
    }

    private void initializeComponents() {
        // Initialize Camera Manager for flashlight
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (cameraManager != null) {
                for (String id : cameraManager.getCameraIdList()) {
                    CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                    Boolean hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    if (hasFlash != null && hasFlash) {
                        cameraId = id;
                        break;
                    }
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to access camera: " + e.getMessage());
        }

        // Initialize Audio Manager
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // Initialize Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vibratorManager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    // ==================== FLASHLIGHT CONTROLS ====================

    /**
     * Toggle flashlight on/off
     * @return Current flashlight state after toggle
     */
    public boolean toggleFlashlight() {
        if (isFlashlightOn) {
            turnOffFlashlight();
        } else {
            turnOnFlashlight();
        }
        return isFlashlightOn;
    }

    /**
     * Turn on the flashlight
     */
    public void turnOnFlashlight() {
        if (cameraId == null || cameraManager == null) {
            Log.e(TAG, "Flashlight not available");
            return;
        }
        
        try {
            cameraManager.setTorchMode(cameraId, true);
            isFlashlightOn = true;
            Log.d(TAG, "Flashlight turned ON");
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to turn on flashlight: " + e.getMessage());
        }
    }

    /**
     * Turn off the flashlight
     */
    public void turnOffFlashlight() {
        if (cameraId == null || cameraManager == null) {
            return;
        }
        
        try {
            cameraManager.setTorchMode(cameraId, false);
            isFlashlightOn = false;
            Log.d(TAG, "Flashlight turned OFF");
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to turn off flashlight: " + e.getMessage());
        }
    }

    /**
     * Flash SOS pattern using flashlight
     * Runs on a background thread
     */
    public void flashSOSPattern() {
        new Thread(() -> {
            try {
                // SOS: 3 short, 3 long, 3 short
                // Short flash = 200ms, Long flash = 600ms, Gap = 200ms
                
                // 3 short flashes (S)
                for (int i = 0; i < 3; i++) {
                    turnOnFlashlight();
                    Thread.sleep(200);
                    turnOffFlashlight();
                    Thread.sleep(200);
                }
                
                Thread.sleep(400); // Gap between letters
                
                // 3 long flashes (O)
                for (int i = 0; i < 3; i++) {
                    turnOnFlashlight();
                    Thread.sleep(600);
                    turnOffFlashlight();
                    Thread.sleep(200);
                }
                
                Thread.sleep(400); // Gap between letters
                
                // 3 short flashes (S)
                for (int i = 0; i < 3; i++) {
                    turnOnFlashlight();
                    Thread.sleep(200);
                    turnOffFlashlight();
                    Thread.sleep(200);
                }
                
            } catch (InterruptedException e) {
                Log.e(TAG, "SOS pattern interrupted: " + e.getMessage());
                turnOffFlashlight();
            }
        }).start();
    }

    /**
     * Check if flashlight is currently on
     */
    public boolean isFlashlightOn() {
        return isFlashlightOn;
    }

    /**
     * Check if device has flashlight
     */
    public boolean hasFlashlight() {
        return cameraId != null;
    }

    // ==================== VOLUME CONTROLS ====================

    /**
     * Set device to maximum volume for emergency alerts
     */
    public void setMaxVolume() {
        if (audioManager == null) return;
        
        // Save original volume
        if (originalVolume == -1) {
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
        }
        
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVolume, AudioManager.FLAG_SHOW_UI);
        
        // Also set alarm volume to max
        int maxAlarmVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVolume, 0);
        
        Log.d(TAG, "Volume set to maximum");
    }

    /**
     * Restore original volume
     */
    public void restoreVolume() {
        if (audioManager == null || originalVolume == -1) return;
        
        audioManager.setStreamVolume(AudioManager.STREAM_RING, originalVolume, AudioManager.FLAG_SHOW_UI);
        originalVolume = -1;
        
        Log.d(TAG, "Volume restored to original");
    }

    /**
     * Get current volume percentage
     */
    public int getVolumePercentage() {
        if (audioManager == null) return 0;
        
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        
        return (int) ((currentVolume / (float) maxVolume) * 100);
    }

    /**
     * Check if device is on silent mode
     */
    public boolean isSilentMode() {
        if (audioManager == null) return false;
        return audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT;
    }

    /**
     * Set ringer mode to normal (enables sound)
     */
    public void enableSound() {
        if (audioManager == null) return;
        audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
    }

    // ==================== VIBRATION CONTROLS ====================

    /**
     * Vibrate device for emergency alert
     * @param durationMs Duration in milliseconds
     */
    public void vibrate(long durationMs) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(durationMs);
        }
    }

    /**
     * Vibrate SOS pattern
     */
    public void vibrateSOSPattern() {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(SOS_PATTERN, -1));
        } else {
            vibrator.vibrate(SOS_PATTERN, -1);
        }
    }

    /**
     * Cancel any ongoing vibration
     */
    public void cancelVibration() {
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    // ==================== EMERGENCY MODE ====================

    /**
     * Activate emergency mode - sets max volume, enables sound, and flashes SOS
     */
    public void activateEmergencyMode() {
        isEmergencyModeActive = true;
        
        // Set maximum volume
        enableSound();
        setMaxVolume();
        
        // Vibrate to alert
        vibrateSOSPattern();
        
        // Flash SOS pattern
        flashSOSPattern();
        
        Log.d(TAG, "Emergency mode ACTIVATED");
    }

    /**
     * Deactivate emergency mode - restores settings
     */
    public void deactivateEmergencyMode() {
        isEmergencyModeActive = false;
        
        // Restore volume
        restoreVolume();
        
        // Stop vibration
        cancelVibration();
        
        // Turn off flashlight
        turnOffFlashlight();
        
        Log.d(TAG, "Emergency mode DEACTIVATED");
    }

    /**
     * Check if emergency mode is active
     */
    public boolean isEmergencyModeActive() {
        return isEmergencyModeActive;
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        deactivateEmergencyMode();
    }
}
