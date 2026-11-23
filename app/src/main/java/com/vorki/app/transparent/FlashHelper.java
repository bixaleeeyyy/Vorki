package com.vorki.app.transparent;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;

public final class FlashHelper {

    private static volatile boolean torchOn = false;
    private static boolean registered = false;

    private FlashHelper() {
    }

    /** Keeps track of whether the torch is on, even if it was switched on by another app. */
    public static synchronized void register(Context context) {
        if (registered) {
            return;
        }
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) {
            return;
        }
        try {
            manager.registerTorchCallback(new CameraManager.TorchCallback() {
                @Override
                public void onTorchModeChanged(String cameraId, boolean enabled) {
                    torchOn = enabled;
                }
            }, null);
            registered = true;
        } catch (RuntimeException ignored) {
        }
    }

    /** Returns false if this phone has no usable flashlight. */
    public static boolean toggle(Context context) {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) {
            return false;
        }
        try {
            String cameraId = findTorchCamera(manager);
            if (cameraId == null) {
                return false;
            }
            boolean turnOn = !torchOn;
            manager.setTorchMode(cameraId, turnOn);
            torchOn = turnOn;
            return true;
        } catch (CameraAccessException | RuntimeException e) {
            return false;
        }
    }

    private static String findTorchCamera(CameraManager manager) throws CameraAccessException {
        for (String id : manager.getCameraIdList()) {
            CameraCharacteristics c = manager.getCameraCharacteristics(id);
            Boolean hasFlash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            Integer facing = c.get(CameraCharacteristics.LENS_FACING);
            if (Boolean.TRUE.equals(hasFlash) && facing != null
                    && facing == CameraCharacteristics.LENS_FACING_BACK) {
                return id;
            }
        }
        return null;
    }
}
