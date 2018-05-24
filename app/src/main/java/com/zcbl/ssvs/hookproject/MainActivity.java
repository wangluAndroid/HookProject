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
        /**
         * 基于sdk25  android系统版本为8.0
         * Activity.class this.startActivity(intent, null)-----> Activity.class startActivityForResult(intent, -1) -----> Activity.class startActivityForResult(intent, requestCode, null) ---
         *          ----> 在上个方法中执行下面的方法 Instrumentation.ActivityResult ar = mInstrumentation.execStartActivity(this, mMainThread.getApplicationThread(), mToken, this,
         *              intent, requestCode, options) ------> Instrumentation.class execStartActivity()-----> 在上个方法中执行此方法：int result = ActivityManagerNative.getDefault().startActivity（）
         *
         *              最终调用到 ActivityManagerNative.getDefault().startActivity（）方法中
         *
         *              1.先看 ActivityManagerNative.getDefault() 这个静态方法，返回一个IActivityManager对象,实际是一个Binder对象，这个对象实际是由Singleton.create()创建的；
         *              IActivityManager 是一个与AMS进行通话的系统私有API对象，它提供了从应用程序返回到ActivityMananger的调用；
         *              2.最终调用IActivityManager对象的startActiviy方法，而这个IActivityMananger对象不是静态的，而IActivityMananger这个对象是Singleton对象的属性，
         *                      而这个Singleton对象是静态的，所以就可以找到系统的Singleton对象，就可以找到系统的IActivityMananger对象进行动态代理；
         *
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
