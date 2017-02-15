package konross.wideorejestrator;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private Button captureButton, settingsButton, recordedVideosView;
    public static Camera mCamera;
    public static FrameLayout preview;
    private CameraPreview mPreview;
    private MediaRecorder mMediaRecorder;
    private SensorManager mManager;
    private Sensor mAccelerometer;
    private CountDownTimer mTimer;
    private int mVideoLength = 8000; //ms
    private boolean isRecording = false, sensorAlarm=true, serviceRunning=false;
    private float mThreshold = 3;
    private double axisX, axisY, axisZ;
    private  int phonePosition, result; // result to orientation
    private String tempFileName = null, filesDirectory=null;
    public SharedPreferences preferences;
    public SharedPreferences.Editor preferencesEditor;
    private File mediaFile = new File("The output file's absolutePath");
    private final Context context = this;
    private static final String PREF_FILE_NAME = "PrefFile";
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;





    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
        preferencesEditor = preferences.edit();
        serviceRunning = preferences.getBoolean("serviceRunning",false);
        filesDirectory = preferences.getString("filesDirectory",null);

        mManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        mAccelerometer = mManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Create an instance of Camera
        mCamera = getCameraInstance();


        captureButton = (Button) findViewById(R.id.button_capture);
        settingsButton = (Button) findViewById(R.id.settings);
        recordedVideosView = (Button) findViewById(R.id.videos_view);
        //
        if(!serviceRunning) {
            orientationCamera();
        }
        else
        {
            captureButton.setBackgroundResource(android.R.drawable.presence_video_busy);
        }


        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
         preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview,0);

        recordedVideosView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(filesDirectory==null){
                    Toast.makeText(getApplicationContext(), "Nie posiadasz jeszcze żadnych nagrań.", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(getApplicationContext(), filesDirectory, Toast.LENGTH_SHORT).show();
                    //Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    //Uri uri = Uri.parse(filesDirectory);
                    //intent.setDataAndType(uri,"video/mp4");
                    //startActivity(Intent.createChooser(intent, "Open folder"));
                    Intent i = new Intent(context, VideosListActivity.class);
                    //MainActivity.this.onPause();
                    startActivity(i);
                }
            }
        });

        //captureButton = (Button) findViewById(R.id.button_capture);
        captureButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(serviceRunning){
                            Intent serviceIntent = new Intent(getApplicationContext(), RecordingService.class);
                            stopService(serviceIntent);
                            serviceRunning = false;
                            //captureButton.setBackgroundResource(android.R.drawable.presence_video_busy);
                            preferencesEditor.putBoolean("serviceRunning", serviceRunning);
                            preferencesEditor.commit();
                            //MainActivity.super.onCreate(savedInstanceState);
                        }
                        else {
                            if (isRecording) {
                                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                                // stop recording and release camera
                                try {
                                    mMediaRecorder.stop();
                                } catch (RuntimeException e) {
                                    mediaFile.delete();  //you must delete the outputfile when the recorder stop failed.
                                    Toast.makeText(getApplicationContext(), "failed!", Toast.LENGTH_SHORT).show();
                                } finally {
                                    releaseMediaRecorder(); // release the MediaRecorder object
                                    mCamera.lock();         // take camera access back from MediaRecorder
                                }

                                // inform the user that recording has stopped
                                captureButton.setBackgroundResource(android.R.drawable.presence_video_online);
                                captureButton.setText("");
                                //recordedVideosView.setVisibility();
                                Toast.makeText(getApplicationContext(), "Koniec nagrywania :)", Toast.LENGTH_SHORT).show();
                                isRecording = false;

                            } else {
                                releaseCamera();
                                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                                // initialize video camera
                                if (prepareVideoRecorder()) {
                                    // Camera is available and unlocked, MediaRecorder is prepared,
                                    // now you can start recording
                                    mMediaRecorder.start();

                                    // inform the user that recording has started
                                    captureButton.setBackgroundResource(android.R.drawable.presence_video_busy);
                                    sensorAlarm = true;
                                    isRecording = true;
                                    Toast.makeText(getApplicationContext(), "Nagrywam :)", Toast.LENGTH_SHORT).show();
                                } else {
                                    // prepare didn't work, release the camera
                                    releaseMediaRecorder();
                                    // inform user
                                    Toast.makeText(getApplicationContext(), "Coś nie działa :(", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                }
        );




    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && isRecording==false) {
            setContentView(R.layout.activity_main);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && isRecording==false){
            setContentView(R.layout.activity_main);
        }
    }



    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event

        mManager.unregisterListener(this);
    }

    @Override
    protected void onResume(){
        super.onResume();
        //mCamera = getCameraInstance();

        mAccelerometer = mManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mManager.registerListener(this, mAccelerometer,SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onBackPressed() {
        if(isRecording) {
            final AlertDialog alertDialog = new AlertDialog.Builder(this).create();

            alertDialog.setTitle("Co mam teraz zrobić?");
            alertDialog.setButton(Dialog.BUTTON_POSITIVE,"Nagrywaj w tle!", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if(!serviceRunning) {
                        Toast.makeText(getApplicationContext(), "Bedzie Service", Toast.LENGTH_SHORT).show();
                        serviceRunning=true;
                        preferencesEditor.putBoolean("serviceRunning", serviceRunning);
                        preferencesEditor.commit();
                        Intent serviceIntent = new Intent(getApplicationContext(), RecordingService.class);
                        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
                        releaseCamera();
                        startService(serviceIntent);
                        //alertDialog.dismiss();
                        MainActivity.this.finish();
                    }
                }
            });
            alertDialog.setButton(Dialog.BUTTON_NEGATIVE,"Koniec pracy",new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    captureButton.callOnClick();
                    MainActivity.this.finish();
                }
            });
            // Set the Icon for the Dialog
            alertDialog.show();


        }
        else{
            MainActivity.this.finish();
        }
    }



    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void orientationCamera(){
        Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        Camera.getCameraInfo(0, info);

        //int result;
        Camera.Parameters parameters = mCamera.getParameters();
        Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

        if(display.getRotation() == Surface.ROTATION_0)
        {
            result = (info.orientation + 360) % 360;
            phonePosition = 1;
            mCamera.setDisplayOrientation(result);
        }

        if(display.getRotation() == Surface.ROTATION_90)
        {
            result = (info.orientation + 270) % 360;
            phonePosition = 2;
            mCamera.setDisplayOrientation(result);
        }

        if(display.getRotation() == Surface.ROTATION_180)
        {
            result = (info.orientation + 180) % 360;
            phonePosition = 3;
            mCamera.setDisplayOrientation(result);
        }

        if(display.getRotation() == Surface.ROTATION_270)
        {
            result = (info.orientation + 90) % 360;
            phonePosition = 4;
            mCamera.setDisplayOrientation(result);
        }

        mCamera.setParameters(parameters);
    }

    private void releaseCamera(){
        if (mCamera != null){
            mPreview.getHolder().removeCallback(mPreview);
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }


    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static   Camera getCameraInstance() {
        Camera c = null;
        try {//camera.open(1) popraw potem
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }


    private boolean prepareVideoRecorder(){

        mCamera = getCameraInstance();
        orientationCamera();
        mMediaRecorder = new MediaRecorder();
        //mMediaRecorder.setOnInfoListener(infoListener);

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        mMediaRecorder.setMaxDuration(mVideoLength); //max. time of recording after event detection
        mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED && sensorAlarm==false){
                    //if (mMediaRecorder != null) {
                        try {
                            mMediaRecorder.stop();
                        } catch (RuntimeException e) {
                            mediaFile.delete();  //you must delete the outputfile when the recorder stop failed.
                            Toast.makeText(getApplicationContext(), "failed", Toast.LENGTH_SHORT).show();
                        } finally {
                            //mMediaRecorder.release();
                            //mMediaRecorder = null;
                        }
                        releaseMediaRecorder(); // release the MediaRecorder object
                        mCamera.lock();         // take camera access back from MediaRecorder
                        isRecording = false;
                        Toast.makeText(getApplicationContext(), "kolejne nagranie!", Toast.LENGTH_SHORT).show();

                        mVideoLength = 6000; //max. time of recording after event detection
                        releaseCamera();
                        //captureButton.setText("Zdarzenie!");
                        if (prepareVideoRecorder()) {
                            //orientationCamera();
                            mMediaRecorder.start();
                            isRecording = true;
                            Toast.makeText(getApplicationContext(), "Nagrywam zdarzenie...", Toast.LENGTH_SHORT).show();
                        } else {
                            releaseMediaRecorder();
                            Toast.makeText(getApplicationContext(), "Coś nie działa :(", Toast.LENGTH_SHORT).show();
                        }
                }
            }
        });

        mMediaRecorder.setOrientationHint(result);
        //mMediaRecorder.setVideoFrameRate(15);
        //mMediaRecorder.setVideoSize(320,240);

        // Step 4: Set output file
        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());

        // Step 5: Set the preview output
        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }
//static
    /** Create a file Uri for saving an image or video */
    private  Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }
//static
    /** Create a File for saving an image or video */
    private  File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        if (Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {

            File mediaStorageDir = null; //new File(Environment.getExternalStorageDirectory(), "Android/data/pl.konross.Wideorejestrator");
            //filesDirectory = mediaStorageDir.getPath().toString();

            if(Build.VERSION.SDK_INT >= 19)
            {
                File[] mediablabla = context.getExternalFilesDirs(null);
                if(mediablabla.length>1) {
                    filesDirectory=mediablabla[1].getPath().toString();//+"Wideorejestrator";
                    mediaStorageDir=mediablabla[1];
                    Toast.makeText(getApplicationContext(), "Mamy SD!", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getApplicationContext(), "NIEMamy SD!", Toast.LENGTH_SHORT).show();
                }
            }
            else {
                mediaStorageDir = new File(Environment.getExternalStorageDirectory(), /*"Android/data/pl.konross.*/"Wideorejestrator");
                filesDirectory = mediaStorageDir.getPath();
            }

            preferencesEditor.putString("filesDirectory", filesDirectory);
            preferencesEditor.commit();

            // This location works best if you want the created images to be shared
            // between applications and persist after your app has been uninstalled.

            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Log.d("MyCameraApp", "failed to create directory");
                    return null;
                }
            }


            // Create a media file name
            tempFileName = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            if (type == MEDIA_TYPE_IMAGE) {
                mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                        "IMG_" + tempFileName + ".jpg");
            } else if (type == MEDIA_TYPE_VIDEO) {
                mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                        "VID_" + tempFileName + ".mp4");
            } else {
                return null;
            }

            //return mediaFile;

        }
        return mediaFile;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(sensorAlarm && isRecording) {
            //if ((Math.abs(event.values[0]) > mThreshold || Math.abs(event.values[1]) > mThreshold || Math.abs(event.values[2]) > (mThreshold + 9.8)) && isRecording == true) {
            //if ((Math.abs(event.values[0]) > mThreshold || Math.abs(event.values[1]) > (mThreshold + 9.8) || Math.abs(event.values[2]) > mThreshold) && isRecording == true) {
            if (((Math.abs(event.values[0]) > mThreshold || Math.abs(event.values[1]) > (mThreshold + 9.8) || Math.abs(event.values[2]) > mThreshold) && (phonePosition==1 || phonePosition == 3))||
                    ((Math.abs(event.values[0]) > (mThreshold +9.8) || Math.abs(event.values[1]) > mThreshold || Math.abs(event.values[2]) > mThreshold) && (phonePosition==2 || phonePosition == 4))) {

                sensorAlarm = false;

                try {
                    mMediaRecorder.stop();
                } catch (RuntimeException e) {
                    mediaFile.delete();  //you must delete the outputfile when the recorder stop failed.
                    Toast.makeText(getApplicationContext(), "failed", Toast.LENGTH_SHORT).show();
                } finally {
                    releaseMediaRecorder(); // release the MediaRecorder object
                    mCamera.lock();         // take camera access back from MediaRecorder
                    isRecording = false;
                }
                Toast.makeText(getApplicationContext(), "Wykryto zdarzenie!", Toast.LENGTH_SHORT).show();

                mVideoLength = 6000; //max. time of recording after event detection
                releaseCamera();
                captureButton.setText("Zdarzenie!");
                if (prepareVideoRecorder()) {
                    //orientationCamera();
                    mMediaRecorder.start();
                    isRecording = true;
                    Toast.makeText(getApplicationContext(), "Nagrywam zdarzenie...", Toast.LENGTH_SHORT).show();
                } else {
                    releaseMediaRecorder();
                    Toast.makeText(getApplicationContext(), "Coś nie działa :(", Toast.LENGTH_SHORT).show();
                }
            }
        }
        else{
            axisX = event.values[0];
            axisY = event.values[1];
            axisZ = event.values[2];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}