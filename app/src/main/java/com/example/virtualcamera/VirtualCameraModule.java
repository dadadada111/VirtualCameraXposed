package com.example.virtualcamera;

import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.util.Log;

import java.nio.ByteBuffer;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class VirtualCameraModule implements IXposedHookLoadPackage {
    private static final String TAG = "VirtualCamera";
    private static VideoDecoder videoDecoder;
    private static byte[] currentFrameData;
    private static final Object frameLock = new Object();
    
    // Hardcoded target package - modify as needed
    // private static final String TARGET_PACKAGE = "com.example.targetapp"; 

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // Filter packages if needed
        // if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;
        
        XposedBridge.log("VirtualCamera: Loaded package " + lpparam.packageName);

        // Initialize Video Decoder (Global singleton logic)
        synchronized (frameLock) {
            if (videoDecoder == null) {
                videoDecoder = new VideoDecoder();
                videoDecoder.startDecoding("/sdcard/virtual_camera.mp4", new VideoDecoder.FrameCallback() {
                    @Override
                    public void onFrameAvailable(byte[] data) {
                        synchronized (frameLock) {
                            currentFrameData = data;
                        }
                    }
                });
            }
        }

        // Hook ImageReader.acquireLatestImage and acquireNextImage
        // This ensures we intercept the image right after the app gets it, but before the app reads it.
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
        } catch (Throwable t) {
            XposedBridge.log("VirtualCamera: Error hooking ImageReader: " + t);
        }
        
        // TODO: Hook Camera1 API (Camera.setPreviewCallback) if needed
        // TODO: Hook Camera2 Surface output (harder, requires EGL/Surface texture manipulation)
    }

    private void replaceImageData(Image image) {
        synchronized (frameLock) {
            if (currentFrameData == null) return;
            
            try {
                Image.Plane[] planes = image.getPlanes();
                if (planes.length > 0) {
                    ByteBuffer buffer = planes[0].getBuffer();
                    // int width = image.getWidth();
                    // int height = image.getHeight();
                    
                    // Simple replacement
                    if (buffer.remaining() >= currentFrameData.length) {
                        buffer.position(0);
                        buffer.put(currentFrameData);
                        buffer.rewind(); 
                    }
                }
            } catch (Exception e) {
                XposedBridge.log("VirtualCamera: Error replacing image data: " + e);
            }
        }
    }
}
