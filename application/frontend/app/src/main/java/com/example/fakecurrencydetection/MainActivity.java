package com.example.fakecurrencydetection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;

import com.main.utils.Env;
import com.main.utils.SharedResource;

import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.main.utils.Request;

import org.json.JSONException;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;


public class MainActivity extends CameraActivity {

    private static final String[] PERMISSIONS={
            Manifest.permission.CAMERA
    };
    private static final int REQUEST_PERMISSION=34;
    private static boolean isFlashOn=false;
    private static final int PERMISSIONS_COUNT=1;
    private boolean isCameraInitialised;
    private static RelativeLayout  rectFrame;
    private ImageButton flashB;
    private ImageButton  takePictureButton;
    private static boolean fM;
    private static boolean captureImage=false;
    private JavaCameraView cameraBridgeViewBase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraBridgeViewBase=findViewById(R.id.camera_preview);
        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {

            }

            @Override
            public void onCameraViewStopped() {

            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                Mat rgba=inputFrame.rgba();
                int w_rect = 720;
                int h_rect = 512;
                if(captureImage){
                    toggleFlash(false);
                    Matrix matrix = new Matrix();

                    matrix.postRotate(90);
                    //rotated bitmap
                    Bitmap bitmap = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(rgba, bitmap);
                    //original bitmap
                    Bitmap rotatedBitmap=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);

                    int startX=(rotatedBitmap.getWidth()/2)-(h_rect/2),
                        startY=(rotatedBitmap.getHeight()/2)-(w_rect/2);
                    //cropping calculation
                    Log.d("Dimensions","rect width: "+w_rect+", rect height"+h_rect+", image width :"+rotatedBitmap.getWidth()+", image height :"+rotatedBitmap.getHeight());
                    Bitmap croppedBitmap= Bitmap.createBitmap(rotatedBitmap,startX,startY,h_rect,w_rect);

                    takenImage(croppedBitmap);
                    captureImage=false;
                }
                int w = rgba.width();
                int h = rgba.height();

                Imgproc.rectangle(rgba, new Point( (w-w_rect)/2, (h-h_rect)/2 ), new Point(
                        (w+w_rect)/2, (h+h_rect)/2 ), new Scalar( 255, 255, 255 ), 2);
                return rgba;
            }
        });
        //init open cv and camera
        if(OpenCVLoader.initDebug()){
            cameraBridgeViewBase.enableView();
        }
        //to make on click for buttons
        flashB=findViewById(R.id.flash);
        takePictureButton=findViewById(R.id.take_picture);
        flashB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleFlash();
            }
        });
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takeImage();
            }
        });

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

    @Override
    protected void onResume() {
        super.onResume();

        // to check if the device has camera device
        if(this.checkCameraHardware(this)){
//            rectFrame=findViewById(R.id.rect_frame);
            if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.M && this.isPermissionDenied()){
                this.requestPermissions(PERMISSIONS,REQUEST_PERMISSION);
                return;
            }
            cameraBridgeViewBase.enableView();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        this.releaseCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.releaseCamera();
    }

    void toggleFlash(){
        this.toggleFlash(isFlashOn?false:true);
    }
    void toggleFlash(boolean type){
        if( this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)){
            if(!type){
                cameraBridgeViewBase.setFlashMode(this,0);
                isFlashOn=false;
            }else{
                cameraBridgeViewBase.setFlashMode(this,1);
                isFlashOn=true;
            }
        }
    }

    private static void takeImage(){
        captureImage=true;
    }
    private void takenImage(Bitmap image){
        if(isFlashOn){
            toggleFlash();
        }
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            // Save the captured image to a file
            sendImageBytes(byteArray, Env.PORT_NUMBER+"/find-currency","currency_image");

            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public void sendImageBytes(byte[] imageBytes, String urlString, String imageName) {
        try {
            //sending request to the backend
            SharedResource sr=new SharedResource();
            Thread th=new Thread(new Request(urlString,imageName,imageBytes,sr));
            th.start();
            while(sr.getResponse()==null){
                continue;
            }
            Log.d("Logged the response : ",sr.getResponse().getString("result"));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Toast.makeText(getApplicationContext(),sr.getResponse().getString("result"),Toast.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void releaseCamera(){
        cameraBridgeViewBase.disableView();
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cameraBridgeViewBase);
    }

}