### HookProject
使用Hook技术，去拦截系统api调用，添加自己的逻辑


##### 一、Hook技术也称为'钩子函数'
        '钩子函数'实际上是一个处理系统消息的程序段，通过**系统调用**，把它挂在到系统；
        在系统发送一个调用系统函数的消息后，系统在调用的系统函数前，钩子程序就先捕获该消息，这样钩子函数先得到控制权。
        这时钩子函数就可以加工处理（改变）该系统函数的行为，还可以强制结束消息的传递。

##### 二、Hook技术实现的步骤
* 1.Java层hook ----- 通过反射
>Java层如果寻找hook点？
>
>   1.所有的hook点具备的条件：对象是静态的，才能还原系统的对象的各个属性与方法；如果没有静态对象成员，就往父类中找静态对象，
>                           一级级往上找，直到找到，如果找不到，不能hook；
>
>   2.通过反射，来得到找到的静态对象；
>
>  注意：不能通过反射创建一个新的系统对象，新的系统对象不具备现有系统的属性，无法进行hook；
>
* 2.将hook到的方法放到系统之外(自己写的代码中去调用)执行

* 3.ndk层hook -----两种在native层的hook（andfix  xpose）

##### 三、如果实现'钩子函数'（hook函数）
* 1.动态代理（适合所有场景）
* 2.利用系统内部提供的接口，通过实现接口，然后注入进系统（特定场景不适用）

##### 三、本项目实现功能----免在AndroidManifest.xml注册式跳转与集中式登录
* 1.hook startActivity，隐藏真实Intent意图，替换为要跳转到proxyActivity的Intent意图，因为ProxyActivity是被AndroidManifest.xml中注册过的，通过此方式，绕过AMS检查；


~~~
 public void hookStartActivity() {

        /**
         * ActivityManagerNative.getDefault().startActivity（）
         * 先还原系统搞对象，不能自己创建一个新的对象
         * 不同的sdk对应的对象不一样
         *  1.先还原拥有IActivityManager对象的静态对象------>ActivityManager是Singleton对象中的属性------>故找到Singleton对象就能找到IActivityManager对象
         *  2.startActivity方法所需参数是一个intent，一个启动意图，我们的启动的目的类是SecondActivity,但是没有在manifest中注册，就需要将intent包装一个经过系统检查过的Intent
         */

        try {
            Class<?> aClass = Class.forName("android.app.ActivityManagerNative");
            Field iActivityManagerSingletonFiled = aClass.getDeclaredField("gDefault");
            iActivityManagerSingletonFiled.setAccessible(true);//该方法修饰符为：private static final
            //因为是静态的变量，所有可以直接获取，传个null即可；如果是非静态的，需要传个对象；
            Object gDefaultObj = iActivityManagerSingletonFiled.get(null);

//            Class<?> singleClass = gDefaultObj.getClass();//不能使用此种方式，因为gDefault为Object对象，没有mInstance属性，反射无法找到
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
    }

~~~

* 2.经过hook startActivity后，将真实的Intent替换为proxyActivity，进行包装，绕过AMS检查后，在装载启动activity时根据业务逻辑，来判断是否还原为真实的Intent；载入一个activity，都是通过Handler发送100的msg进行处理的，我们hook Handler对象，接收发送的消息，来将系统的业务逻辑拉到自己代码逻辑中执行；

> hook Handler通过实现接口方式；注意Handler中的dispatchMessage(Message msg)方法；

~~~
   /**
     * Handle system messages here.
     */
    public void dispatchMessage(Message msg) {
        if (msg.callback != null) {
            handleCallback(msg);
        } else {
            if (mCallback != null) {
                if (mCallback.handleMessage(msg)) {
                    return;
                }
            }
            handleMessage(msg);
        }
    }
    
    

~~~

~~~

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
                if (false) {
                    //还原-----替换proxyIntent中的要跳转的activity，然后进行真正的跳转
                    proxyIntent.setComponent(realIntent.getComponent());
                }else{
                    ComponentName cn = new ComponentName(context, LoginActivity.class);
                    proxyIntent.setComponent(cn);
                    //把要跳转的真正的类，放到一个参数中，带到LoginActivity中，登录完后，跳转到真正的类
                    proxyIntent.putExtra("realGoToActivity", realIntent.getComponent().getClassName());
                }

            }



        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

~~~

* 3.启动一个activity

~~~

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
~~~
