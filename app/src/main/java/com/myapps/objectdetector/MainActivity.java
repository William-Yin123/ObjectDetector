package com.myapps.objectdetector;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.HandlerThread;
import android.util.Log;
import android.view.TextureView;
import android.view.WindowManager;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "ObjectDetector";

    private static final int PERMISSIONS_REQUEST = 1;
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;

    private TextureView textureView;
    private OverlayView overlayView;

    private SurfaceTextureListener surfaceTextureListener;
    private HandlerThread backgroundThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        textureView = findViewById(R.id.textureView);
        overlayView = findViewById(R.id.overlayView);

        CameraCallBackListener cameraCallBackListener = new CameraCallBackListener(
                new Detector(this),
                overlayView
        );
        surfaceTextureListener = new SurfaceTextureListener(cameraCallBackListener);

        if (!hasPermission()) {
            requestPermission();
        } else {
            startCameraPreview();
        }
    }

    private void startCameraPreview() {
        if (textureView.isAvailable()) {
            surfaceTextureListener.startPreview();
        } else {
            startBackgroundThread();
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    private void stopCameraPreview() {
        surfaceTextureListener.stopCamera();
        stopBackgroundThread();
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final String[] permissions,
                                          final int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (allPermissionsGranted(grantResults)) {
                startCameraPreview();
            } else {
                requestPermission();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startCameraPreview();
    }

    @Override
    public void onPause() {
        stopCameraPreview();
        super.onPause();
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
            } catch (InterruptedException exception) {
                Log.e(TAG, exception.getMessage(), exception);
            }
        }
    }

    private static boolean allPermissionsGranted(final int[] grantResults) {
        return grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        requestPermissions(new String[] {PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
    }

    private boolean hasPermission() {
        return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
}
