package com.example.virtualcamera;

import android.hardware.camera2.CameraDevice;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class VirtualCameraModule implements IXposedHookLoadPackage {
    private static final String TAG = "VirtualCamera";
    private static final String CONFIG_FILE_PATH = "/sdcard/virtual_camera_config.txt";
    private static final String LOG_FILE_PATH = "/sdcard/virtual_camera_log.txt";
    private static final String DEFAULT_VIDEO_PATH = "/sdcard/virtual_camera.mp4";

    private static VideoDecoder videoDecoder;
    private static byte[] currentFrameData;
    private static final Object frameLock = new Object();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // Log package load to verify module is active
        // Only log for interesting packages or system to avoid spam
        // if (lpparam.packageName.equals("android")) return; 
        
        logToFile("Loaded package: " + lpparam.packageName);

        // Hook CameraManager.openCamera to detect when camera is accessed
        try {
            XposedHelpers.findAndHookMethod("android.hardware.camera2.CameraManager", lpparam.classLoader, 
                "openCamera", String.class, "android.hardware.camera2.CameraDevice$StateCallback", Handler.class, 
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String cameraId = (String) param.args[0];
                        logToFile("CameraManager.openCamera called for ID: " + cameraId + " in " + lpparam.packageName);
                        
                        // Initialize decoder here if not already
                        initDecoder();
                    }
                });
        } catch (Throwable t) {
            logToFile("Failed to hook CameraManager.openCamera: " + t);
        }

        // Hook ImageReader to intercept frames
        XC_MethodHook imageAcquireHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Image image = (Image) param.getResult();
                if (image != null) {
                    replaceImageData(image);
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(ImageReader.class, "acquireLatestImage", imageAcquireHook);
            XposedHelpers.findAndHookMethod(ImageReader.class, "acquireNextImage", imageAcquireHook);
            logToFile("Hooked ImageReader successfully in " + lpparam.packageName);
        } catch (Throwable t) {
            logToFile("Error hooking ImageReader: " + t);
        }
    }

    private void initDecoder() {
        synchronized (frameLock) {
            if (videoDecoder == null) {
                String videoPath = getVideoPath();
                logToFile("Initializing decoder with video: " + videoPath);
                
                videoDecoder = new VideoDecoder();
                videoDecoder.startDecoding(videoPath, new VideoDecoder.FrameCallback() {
                    @Override
                    public void onFrameAvailable(byte[] data) {
                        synchronized (frameLock) {
                            currentFrameData = data;
                        }
                    }
                });
            }
        }
    }

    private String getVideoPath() {
        File configFile = new File(CONFIG_FILE_PATH);
        if (configFile.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(configFile));
                String line = br.readLine();
                br.close();
                if (line != null && !line.trim().isEmpty()) {
                    return line.trim();
                }
            } catch (IOException e) {
                logToFile("Error reading config: " + e);
            }
        }
        return DEFAULT_VIDEO_PATH;
    }

    private void replaceImageData(Image image) {
        synchronized (frameLock) {
            if (currentFrameData == null) return;
            
            try {
                Image.Plane[] planes = image.getPlanes();
                if (planes.length > 0) {
                    ByteBuffer buffer = planes[0].getBuffer();
                    if (buffer.remaining() >= currentFrameData.length) {
                        buffer.position(0);
                        buffer.put(currentFrameData);
                        buffer.rewind(); 
                        // Optional: log every N frames to avoid spam, or just first frame
                        // logToFile("Replaced frame!"); 
                    }
                }
            } catch (Exception e) {
                logToFile("Error replacing image data: " + e);
            }
        }
    }

    private static void logToFile(String message) {
        XposedBridge.log(TAG + ": " + message); // Also log to Xposed standard log
        try {
            File logFile = new File(LOG_FILE_PATH);
            FileOutputStream fos = new FileOutputStream(logFile, true);
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            String logMsg = timestamp + ": " + message + "\n";
            fos.write(logMsg.getBytes());
            fos.close();
        } catch (IOException e) {
            // Ignore log errors
        }
    }
}
