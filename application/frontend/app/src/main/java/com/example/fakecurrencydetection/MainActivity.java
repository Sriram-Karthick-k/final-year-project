package com.example.fakecurrencydetection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;

import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.main.utils.Env;
import com.main.utils.LayoutOnCreate;
import com.main.utils.SharedResource;

import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import com.main.utils.Request;


public class MainActivity extends AppCompatActivity  {

    private static final String[] PERMISSIONS={
            Manifest.permission.CAMERA
    };
    private static final int REQUEST_PERMISSION=34;
    private static final int PERMISSIONS_COUNT=1;
    private boolean isCameraInitialised;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        LayoutOnCreate.init(this);
    }

    private boolean isPermissionDenied(){
        for(int i=0;i<PERMISSIONS_COUNT;i++){
            if(this.checkSelfPermission(PERMISSIONS[i])!=PackageManager.PERMISSION_GRANTED){
                return true;
            }
        }
        return false;
    }
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==REQUEST_PERMISSION && grantResults.length>0){
            if(this.isPermissionDenied()){
                Toast.makeText(this,"Camera Permission is must for this application.",Toast.LENGTH_LONG).show();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                ((ActivityManager)(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            }else{
                onResume();
            }
        }
    }


    private Camera mCamera=null;
    private static SurfaceHolder myHolder;
    private static CameraPreview mPreview;
    private static FrameLayout preview;
    private static FrameLayout  optionsContainer;
    private ImageButton flashB;
    private ImageButton  takePicture;
    private static OrientationEventListener orientationEventListener =null;
    private static boolean fM;

//    @Override
    protected void onResume() {
        super.onResume();

//        // to check if the device has camera device
        if(this.checkCameraHardware(this)){
            if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.M && this.isPermissionDenied()){
                this.requestPermissions(PERMISSIONS,REQUEST_PERMISSION);
                return;
            }
            if(!isCameraInitialised){
                mCamera=Camera.open();
                mPreview=new CameraPreview(this,mCamera);
                preview=(FrameLayout)findViewById(R.id.camera_preview);
                preview.addView(mPreview);

                Log.d("Child of container","");
                optionsContainer=(FrameLayout) findViewById(R.id.camera_options);
                optionsContainer.bringToFront();
                optionsContainer.setZ(100f);
                rotateCamera();
//                //to enable flash light
                flashB=findViewById(R.id.flash);
                if(hasFlash()){
                    flashB.setVisibility(View.VISIBLE);

                    preview.bringChildToFront(flashB);
                    flashB.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            toggleFlash();
                        }
                    });
                }else{
                    flashB.setVisibility(View.GONE);
                }

                //to take picture
                takePicture=findViewById(R.id.take_picture);
                takePicture.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mCamera.takePicture(null,null,mPicture);
                    }
                });

                //to check for orientation change
                orientationEventListener=new OrientationEventListener(this) {
                    @Override
                    public void onOrientationChanged(int i) {
                        rotateCamera();
                    }
                };
                orientationEventListener.enable();

                //to change focus mode onclick
                preview.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        changeFocusMode();
                    }

                });
                isCameraInitialised=true;
            }else{
                try {
                    mCamera.reconnect();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Log.d("Came to 1","here ");
            }
        }
    }
    private Camera.PictureCallback mPicture=new Camera.PictureCallback(){

        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {
            Log.d("Bytes of the images ",""+bytes.length);
            if(isFlashOn){
                toggleFlash();
            }
            try {
                // Save the captured image to a file
                sendImageBytes(bytes, Env.PORT_NUMBER+"/find-currency","currency_image");

                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            camera.startPreview();
        }
    };

    public void sendImageBytes(byte[] imageBytes, String urlString, String imageName) {
        try {
            SharedResource sr=new SharedResource();
            Thread th=new Thread(new Request(urlString,imageName,imageBytes,sr));
            th.start();
            while(sr.getResponse()==null){
                continue;
            }
            Log.d("Logged the response : ",sr.getResponse().getString("result"));
            Toast.makeText(getApplicationContext(),sr.getResponse().getString("result"),Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void changeFocusMode(){
        if(whichCamera){
            if(fM){
                p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }else{
                p.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            try{
                mCamera.setParameters(p);
            }catch (Exception e){

            }
            fM=!fM;
        }
    }
    private static boolean isFlashOn=false;
    void toggleFlash(){
        if(whichCamera){
            Log.d("flash :" ,""+isFlashOn);
            if(isFlashOn){
                p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }else{
                p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            }
            try{
                mCamera.setParameters(p);
            }catch (Exception e){
                Log.d("error :",e.getMessage());
            }
            isFlashOn=!isFlashOn;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.releaseCamera();
    }

    private void releaseCamera(){
        if(mCamera!=null){
            preview.removeView(mPreview);
            mCamera.release();
            orientationEventListener.disable();
            mCamera=null;
            whichCamera=!whichCamera;
            isFlashOn=false;
            fM=false;
        }
    }

    private static List<String> camEffects;
    private static boolean hasFlash(){
        camEffects=p.getSupportedColorEffects();
        final List<String> flashModes=p.getSupportedFlashModes();
        if(flashModes==null){
            return false;
        }
        for(String flashMode:flashModes){
            if(Camera.Parameters.FLASH_MODE_ON.equals(flashMode)){
                return true;
            }
        }
        return false;
    }
    private static int rotation;
    private static boolean whichCamera=true;
    private static Camera.Parameters p;
    private void rotateCamera(){
        if(mCamera!=null){
            rotation=this.getWindowManager().getDefaultDisplay().getRotation();
            if(rotation==0){
                rotation=90;
            }else if(rotation==1){
                rotation=0;
            }else if(rotation==2){
                rotation=270;
            }else{
                rotation=180;
            }
            mCamera.setDisplayOrientation(rotation);
            if(!whichCamera){
                if(rotation==90){
                    rotation=270;
                }else if(rotation==270){
                    rotation=90;
                }
            }
            p=mCamera.getParameters();
            p.setRotation(rotation);
            mCamera.setParameters(p);
        }
    }
    private static class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private static SurfaceHolder mHolder;
        private static Camera mCamera;
        private CameraPreview(Context context,Camera camera){
            super(context);
            mCamera=camera;
            mHolder=getHolder();
            mHolder.addCallback(this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
        @Override
        public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
            myHolder=surfaceHolder;
            try {
                mCamera.setPreviewDisplay(myHolder);
                mCamera.startPreview();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

        }

        public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder,int format,int w,int h) {

        }
    }

    //request class using thread

}