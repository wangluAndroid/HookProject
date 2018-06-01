package com.zcbl.ssvs.hookproject;

import android.app.Application;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by serenitynanian on 2018/5/23.
 */

public class AppApplication extends Application {

    private AssetManager assetManager ;
    private Resources newResources ;
    @Override
    public void onCreate() {
        super.onCreate();
        HookUtils hookUtils = new HookUtils(this);
        hookUtils.hookStartActivity();
        hookUtils.hookMH();




        //resources融合
        String apkPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/plugin.apk";
        try {
            assetManager = AssetManager.class.newInstance();
            Class<? extends AssetManager> aClass = assetManager.getClass();
            Method addAssetPath = aClass.getDeclaredMethod("addAssetPath", String.class);
            addAssetPath.setAccessible(true);
            addAssetPath.invoke(assetManager, apkPath);



            //ensureStringBlocks()----------->StringBlocks数组--------->Resources.getDrawbale  getText()...
            //也就是说，如果插件中想要使用getText、getDrawable()....等方法，必须手动调用ensureStringBlocks()方法加载插件中的values目录下的xml文件资源
            //手动调用ensureStringBlocks()方法,将插件的StringBlocks实例化
            Method ensureStringBlocks = aClass.getDeclaredMethod("ensureStringBlocks");
            ensureStringBlocks.setAccessible(true);
            ensureStringBlocks.invoke(assetManager);


            Resources resources = getResources();
            //创建加载plugin.apk的Resources对象
            newResources = new Resources(assetManager, resources.getDisplayMetrics(), resources.getConfiguration());


        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }


    }


    //需要在插件中的BaseActivity中重写下面两个方法，然后调用下面的两个方法；这样让插件中的加载resources的assetmanager使用自己声明的；
    public AssetManager getAssetManager() {
        return assetManager==null?super.getAssets():assetManager;
    }

    public Resources getNewResources() {
        return newResources==null?super.getResources():newResources;
    }
}
