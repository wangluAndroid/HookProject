package com.zcbl.ssvs.hookproject;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

/**
 * Created by serenitynanian on 2018/5/23.
 */

public class HookUtils {

    private static final String TAG = HookUtils.class.getSimpleName();
    private Context context;

    public HookUtils(Context context) {
        this.context = context;
    }

    public void hookStartActivity() {

        /**
         * ActivityManagerNative.getDefault().startActivity（）
         * 先还原系统搞对象，不能自己创建一个新的对象
         * 不同的sdk对应的对象不一样
         *  1.先还原拥有IActivityManager对象的静态对象------>ActivityManager是Singleton对象中的属性------>故找到Singleton对象就能找到IActivityManager对象
         *  2.startActivity方法所需参数是一个intent，一个启动意图，我们的启动的目的类是SecondActivity,但是没有在manifest中注册，就需要将intent包装一个经过系统检查过的Intent
         */

        try {
            Class aClass = Class.forName("android.app.ActivityManagerNative");
            Field iActivityManagerSingletonFiled = aClass.getDeclaredField("gDefault");
            iActivityManagerSingletonFiled.setAccessible(true);//该方法修饰符为：private static final
            //因为是静态的变量，所有可以直接获取，传个null即可；如果是非静态的，需要传个对象；
            Object gDefaultObj = iActivityManagerSingletonFiled.get(null);

//            Class singleClass = gDefaultObj.getClass();//不能使用此种方式，因为Singleton这个类也是@hide，经过反射得到的gDefaultObj为：ActivityManagerNative$1@3771,
//                      1@3771并不是Singleton对象，所以系统找不到mInstance属性，反射无法找到；只能通过全类名找到
            Class<?> singleClass = Class.forName("android.util.Singleton");
            Field mInstance = singleClass.getDeclaredField("mInstance");
            mInstance.setAccessible(true);
            //还原IManangerActiviy 这个是系统自己创建的对象
            Object iActivityMananger = mInstance.get(gDefaultObj);

            // 使用动态代理 来代理iActivityMananger对象 将系统的调用的api拉到我们的java层来执行
            // 我们代理系统的对象
            // 第二个参数：让代理对象也实现真实对象的接口，让代理的对象看起来更像真实对象
            Class<?> IActivityManagerClass = Class.forName("android.app.IActivityManager");

            Object proxyObject = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),new Class[]{IActivityManagerClass},new MyStartActivity(iActivityMananger));
            //将系统的iActivityMananger对象----替换为----动态代理实现的iActivityMananger对象，并且动态代理实现的iActivityMananger对象实现了真实对象的所有接口，
            //              真实对象调用什么方法，都会调用代理对象中的invoke方法，invoke方法中的第二个参数就代表真实对象调用的方法；
            mInstance.set(gDefaultObj,proxyObject);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    public void hookMH(){
        try {
            Class<?> aClass = Class.forName("android.app.ActivityThread");
            Field sCurrentActivityThreadField = aClass.getDeclaredField("sCurrentActivityThread");
            sCurrentActivityThreadField.setAccessible(true);
            Object activityThreadObj = sCurrentActivityThreadField.get(null);

            Field mHField = aClass.getDeclaredField("mH");
            mHField.setAccessible(true);
            //hook点
            Handler mHObj = (Handler) mHField.get(activityThreadObj);


            //通过设置接口方式：将系统代码拉到外部代码中（我们的代码中）执行
            //因为mCallback没有set方法，不能使用set；又因为mCallback可以通过构造方法传入，但是不能创建新的对象，否则新创建的不是系统的；
            Field mCallbackField = Handler.class.getDeclaredField("mCallback");
            mCallbackField.setAccessible(true);
            mCallbackField.set(mHObj,new MyCallback(mHObj));


        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    private void handlerLaunchActivity(Message msg){

        //msg有个Intent的成员变量
        Object object = msg.obj;
        try {
            Field intentField=object.getClass().getDeclaredField("intent");
            intentField.setAccessible(true);

            Intent proxyIntent = (Intent) intentField.get(object);
            //取出真实的Intent
            Intent realIntent = proxyIntent.getParcelableExtra("realIntent");
            //如果第一次启动时，是LaunchActivity，是没有传进来的proxyIntent.getParcelableExtra("realIntent");
            if (null == realIntent) {
                Log.e(TAG, "handlerLaunchActivity: ------第一次启动app，进入此逻辑-----");

            }
            if (realIntent != null) {
                //集中式登录逻辑
                //todo 如果没有登录----------作出以下处理
                if (false) {
                    //还原-----替换proxyIntent中的要跳转的activity，然后进行真正的跳转
                    proxyIntent.setComponent(realIntent.getComponent());
                }else{
                    //// TODO: 2018/5/25  登录完成 进行还原真实Intent
                    ComponentName cn = new ComponentName(context, LoginActivity.class);
                    //把要跳转的真正的类，放到一个参数中，带到LoginActivity中，登录完后，跳转到真正的类
                    proxyIntent.putExtra("realGoToActivity", realIntent.getComponent().getClassName());

                    proxyIntent.setComponent(cn);

                }

            }



        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    class MyCallback implements Handler.Callback{
        //持有真实的handler
        private Handler mH ;

        public MyCallback(Handler mH) {
            this.mH = mH;
        }

        @Override
        public boolean handleMessage(Message msg) {

            if (msg.what == 100) {//LAUNCH_ACTIVITY 即将要加载一个activity
                // 还原原来的Intent中的要启动的activity
                handlerLaunchActivity(msg);
            }
            //加工完------>一定要丢给系统处理，才能正常处理
            mH.handleMessage(msg);
            //如果不需要进一步处理，返回true
            return true;
        }
    }


    class MyStartActivity implements InvocationHandler {

        private Object realObject ;

        public MyStartActivity(Object realObject) {
            this.realObject = realObject;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("startActivity".equals(method.getName())) {
                Log.e(TAG, "invoke: ---------->"+method.getName());
                //包装意图Intent，将传进来的Intent中没有注册的Activity替换为注册过的Intent，并且这个新的Intent包含没有
                //      注册过的Intent参数
                int index = 0 ;
                //传进来的intent
                Intent realIntent = null ;
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof Intent) {
                        //得到参数数组中Intent所在的索引值
                        index = i ;
                        realIntent = (Intent) args[i];
                        break ;
                    }
                }

                Intent newIntent = new Intent();
                ComponentName componentName = new ComponentName(context, ProxyActivity.class);
                newIntent.setComponent(componentName);
                //将真实的Intent隐藏到键值对中，这个真实的Intent中要跳转的Activity没有在AndroidManifest.xml中注册过
                newIntent.putExtra("realIntent", realIntent);

                /**
                 * 1.跳转的时候：进行封装---->让AMS检测通过
                 *
                 */
                //替换要跳转的意图为 新创建的Intent，这个新创建的Intent中要跳转的Activity是被AndroidManifest.xml中注册过的
                args[index] = newIntent ;

                /**
                 * 2.载入Activity时候：进行还原
                 * 一旦looper发送一个Launch_Activity为100的消息，就已经经过了AMS的检测，即将载入一个Activity；
                 *
                 * 这个hook点就是系统系统的Handler---->ActivityThread.class H类 就是handler对象，通过处理为100的消息进行真正载入一个activity，
                 *              但是这个对象是非静态的，其持有类ActivityThread也不是静态类，但ActivityThread自己持有一个自己的静态对象sCurrentActivityThread,
                 *              此对象时在main方法中实例化的；
                 *
                 */

            }
            //只是添加一些自己的逻辑，最终还是调用真实的方法
            return method.invoke(realObject,args);
        }


        /**
         * 插件未安装apk的dex-------> Element
         */
        public void injectPluginClass(){
            String cachePath = context.getCacheDir().getAbsolutePath();
            String apkPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/plugin.apk";
            //DexClassLoader可以加载任意路径下的apk--->dex
            DexClassLoader dexClassLoader = new DexClassLoader(apkPath, cachePath, cachePath, context.getClassLoader());

            //1.找到  插件中的Elements数组   DexPathList------>dexElements
            try {
                Class<?> aClass = Class.forName("dalvik.system.BaseDexClassLoader");
                Field dexPathList = aClass.getDeclaredField("DexPathList");
                dexPathList.setAccessible(true);
                Object dexPathListObj = dexPathList.get(dexClassLoader);

                //在拿到DexPathList中的dexElements数组
                Class<?> aClass1 = dexPathListObj.getClass();
                Field dexElements = aClass1.getDeclaredField("dexElements");
                dexElements.setAccessible(true);
                //自己插件中的dexElements[]数组
                Object dexElementsObj = dexElements.get(dexPathListObj);


                //2.找到   系统的Elements数组 dexElements
                //系统的class都是经过PathClassLoader加载的
                PathClassLoader pathClassLoader = (PathClassLoader) context.getClassLoader();
                Class<?> systemClass = Class.forName("dalvik.system.BaseDexClassLoader");
                Field systemDexPathList = systemClass.getDeclaredField("DexPathList");
                systemDexPathList.setAccessible(true);
                Object systemDexPathListObj = dexPathList.get(pathClassLoader);

                //在拿到DexPathList中的dexElements数组
                Class<?> systemClass1 = systemDexPathListObj.getClass();
                Field systemDexElements = systemClass1.getDeclaredField("dexElements");
                systemDexElements.setAccessible(true);
                //自己插件中的dexElements[]数组
                Object systemDexElementsObj = systemDexElements.get(systemDexPathListObj);


                //3. 上面的两个 dexElements数组 合并成新的dexElements  然后通过反射重新注入系统的field(系统的dexElements)
                // 创建新的Element[]对象  数组长度是上面两个Element[]相加
                int  systemLength = Array.getLength(systemDexElementsObj);
                int  pluginLength = Array.getLength(dexElementsObj);
                int totalLength = systemLength+pluginLength;
                //dalvik.system.Element
                //找到Element的Class类型  数组  每一个成员类型
                Class<?> componentType = systemDexElementsObj.getClass().getComponentType();
                Object newElementsArray = Array.newInstance(componentType, totalLength);


                //融合
                for (int i = 0 ;i<totalLength;i++) {
                    if (i < pluginLength) {
                        Array.set(newElementsArray, i, Array.get(dexElementsObj, i));
                    }else{
                        Array.set(newElementsArray,i,Array.get(systemDexElementsObj,i-pluginLength));
                    }
                }

                Field elementsFields = systemDexElementsObj.getClass().getDeclaredField("dexElements");
                elementsFields.setAccessible(true);
                //将新生产的Elements数组对象重新放到系统中去
                elementsFields.set(systemDexElementsObj,newElementsArray);

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }



        }
    }
}
