package com.zcbl.ssvs.hookproject;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/**
 * Created by serenitynanian on 2018/5/23.
 */

public class SecondActivity extends Activity {

    private static final String TAG = SecondActivity.class.getSimpleName();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.e(TAG, "test this: ="+this);
        Log.e(TAG, "test getResource: ="+getResources());
        Log.e(TAG, "test getApplication: ="+getApplication());
        Log.e(TAG, "test getApplication class: ="+getApplication().getClass().getName());
        setContentView(R.layout.activity_second);
    }
}
