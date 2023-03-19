package com.main.utils;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;

public class LayoutOnCreate{
    public static void init(AppCompatActivity activity){
        if(activity.getSupportActionBar()!=null){
            activity.getSupportActionBar().hide();
        }

    }

}
