package com.myapps.objectdetector;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.util.Log;

import java.util.List;

public class CameraCallBackListener implements Camera.PreviewCallback {

    private boolean isProcessingFrame;
    private int[] rgbBytes;
    private int previewHeight, previewWidth;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private static final int CROP_SIZE = 300;

    private Detector detector;
    private OverlayView overlayView;

    public CameraCallBackListener(Detector detector, OverlayView overlayView) {
        this.overlayView = overlayView;
        this.detector = detector;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (isProcessingFrame) {
            return;
        }

        isProcessingFrame = true;
        if (rgbBytes == null) {
            init(camera);
        }

        obtainCroppedImage(data);
        detectInBackground(data, camera);
    }

    private void init(Camera camera) {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        previewHeight = previewSize.height;
        previewWidth = previewSize.width;

        rgbBytes = new int[previewWidth * previewHeight];

        frameToCropTransform = ImageUtils.getTransformationMatrix(
                previewWidth,
                previewHeight,
                CROP_SIZE,
                CROP_SIZE,
                90,
                false
        );

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        croppedBitmap = Bitmap.createBitmap(CROP_SIZE, CROP_SIZE, Bitmap.Config.ARGB_8888);
    }

    private void obtainCroppedImage(final byte[] bytes) {
        ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        rgbFrameBitmap.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight);

        Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
    }

    private void detectInBackground(final byte[] bytes, final Camera camera) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Recognition> recognitions = detector.detect(croppedBitmap);

                for(Recognition recognition : recognitions) {
                    cropToFrameTransform.mapRect(recognition.getLocation());
                }

                overlayView.setRecognitions(recognitions);
                overlayView.postInvalidate();

                isProcessingFrame = false;
                camera.addCallbackBuffer(bytes);
                Log.i(MainActivity.TAG, recognitions.toString());
            }
        }).start();
    }
}
