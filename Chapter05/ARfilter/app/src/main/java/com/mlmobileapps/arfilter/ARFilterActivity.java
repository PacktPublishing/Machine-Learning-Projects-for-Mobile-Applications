package com.mlmobileapps.arfilter;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.media.Image;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class ARFilterActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Context context;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private static final int REQUEST_STORAGE_PERMISSION = 201;
    private TextView cameraVersion;
    private ImageView ivAutoFocus;

    private static TextView textView;
    private static ImageClassifier classifier;
    private static ARFilterActivity mn;

    // CAMERA VERSION ONE DECLARATIONS
    private CameraSource mCameraSource = null;

    // CAMERA VERSION TWO DECLARATIONS
    private Camera2Source mCamera2Source = null;

    // COMMON TO BOTH CAMERAS
    private CameraSourcePreview mPreview;
    private FaceDetector previewFaceDetector = null;
    private GraphicOverlay mGraphicOverlay;
    private FaceGraphic mFaceGraphic;
    private boolean wasActivityResumed = false;
    private boolean isRecordingVideo = false;
    private Button takePictureButton;
    private ImageButton switchButton;
    private Button videoButton;

    // DEFAULT CAMERA BEING OPENED
    private boolean usingFrontCamera = true;

    // MUST BE CAREFUL USING THIS VARIABLE.
    // ANY ATTEMPT TO START CAMERA2 ON API < 21 WILL CRASH.
    private boolean useCamera2 = false;
    protected static final String MODEL_TYPE = "TYPE";
    private ImageView imageView;
    private View main;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_arfilter);
        context = getApplicationContext();

       // takePictureButton = (Button) findViewById(R.id.btn_takepicture);
        switchButton = (ImageButton) findViewById(R.id.btn_switch);
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
        cameraVersion = (TextView) findViewById(R.id.cameraVersion);
        ivAutoFocus = (ImageView) findViewById(R.id.ivAutoFocus);
        textView = (TextView) findViewById(R.id.text);
     //   screenshotview = (ImageView)findViewById(R.id.screenshot);
        main = findViewById(R.id.mainlayout);
        mn = ARFilterActivity.this;


        try {
            classifier = new ImageClassifier(mn);
            Log.d(TAG,"Initialized classifier successfully");
        }
        catch(IOException e){
            Log.d(TAG,"Failed to initialize an image classifier.\n"+e.getMessage());
        }

        if(checkGooglePlayAvailability()) {
            requestPermissionThenOpenCamera();

            switchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(usingFrontCamera) {
                        stopCameraSource();
                        createCameraSourceBack();
                        usingFrontCamera = false;
                    } else {
                        stopCameraSource();
                        createCameraSourceFront();
                        usingFrontCamera = true;
                    }
                }
            });



            mPreview.setOnTouchListener(CameraPreviewTouchListener);
        }
    }

    public static String classifierType(){
        String type = mn.getIntent().getExtras().getString("TYPE");
        if(type!=null) {
            if(type.equals("gender"))
                return "gender";
            else
                return "emotion";
        }
        else
            return null;
    }

    public static ImageClassifier getClassifier(){
        return classifier;
    }

    public static void setToast(final String text){
        mn.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(text);
            }
        });
    }

    final CameraSource.ShutterCallback cameraSourceShutterCallback = new CameraSource.ShutterCallback() {@Override public void onShutter() {Log.d(TAG, "Shutter Callback!");}};
    final CameraSource.PictureCallback cameraSourcePictureCallback = new CameraSource.PictureCallback() {
        @Override
        public void onPictureTaken(Bitmap picture) {
            Log.d(TAG, "Taken picture is here!");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switchButton.setEnabled(true);
                }
            });
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(new File(Environment.getExternalStorageDirectory(), "/camera_picture.png"));
                picture.compress(Bitmap.CompressFormat.JPEG, 95, out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    final Camera2Source.ShutterCallback camera2SourceShutterCallback = new Camera2Source.ShutterCallback() {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onShutter() {Log.d(TAG, "Shutter Callback for CAMERA2");}
    };

    final Camera2Source.PictureCallback camera2SourcePictureCallback = new Camera2Source.PictureCallback() {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onPictureTaken(Image image) {
            Log.d(TAG, "Taken picture is here!");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switchButton.setEnabled(true);
                }
            });
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            Bitmap picture = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(new File(Environment.getExternalStorageDirectory(), "/camera2_picture.png"));
                picture.compress(Bitmap.CompressFormat.JPEG, 95, out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private boolean checkGooglePlayAvailability() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context);
        if(resultCode == ConnectionResult.SUCCESS) {
            return true;
        } else {
            if(googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(ARFilterActivity.this, resultCode, 2404).show();
            }
        }
        return false;
    }

    private void requestPermissionThenOpenCamera() {
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "requestPermissionThenOpenCamera: "+Build.VERSION.SDK_INT);
                useCamera2 = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
                createCameraSourceFront();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private void createCameraSourceFront() {
        previewFaceDetector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.NO_CLASSIFICATIONS)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.FAST_MODE)
                .setProminentFaceOnly(usingFrontCamera)
                .setTrackingEnabled(true)
                .setMinFaceSize(usingFrontCamera?0.35f : 0.15f)
                .build();

        if(previewFaceDetector.isOperational()) {
            previewFaceDetector.setProcessor(new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory()).build());
        } else {
            Toast.makeText(context, "FACE DETECTION NOT AVAILABLE", Toast.LENGTH_SHORT).show();
        }
        Log.e(TAG, "createCameraSourceFront: "+useCamera2 );
        if(useCamera2) {
            mCamera2Source = new Camera2Source.Builder(context, previewFaceDetector)
                    .setFocusMode(Camera2Source.CAMERA_AF_AUTO)
                    .setFlashMode(Camera2Source.CAMERA_FLASH_AUTO)
                    .setFacing(Camera2Source.CAMERA_FACING_FRONT)
                    .build();
            startCameraSource();
            //IF CAMERA2 HARDWARE LEVEL IS LEGACY, CAMERA2 IS NOT NATIVE.
            //WE WILL USE CAMERA1.
//            if(mCamera2Source.isCamera2Native()) {
//                startCameraSource();
//            } else {
//                useCamera2 = false;
//                if(usingFrontCamera) createCameraSourceFront(); else createCameraSourceBack();
//            }
        } else {
            mCameraSource = new CameraSource.Builder(context, previewFaceDetector)
                    .setFacing(CameraSource.CAMERA_FACING_FRONT)
                    .setRequestedFps(30.0f)
                    .build();

            startCameraSource();
        }
    }

    private void createCameraSourceBack() {
        previewFaceDetector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.NO_CLASSIFICATIONS)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.FAST_MODE)
                .setProminentFaceOnly(true)
                .setTrackingEnabled(true)
                .build();

        if(previewFaceDetector.isOperational()) {
            previewFaceDetector.setProcessor(new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory()).build());
        } else {
            Toast.makeText(context, "FACE DETECTION NOT AVAILABLE", Toast.LENGTH_SHORT).show();
        }

        if(useCamera2) {
            mCamera2Source = new Camera2Source.Builder(context, previewFaceDetector)
                    .setFocusMode(Camera2Source.CAMERA_AF_AUTO)
                    .setFlashMode(Camera2Source.CAMERA_FLASH_AUTO)
                    .setFacing(Camera2Source.CAMERA_FACING_BACK)
                    .build();

            //IF CAMERA2 HARDWARE LEVEL IS LEGACY, CAMERA2 IS NOT NATIVE.
            //WE WILL USE CAMERA1.
            if(mCamera2Source.isCamera2Native()) {
                startCameraSource();
            } else {
                useCamera2 = false;
                if(usingFrontCamera) createCameraSourceFront(); else createCameraSourceBack();
            }
        } else {
            mCameraSource = new CameraSource.Builder(context, previewFaceDetector)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedFps(30.0f)
                    .build();

            startCameraSource();
        }
    }

    private void startCameraSource() {
        if(useCamera2) {
            if(mCamera2Source != null) {
                cameraVersion.setText("Camera 2");
                try {mPreview.start(mCamera2Source, mGraphicOverlay);
                } catch (IOException e) {
                    Log.e(TAG, "Unable to start camera source 2.", e);
                    mCamera2Source.release();
                    mCamera2Source = null;
                }
            }
        } else {
            if (mCameraSource != null) {
                cameraVersion.setText("Camera 1");
                try {mPreview.start(mCameraSource, mGraphicOverlay);
                } catch (IOException e) {
                    Log.e(TAG, "Unable to start camera source.", e);
                    mCameraSource.release();
                    mCameraSource = null;
                }
            }
        }
    }

    private void stopCameraSource() {
        mPreview.stop();
    }

    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay,context,usingFrontCamera);
        }
    }

    private class GraphicFaceTracker extends Tracker<Face> {
        private static final String TAG = "FaceTracker";

        private GraphicOverlay mOverlay;
        private Context mContext;
        private boolean mIsFrontFacing;
        private FaceGraphic mFaceGraphic;
        private FaceData mFaceData;
        private boolean mPreviousIsLeftEyeOpen = true;
        private boolean mPreviousIsRightEyeOpen = true;

        // Subjects may move too quickly to for the system to detect their detect features,
        // or they may move so their features are out of the tracker's detection range.
        // This map keeps track of previously detected facial landmarks so that we can approximate
        // their locations when they momentarily "disappear".
        private Map<Integer, PointF> mPreviousLandmarkPositions = new HashMap<>();

        GraphicFaceTracker(GraphicOverlay overlay, Context context, boolean isFrontFacing) {
            mOverlay = overlay;
            mContext = context;
            mIsFrontFacing = isFrontFacing;
            mFaceData = new FaceData();
        }

        // 1 Called when a new Face is detected and its tracking begins. Youâ€™re using it to create a new
        // instance of FaceGraphic, which makes sense: when a new face is detected, you want to create
        // new AR images to draw over it.
        @Override
        public void onNewItem(int id, Face face) {
            mFaceGraphic = new FaceGraphic(mOverlay, mContext, mIsFrontFacing);
        }

        // 2 Called when some property (position, angle, or state) of a tracked face changes. Youâ€™re using
        // it to add the FaceGraphic instance to the GraphicOverlay and then call FaceGraphicâ€™s update
        // method, which passes along the tracked faceâ€™s data.
        @Override
        public void onUpdate(FaceDetector.Detections detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            updatePreviousLandmarkPositions(face);

            // Get head angles.
            mFaceData.setEulerY(face.getEulerY());
            mFaceData.setEulerZ(face.getEulerZ());

            // Get face dimensions.
            mFaceData.setPosition(face.getPosition());
            mFaceData.setWidth(face.getWidth());
            mFaceData.setHeight(face.getHeight());

            // Get the positions of facial landmarks.
            mFaceData.setLeftEyePosition(getLandmarkPosition(face, Landmark.LEFT_EYE));
            mFaceData.setRightEyePosition(getLandmarkPosition(face, Landmark.RIGHT_EYE));
            mFaceData.setMouthBottomPosition(getLandmarkPosition(face, Landmark.LEFT_CHEEK));
            mFaceData.setMouthBottomPosition(getLandmarkPosition(face, Landmark.RIGHT_CHEEK));
            mFaceData.setNoseBasePosition(getLandmarkPosition(face, Landmark.NOSE_BASE));
            mFaceData.setMouthBottomPosition(getLandmarkPosition(face, Landmark.LEFT_EAR));
            mFaceData.setMouthBottomPosition(getLandmarkPosition(face, Landmark.LEFT_EAR_TIP));
            mFaceData.setMouthBottomPosition(getLandmarkPosition(face, Landmark.RIGHT_EAR));
            mFaceData.setMouthBottomPosition(getLandmarkPosition(face, Landmark.RIGHT_EAR_TIP));
            mFaceData.setMouthLeftPosition(getLandmarkPosition(face, Landmark.LEFT_MOUTH));
            mFaceData.setMouthBottomPosition(getLandmarkPosition(face, Landmark.BOTTOM_MOUTH));
            mFaceData.setMouthRightPosition(getLandmarkPosition(face, Landmark.RIGHT_MOUTH));

            // 1
            final float EYE_CLOSED_THRESHOLD = 0.4f;
            float leftOpenScore = face.getIsLeftEyeOpenProbability();
            if (leftOpenScore == Face.UNCOMPUTED_PROBABILITY) {
                mFaceData.setLeftEyeOpen(mPreviousIsLeftEyeOpen);
            } else {
                mFaceData.setLeftEyeOpen(leftOpenScore > EYE_CLOSED_THRESHOLD);
                mPreviousIsLeftEyeOpen = mFaceData.isLeftEyeOpen();
            }
            float rightOpenScore = face.getIsRightEyeOpenProbability();
            if (rightOpenScore == Face.UNCOMPUTED_PROBABILITY) {
                mFaceData.setRightEyeOpen(mPreviousIsRightEyeOpen);
            } else {
                mFaceData.setRightEyeOpen(rightOpenScore > EYE_CLOSED_THRESHOLD);
                mPreviousIsRightEyeOpen = mFaceData.isRightEyeOpen();
            }

            // 2
            // See if there's a smile!
            // Determine if person is smiling.
            final float SMILING_THRESHOLD = 0.8f;
            mFaceData.setSmiling(face.getIsSmilingProbability() > SMILING_THRESHOLD);

            mFaceGraphic.update(mFaceData);
        }

        // 3
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }

        // Facial landmark utility methods
        // ===============================

        /** Given a face and a facial landmark position,
         *  return the coordinates of the landmark if known,
         *  or approximated coordinates (based on prior data) if not.
         */
        private PointF getLandmarkPosition(Face face, int landmarkId) {
            for (Landmark landmark : face.getLandmarks()) {
                if (landmark.getType() == landmarkId) {
                    return landmark.getPosition();
                }
            }

            PointF landmarkPosition = mPreviousLandmarkPositions.get(landmarkId);
            if (landmarkPosition == null) {
                return null;
            }

            float x = face.getPosition().x + (landmarkPosition.x * face.getWidth());
            float y = face.getPosition().y + (landmarkPosition.y * face.getHeight());
            return new PointF(x, y);
        }

        private void updatePreviousLandmarkPositions(Face face) {
            for (Landmark landmark : face.getLandmarks()) {
                PointF position = landmark.getPosition();
                float xProp = (position.x - face.getPosition().x) / face.getWidth();
                float yProp = (position.y - face.getPosition().y) / face.getHeight();
                mPreviousLandmarkPositions.put(landmark.getType(), new PointF(xProp, yProp));
            }
        }
    }

    private final CameraSourcePreview.OnTouchListener CameraPreviewTouchListener = new CameraSourcePreview.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent pEvent) {
            v.onTouchEvent(pEvent);
            if (pEvent.getAction() == MotionEvent.ACTION_DOWN) {
                int autoFocusX = (int) (pEvent.getX() - Utils.dpToPx(60)/2);
                int autoFocusY = (int) (pEvent.getY() - Utils.dpToPx(60)/2);
                ivAutoFocus.setTranslationX(autoFocusX);
                ivAutoFocus.setTranslationY(autoFocusY);
                ivAutoFocus.setVisibility(View.VISIBLE);
                ivAutoFocus.bringToFront();
                if(useCamera2) {
                    if(mCamera2Source != null) {
                        mCamera2Source.autoFocus(new Camera2Source.AutoFocusCallback() {
                            @Override
                            public void onAutoFocus(boolean success) {
                                runOnUiThread(new Runnable() {
                                    @Override public void run() {ivAutoFocus.setVisibility(View.GONE);}
                                });
                            }
                        }, pEvent, v.getWidth(), v.getHeight());
                    } else {
                        ivAutoFocus.setVisibility(View.GONE);
                    }
                } else {
                    if(mCameraSource != null) {
                        mCameraSource.autoFocus(new CameraSource.AutoFocusCallback() {
                            @Override
                            public void onAutoFocus(boolean success) {
                                runOnUiThread(new Runnable() {
                                    @Override public void run() {ivAutoFocus.setVisibility(View.GONE);}
                                });
                            }
                        });
                    } else {
                        ivAutoFocus.setVisibility(View.GONE);
                    }
                }
            }
            return false;
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestPermissionThenOpenCamera();
            } else {
                Toast.makeText(ARFilterActivity.this, "CAMERA PERMISSION REQUIRED", Toast.LENGTH_LONG).show();
                finish();
            }
        }
        if(requestCode == REQUEST_STORAGE_PERMISSION) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestPermissionThenOpenCamera();
            } else {
                Toast.makeText(ARFilterActivity.this, "STORAGE PERMISSION REQUIRED", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(wasActivityResumed)
            //If the CAMERA2 is paused then resumed, it won't start again unless creating the whole camera again.
            if(useCamera2) {
                if(usingFrontCamera) {
                    createCameraSourceFront();
                } else {
                    createCameraSourceBack();
                }
            } else {
                startCameraSource();
            }
    }

    @Override
    protected void onPause() {
        super.onPause();
        wasActivityResumed = true;
        stopCameraSource();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCameraSource();
        if(previewFaceDetector != null) {
            previewFaceDetector.release();
        }
    }
}

