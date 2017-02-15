package konross.wideorejestrator;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.ContentValues.TAG;

public class RecordingService extends Service implements SensorEventListener {
    private void writeToLogs(String message) {
        Log.d("HelloServices", message);
    }


    private FrameLayout preview;
    private SurfaceHolder mSurfaceHolder;
    private SensorManager mManager;
    private Sensor mAccelerometer;
    private CameraPreview mPreview;
    private static Camera mCamera;
    private int mVideoLength = 120000; //ms
    private int phonePosition;
    private boolean isRecording = false, sensorAlarm=true, serviceRunning=false;
    private float mThreshold = 3;
    private MediaRecorder mMediaRecorder;
    private File mediaFile = new File("The output file's absolutePath");
    private final Context context = this;
    private static final String PREF_FILE_NAME = "PrefFile";
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    public RecordingService() {
    }
    @Override
    public void onCreate() {
        super.onCreate();
        writeToLogs("Called onCreate() method.");
        Toast.makeText(getApplicationContext(), "Servis serwus xD", Toast.LENGTH_SHORT).show();
        isRecording = false;
        //orientationCamera();
        mCamera = MainActivity.mCamera;
        preview = MainActivity.preview;
        mManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        mAccelerometer = mManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
       // mSurfaceHolder = CameraRecorder.mSurfaceHolder;

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        writeToLogs("Called onStartCommand() methond");
        super.onStartCommand(intent, flags, startId);
        if (isRecording == false)
            startRecording();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        writeToLogs("Called onDestroy() method");
        stopRecording();
        isRecording = false;

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public boolean startRecording(){


        if (prepareVideoRecorder()) {
            // Camera is available and unlocked, MediaRecorder is prepared,
            // now you can start recording
            mMediaRecorder.start();

            isRecording = true;
            return true;

        } else {
            // prepare didn't work, release the camera
            releaseMediaRecorder();
            return false;
        }



        /*
            Toast.makeText(getBaseContext(), "Recording Started", Toast.LENGTH_SHORT).show();


            mCamera = Camera.open();
            orientationCamera();

            /*Camera.Parameters params = mCamera.getParameters();
            mCamera.setParameters(params);
            Camera.Parameters p = mCamera.getParameters();

            final List<Camera.Size> listPreviewSize = p.getSupportedPreviewSizes();
            for (Camera.Size size : listPreviewSize) {
                Log.i(TAG, String.format("Supported Preview Size (%d, %d)", size.width, size.height));
            }

            Camera.Size previewSize = listPreviewSize.get(0);
            p.setPreviewSize(previewSize.width, previewSize.height);
            mCamera.setParameters(p);*

            mPreview = new CameraPreview(this, mCamera);

            mCamera.unlock();

            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.setCamera(mCamera);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setOutputFile(Environment.getExternalStorageDirectory().getPath() + "/video.mp4");
            mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

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
            mMediaRecorder.start();

            isRecording = true;

            return true;
*/

    }

    private boolean prepareVideoRecorder(){

        mCamera = getCameraInstance();
        orientationCamera();
        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        //mMediaRecorder.setMaxDuration(mVideoLength); //max. time of recording after event detection
        //mMediaRecorder.setVideoFrameRate(15);
        //mMediaRecorder.setVideoSize(320,240);

        // Step 4: Set output file
        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());

        // Step 5: Set the preview output
        //mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

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

    private File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    public void stopRecording() {
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
     ;
        Toast.makeText(getApplicationContext(), "Koniec nagrywania :)", Toast.LENGTH_SHORT).show();
        isRecording = false;

        /*Toast.makeText(getBaseContext(), "Recording Stopped", Toast.LENGTH_SHORT).show();
        try {
            mCamera.reconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }

        mMediaRecorder.stop();
        releaseMediaRecorder();

        mCamera.stopPreview();

        if (mCamera != null){
            mPreview.getHolder().removeCallback(mPreview);
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }*/
    }

    private void releaseCamera(){
        if (mCamera != null){
            mPreview.getHolder().removeCallback(mPreview);
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    private void orientationCamera(){
        Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        Camera.getCameraInfo(0, info);

        int result;
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

    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    public static   Camera getCameraInstance() {
        Camera c = null;
        try {//camera.open(1) popraw potem
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(sensorAlarm && isRecording) {
            //if ((Math.abs(event.values[0]) > mThreshold || Math.abs(event.values[1]) > mThreshold || Math.abs(event.values[2]) > (mThreshold + 9.8)) && isRecording == true) {
            if (((Math.abs(event.values[0]) > mThreshold || Math.abs(event.values[1]) > (mThreshold + 9.8) || Math.abs(event.values[2]) > mThreshold) && (phonePosition==1 || phonePosition == 3))||
                    ((Math.abs(event.values[0]) > mThreshold || Math.abs(event.values[1]) > mThreshold || Math.abs(event.values[2]) > (mThreshold + 9.8)) && (phonePosition==2 || phonePosition == 4))) {
                sensorAlarm = false;

                try {
                    mMediaRecorder.stop();
                } catch (RuntimeException e) {
                    mediaFile.delete();  //you must delete the outputfile when the recorder stop failed.
                    Toast.makeText(getApplicationContext(), "failed", Toast.LENGTH_SHORT).show();
                } finally {
                    mMediaRecorder.release();
                    mMediaRecorder = null;
                }
                releaseMediaRecorder(); // release the MediaRecorder object
                mCamera.lock();         // take camera access back from MediaRecorder
                isRecording = false;
                Toast.makeText(getApplicationContext(), "Wykryto zdarzenie!", Toast.LENGTH_SHORT).show();

                mVideoLength = 15000; //max. time of recording after event detection
                releaseCamera();
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
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
