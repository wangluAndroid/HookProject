package com.zcbl.ssvs.hookproject;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void jumpSecondActivity(View view) {
        /**
         *  跳转到目标对象SecondActivity  ----但是跳转到目标对象SecondActivity没有在AndroidManifest.xml中注册----> 没有注册的话会被Android系统识破  -----
         *          ---为了能够顺利启动SecondActivity,我们必须劫持AMS，包装SecondActiviy，然后在还原成SecondActivity-------> SecondActivity
         */
        Intent intent = new Intent(this, SecondActivity.class);
        intent.getComponent();
        /**
         * Activity this.startActivity(intent, null)-----> Activity startActivityForResult(intent, -1) -----> Activity startActivityForResult(intent, requestCode, null) ---
         *          ----> 上个方法中的 Instrumentation.ActivityResult ar = mInstrumentation.execStartActivity(this, mMainThread.getApplicationThread(), mToken, this,
         *              intent, requestCode, options) ------> Instrumentation.class execStartActivity()-----> 上个方法中的int result = ActivityManager.getService().startActivity（）
         *
         *              最终调用到 ActivityManager.getService().startActivity（）方法中
         *              1.先看 ActivityManager.getService() 这个静态方法 返回一个IActivityManager对象,实际是一个Binder对象，这个对象实际是由Singleton.create()创建的；
         *              IActivityManager 是一个与AMS进行通话的系统私有API对象，它提供了从应用程序返回到AMS的调用；
         *              2.最终调用startActiviy 对象是ActiviyManager中的IActivityMananger对象，而这个IActivityMananger对象不是静态的，而IActivityMananger这个对象是Singleton对象的属性，
         *                      这个Singleton对象是静态的
         *
         *
         */
        startActivity(intent);
    }

    public void jumpThirdActivity(View view) {
        Intent intent = new Intent(this, ThirdActivity.class);
        startActivity(intent);
    }
}
