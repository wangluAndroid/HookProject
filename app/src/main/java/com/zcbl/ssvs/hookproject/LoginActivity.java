package com.zcbl.ssvs.hookproject;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * Created by serenitynanian on 2018/5/23.
 *
 * todo    注意事项如下：不能继承AppCompatActivity
 *
 * 这里必须继承自Activity 如果继承自AppCompatActivity就会报出
 * java.lang.RuntimeException: Unable to start activity ComponentInfo{com.zcbl.ssvs.hookproject/com.zcbl.ssvs.hookproject.LoginActivity}:
 * java.lang.IllegalArgumentException: android.content.pm.PackageManager$NameNotFoundException: ComponentInfo{com.zcbl.ssvs.hookproject/com.zcbl.ssvs.hookproject.LoginActivity}
 */
//必须继承自Activity
public class LoginActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }
}
