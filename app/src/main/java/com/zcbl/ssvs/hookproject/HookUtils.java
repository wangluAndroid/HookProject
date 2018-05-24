package com.zcbl.ssvs.hookproject;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by serenitynanian on 2018/5/23.
 */

public class HookUtils {

    private static final String TAG = HookUtils.class.getSimpleName();

    public void hookStartActivity() {

        /**
         * ActivityManager.getService().startActivity（）
         * 先还原系统搞对象，不能自己创建一个新的对象
         * 不同的sdk对应的对象不一样
         *  1.先还原拥有IActivityManagerSingleton对象的静态成员------>ActivityManager是IActivityManagerSingleton对象中的属性
         */

        try {
            Class<?> aClass = Class.forName("android.app.ActivityManagerNative");
            Field iActivityManagerSingletonFiled = aClass.getDeclaredField("IActivityManagerSingleton");
            iActivityManagerSingletonFiled.setAccessible(true);//该方法修饰符为：private static final
            //因为是静态的变量，所有可以直接获取，传个null即可；如果是非静态的，需要传个对象；
            Object singletonObj = iActivityManagerSingletonFiled.get(null);

//            Class<?> singleClass = singletonObj.getClass();
            Class<?> singleClass = Class.forName("android.util.Singleton");
            Field mInstance = singleClass.getDeclaredField("mInstance");
            mInstance.setAccessible(true);
            //还原IManangerActiviy 这个是系统自己创建的对象
            Object iActivityMananger = mInstance.get(singletonObj);

            // 使用动态代理 将系统的调用的api拉到我们的java层来执行
            // 我们代理系统的对象
            // 第二个参数：让代理对象也实现真实对象的接口，让代理的对象看起来更像真实对象
            Object proxyObject = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),new Class[]{aClass},new MyStartActivity(iActivityMananger));
            //将系统的iActivityMananger对象----替换为----动态代理实现的iActivityMananger对象，并且动态代理实现的iActivityMananger对象实现了真实对象的所有接口，
            //              真实对象调用什么方法，都会调用代理对象中的invoke方法，invoke方法中的第二个参数就代表真实对象调用的方法；
            mInstance.set(singletonObj,proxyObject);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
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
                System.out.println("------------invoke--------------");
            }
            //只是添加一些自己的逻辑，最终还是调用真实的方法
            return method.invoke(realObject,args);
        }
    }
}
