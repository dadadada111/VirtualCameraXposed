package com.example.virtualcamera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private static final int REQUEST_CODE_PICK_VIDEO = 1001;
    private static final int REQUEST_CODE_PERMISSION = 1002;
    private static final String CONFIG_FILE_PATH = "/sdcard/virtual_camera_config.txt";
    private static final String LOG_FILE_PATH = "/sdcard/virtual_camera_log.txt";
    private static final String DEFAULT_VIDEO_PATH = "/sdcard/virtual_camera.mp4";

    private TextView tvVideoPath;
    private VideoView videoPreview;
    private TextView tvLogs;
    private Button btnSelectVideo;
    private Button btnRefreshLogs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvVideoPath = findViewById(R.id.tv_video_path);
        videoPreview = findViewById(R.id.video_preview);
        tvLogs = findViewById(R.id.tv_logs);
        btnSelectVideo = findViewById(R.id.btn_select_video);
        btnRefreshLogs = findViewById(R.id.btn_refresh_logs);

        checkPermissions();
        loadConfig();
        loadLogs();

        btnSelectVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*");
                startActivityForResult(intent, REQUEST_CODE_PICK_VIDEO);
            }
        });

        btnRefreshLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadLogs();
            }
        });
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        } else {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_VIDEO && resultCode == RESULT_OK && data != null) {
            Uri videoUri = data.getData();
            if (videoUri != null) {
                saveVideoToSdCard(videoUri);
            }
        }
    }

    private void saveVideoToSdCard(Uri sourceUri) {
        try {
            InputStream is = getContentResolver().openInputStream(sourceUri);
            File destFile = new File(DEFAULT_VIDEO_PATH);
            OutputStream os = new FileOutputStream(destFile);
            byte[] buffer = new byte[4096];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.close();
            is.close();
            
            // Update config to point to this file (redundant if we overwrite default, but good for future)
            saveConfig(DEFAULT_VIDEO_PATH);
            
            Toast.makeText(this, "Video saved to " + DEFAULT_VIDEO_PATH, Toast.LENGTH_SHORT).show();
            updateUI(DEFAULT_VIDEO_PATH);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save video: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveConfig(String path) {
        try {
            FileOutputStream fos = new FileOutputStream(CONFIG_FILE_PATH);
            fos.write(path.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        File configFile = new File(CONFIG_FILE_PATH);
        String path = DEFAULT_VIDEO_PATH;
        if (configFile.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(configFile));
                String line = br.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    path = line.trim();
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        updateUI(path);
    }

    private void updateUI(String path) {
        tvVideoPath.setText("Current Video: " + path);
        videoPreview.setVideoPath(path);
        videoPreview.start();
    }

    private void loadLogs() {
        File logFile = new File(LOG_FILE_PATH);
        if (!logFile.exists()) {
            tvLogs.setText("No logs found at " + LOG_FILE_PATH);
            return;
        }

        StringBuilder logs = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(logFile));
            String line;
            while ((line = br.readLine()) != null) {
                logs.append(line).append("\n");
            }
            br.close();
            
            // Scroll to bottom
            tvLogs.setText(logs.toString());
            // TODO: Auto scroll
        } catch (IOException e) {
            tvLogs.setText("Error reading logs: " + e.getMessage());
        }
    }
}
