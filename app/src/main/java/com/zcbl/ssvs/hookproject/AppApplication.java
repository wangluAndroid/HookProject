package com.zcbl.ssvs.hookproject;

import android.app.Application;

/**
 * Created by serenitynanian on 2018/5/23.
 */

public class AppApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        new HookUtils().hookStartActivity();
    }
}