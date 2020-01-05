package com.myapps.objectdetector;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Size;
import android.view.TextureView;

import java.io.IOException;
import java.util.List;

public class SurfaceTextureListener implements TextureView.SurfaceTextureListener {

    public static final Size PREVIEW_SIZE = new Size(640, 480);
    public static final int ORIENTATION = 90;

    private Camera camera;
    private Camera.PreviewCallback previewCallback;

    public SurfaceTextureListener(Camera.PreviewCallback previewCallback) {
        this.previewCallback = previewCallback;
    }

    @Override
    public void onSurfaceTextureAvailable(
            final SurfaceTexture texture, final int width, final int height) {
        int id = getCameraId();
        camera = Camera.open(id);

        try {
            Camera.Parameters parameters = camera.getParameters();
            List<String> focusModes = parameters.getSupportedFocusModes();

            if (focusModes != null
                    && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            parameters.setPreviewSize(PREVIEW_SIZE.getWidth(), PREVIEW_SIZE.getHeight());
            camera.setParameters(parameters);
            camera.setDisplayOrientation(ORIENTATION);
            camera.setPreviewTexture(texture);
        } catch (IOException exception) {
            camera.release();
        }

        camera.addCallbackBuffer(new byte[ImageUtils.getYUVByteSize(PREVIEW_SIZE)]);
        camera.setPreviewCallbackWithBuffer(previewCallback);
        startPreview();

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public void startPreview() {
        camera.startPreview();
    }

    public void stopCamera() {
        if (camera != null) {
            camera.stopPreview();
        }
    }

    private static int getCameraId() {
        Camera.CameraInfo ci = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, ci);
            if (ci.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return i;
            }
        }
        return -1;
    }
}
