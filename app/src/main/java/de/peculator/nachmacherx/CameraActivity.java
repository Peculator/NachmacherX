package de.peculator.nachmacherx;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static de.peculator.nachmacherx.Utils.getRatio;

/**
 * Created by peculator
 */
public class CameraActivity extends Activity implements Camera.PictureCallback {

    private Camera mCamera;
    private CameraPreview mPreview;

    private boolean hasRotated = false;

    public Preferences myPrefs;
    private SensorManager mSensorManager;
    private SensorListener mSensorListener;
    private boolean hasFront = false;


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(MainActivity.TAG, newConfig.toString());

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        setResult(RESULT_CANCELED);
        //Make a ref to the preferences
        myPrefs = MainActivity.myPrefs;

        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);


        // Add a listener to the Capture button
        Button captureButton = (Button) findViewById(R.id.captureButton);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        onClickFunction();
                    }
                }
        );

        captureButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getApplicationContext(), getString(R.string.camera_capture), Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        mSensorManager = (SensorManager) this
                .getSystemService(Context.SENSOR_SERVICE);

        mSensorListener = new SensorListener() {


            @Override
            public void onSensorChanged(int sensor, float[] values) {

                if (sensor == SensorManager.SENSOR_ORIENTATION) {
                    LinearLayout ll = (LinearLayout) findViewById(R.id.cameraMenu);

                    float rotation = 0;

                    float pitch = values[2];
                    float roll = values[1];

                    if (pitch <= 0 && pitch >= -180) {
                        // mostly vertical
                        rotation = 270f;
                    } else if (pitch > 0 && pitch <= 180) {
                        // vertical inverted
                        rotation = 90f;
                    }

                    if (roll <= 0 && roll >= -180 && Math.abs(pitch) < Math.abs(roll)) {
                        // mostly left
                        rotation = 0f;
                    } else if (roll > 0 && roll <= 180 && Math.abs(pitch) < Math.abs(roll)) {
                        // mostly right
                        rotation = 180f;
                    }


                    for (int i = 0; i < ll.getChildCount(); i++) {

                        ll.getChildAt(i).setRotation(rotation);
                    }

                }
            }

            @Override
            public void onAccuracyChanged(int sensor, int accuracy) {

            }
        };
        mSensorManager.registerListener(mSensorListener, Sensor.TYPE_ORIENTATION, SensorManager.SENSOR_DELAY_GAME);

        Button vb = (Button) findViewById(R.id.visibilityButton);
        vb.setText(myPrefs.lastAlpha + "");

        vb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int value = (myPrefs.lastAlpha + 20) % 100;
                Button vb = (Button) v;
                vb.setText(value + "");

                myPrefs.setLastAlpha(value);
                myPrefs.storePreferences();


                ImageView myImageView = (ImageView) findViewById(R.id.overlay_image);
                myImageView.setAlpha((float) value / 100);

            }
        });
        vb.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getApplicationContext(), getString(R.string.camera_alpha), Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        Button rb = (Button) findViewById(R.id.rotationRButton);
        rb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (MainActivity.overlay != null) {

                    ImageView myImageView = (ImageView) findViewById(R.id.overlay_image);

                    Matrix matrix = new Matrix();

                    if (MainActivity.overlay.getWidth() < MainActivity.overlay.getHeight() && !hasRotated) {
                        matrix.postRotate(0);
                        hasRotated = true;
                    } else {
                        matrix.postRotate(90);
                    }


                    // return new bitmap rotated using matrix
                    MainActivity.overlay = Bitmap.createBitmap(MainActivity.overlay, 0, 0, MainActivity.overlay.getWidth(), MainActivity.overlay.getHeight(),
                            matrix, true);

                    float maxRatio = getRatio(MainActivity.overlay,myImageView);

                    myImageView.setImageBitmap(Bitmap.createScaledBitmap(MainActivity.overlay,
                            (int) ((float) MainActivity.overlay.getWidth() / maxRatio), (int) ((float) MainActivity.overlay.getHeight() / maxRatio), false));

                }
            }
        });

        rb.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getApplicationContext(), getString(R.string.camera_rotate), Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        Button cb = (Button) findViewById(R.id.changeCamera);
        if (hasFront) {
            cb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    myPrefs.setFrontCamera(!myPrefs.isFrontCamera());
                    myPrefs.storePreferences();
                    restartPreview(myPrefs.isFrontCamera());
                }
            });
        }

        cb.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getApplicationContext(), getString(R.string.camera_switch), Toast.LENGTH_SHORT).show();
                return true;
            }
        });


        ((ImageView) findViewById(R.id.overlay_image)).setAlpha((float) myPrefs.lastAlpha / 100);

        ((ImageView) findViewById(R.id.overlay_image)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickFunction();
            }
        });

        if (cameraAvailable(mCamera)) {
            initCameraPreview();
        } else {
            finish();
        }
    }

    void restartPreview(boolean isFront) {

        mCamera.stopPreview();
        mCamera.release();

        int camId = Camera.CameraInfo.CAMERA_FACING_BACK;
        int camIdFront = Camera.CameraInfo.CAMERA_FACING_FRONT;
        if (isFront) {
            mCamera = Camera.open(camIdFront);

        } else {
            mCamera = Camera.open(camId);

        }
        initCameraPreview();
    }

    // Show the camera view on the activity
    private void initCameraPreview() {
        ImageView myImageView = (ImageView) findViewById(R.id.overlay_image);

        mPreview = (CameraPreview) findViewById(R.id.camera_preview);

        final Camera.Parameters params = mCamera.getParameters();

        final Camera.Size size = getOptimalSize(mCamera,
                getWindowManager());
        params.setJpegQuality(100);

        Camera.Size picSize = getOptimalPictureSize(mCamera);
        params.setPictureSize(picSize.width, picSize.height);

        mPreview.setLayoutParams(new FrameLayout.LayoutParams(size.width,
                size.height, Gravity.CENTER));


        if (MainActivity.overlay != null) {
            Bitmap a = MainActivity.overlay;
            // create new matrix object
            Matrix matrix = new Matrix();

            if (a.getWidth() < a.getHeight())
                matrix.postRotate(270);

            // return new bitmap rotated using matrix
            a = Bitmap.createBitmap(a, 0, 0, a.getWidth(), a.getHeight(),
                    matrix, true);

            float maxRatio = getRatio(a,myImageView);

            myImageView.setImageBitmap(Bitmap.createScaledBitmap(a,
                    (int) ((float) a.getWidth() / maxRatio), (int) ((float) a.getHeight() / maxRatio), false));

        }

        if (myPrefs.isFrontCamera())
            params.set("camera-id", 2);
        else {
            params.set("camera-id", 1);
        }

        if(hasCameraAutofocus(mCamera)){
            Log.i(MainActivity.TAG,"Autofocus");
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }

        if(hasCameraFlash(mCamera)){
            params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        }

        params.setPreviewSize(size.width, size.height);
        params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        params.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
        params.setAntibanding(Camera.Parameters.ANTIBANDING_AUTO);

        mCamera.setParameters(params);

        mPreview.init(mCamera);
    }

    public static boolean hasCameraFlash(Camera camera) {
        List<String> supportedFlashModes = camera.getParameters().getSupportedFlashModes();
        return supportedFlashModes != null && supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_AUTO);
    }

    public static boolean hasCameraAutofocus(Camera camera) {
        List<String> supportedFocusModes = camera.getParameters().getSupportedFocusModes();
        return supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO) &&
                camera.getParameters().getFocusMode()!= Camera.Parameters.FOCUS_MODE_FIXED &&
                camera.getParameters().getFocusMode()!= Camera.Parameters.FOCUS_MODE_EDOF &&
                camera.getParameters().getFocusMode()!= Camera.Parameters.FOCUS_MODE_INFINITY;
    }

    public static Camera.Size getOptimalPictureSize(Camera cam) {
        final Camera.Parameters parameters = cam.getParameters();


        if (MainActivity.myPrefs.getLastURLSource() != "") {
            BitmapFactory.Options ops = new BitmapFactory.Options();
            ops.inJustDecodeBounds = true;

            BitmapFactory.decodeFile(MainActivity.myPrefs.getLastURLSource(), ops);

            for (int i = 0; i <parameters.getSupportedPictureSizes().size() ; i++) {
                if(parameters.getSupportedPictureSizes().get(i).width == ops.outWidth &&
                        parameters.getSupportedPictureSizes().get(i).height == ops.outHeight ||
                        parameters.getSupportedPictureSizes().get(i).width == ops.outHeight &&
                                parameters.getSupportedPictureSizes().get(i).height == ops.outWidth ) {
                    Log.i(MainActivity.TAG, "Same Size:" +parameters.getSupportedPictureSizes().get(i).width + ":" + parameters.getSupportedPictureSizes().get(i).height);
                    return parameters.getSupportedPictureSizes().get(i);
                }
            }

        }
        Log.i(MainActivity.TAG, parameters.getSupportedPictureSizes().get(0).width + ":" + parameters.getSupportedPictureSizes().get(0).height);
        return parameters.getSupportedPictureSizes().get(0);
    }

    public static Camera.Size getOptimalSize(Camera cam, WindowManager wm) {
        Camera.Size result = null;
        final Camera.Parameters parameters = cam.getParameters();

        for (final Camera.Size size : parameters.getSupportedPreviewSizes()) {

            Point displaySize = getDisplaySize(wm);

            if (size.width <= displaySize.x && size.height <= displaySize.y) {
                if (result == null) {
                    result = size;
                } else {
                    final int resultArea = result.width * result.height;
                    final int newArea = size.width * size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }
        if (result == null) {
            result = parameters.getSupportedPreviewSizes().get(0);
        }
        return result;
    }

    @SuppressLint("NewApi")
    public static Point getDisplaySize(WindowManager w) {
        int Measuredwidth = 0;
        int Measuredheight = 0;
        Point size = new Point();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            w.getDefaultDisplay().getSize(size);

            Measuredwidth = size.x;
            Measuredheight = size.y;
        } else {
            Display d = w.getDefaultDisplay();
            Measuredwidth = d.getWidth();
            Measuredheight = d.getHeight();
        }

        return new Point(Measuredwidth, Measuredheight);
    }

    public static boolean cameraAvailable(Camera camera) {
        return camera != null;
    }

    private void onClickFunction() {
        mCamera.takePicture(null, null, this);
    }


    public Camera getCameraInstance() {
        int cameraCount;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();

        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                hasFront = true;
        }

        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);

            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT && myPrefs.isFrontCamera()) {
                try {
                    cam = Camera.open(camIdx);
                    break;
                } catch (RuntimeException e) {
                    Log.e(MainActivity.TAG, "Camera failed to open: " + e.getLocalizedMessage());
                }
            } else if (cameraInfo.facing != Camera.CameraInfo.CAMERA_FACING_FRONT && !myPrefs.isFrontCamera()) {
                try {
                    cam = Camera.open(camIdx);
                    break;
                } catch (RuntimeException e) {
                    Log.e(MainActivity.TAG, "Camera failed to open: " + e.getLocalizedMessage());
                }

            }
        }

        return cam;
    }


    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        File pictureFile = getOutputMediaFile(false);
        if (pictureFile == null) {
            Log.d(MainActivity.TAG, "Error creating media file, check storage permissions: ");
            return;
        }


        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(MainActivity.TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(MainActivity.TAG, "Error accessing file: " + e.getMessage());
        }

        Log.d(MainActivity.TAG, "Stream finished");

        addImageToGallery(pictureFile.getPath(), getApplicationContext());
        setResult(pictureFile.getPath());
        releaseCamera();
        finish();
    }

    private void setResult(String path) {
        Intent intent = new Intent();
        intent.putExtra(Preferences.IMAGE_PATH, path);
        setResult(RESULT_OK, intent);
    }

    public static void addImageToGallery(final String filePath,
                                         final Context context) {

        ContentValues values = new ContentValues();

        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, filePath);

        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values);
    }

    public static File getOutputMediaFile(boolean merge) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                Preferences.FOLDER_NAME_GALLERY);
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(MainActivity.TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());

        String merged = (merge == true) ? "_merged_" : "";

        return new File(mediaStorageDir.getPath() + File.separator
                + "IMG_" + timeStamp + merged + "_NachmacherX.jpg");
    }

    @Override
    protected void onDestroy() {
        releaseCamera();
        mSensorManager.unregisterListener(mSensorListener);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            Log.i(MainActivity.TAG, "releasing");
            mCamera = null;
        }

    }
}

