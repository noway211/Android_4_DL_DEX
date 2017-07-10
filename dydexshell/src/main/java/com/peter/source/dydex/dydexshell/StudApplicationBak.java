package com.peter.source.dydex.dydexshell;

import android.app.Application;
import android.content.ContentProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;

import com.peter.source.dydex.dydexshell.util.ActivityThreadCompat;
import com.peter.source.dydex.dydexshell.util.Reflect;
import com.peter.source.dydex.dydexshell.util.ReflectException;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dalvik.system.DexClassLoader;

/**
 * Created by zhukui on 2016/12/19.
 */

public class StudApplicationBak extends Application {

    private static final String appkey = "APPLICATION_CLASS_NAME";
    private String odexPath;
    private String libPath;
    private String sourcePath;



    //这是context 赋值
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            File sourceapk = this.getDir("source_dex", MODE_PRIVATE);
            File odex = this.getDir("source_odex", MODE_PRIVATE);
            File libs = this.getDir("source_lib", MODE_PRIVATE);
            odexPath = odex.getAbsolutePath();
            libPath = libs.getAbsolutePath();
            sourcePath = sourceapk.getAbsolutePath();
            SharedPreferences sp = getSharedPreferences("dexconfig", Context.MODE_PRIVATE);
            Boolean isInit = sp.getBoolean("initdex",false);
            if (!isInit) {
                try {
                    // 读取程序classes.dex文件
                    byte[] dexdata = this.readDexFileFromApk();
                    // 分离出解壳后的apk文件已用于动态加载
                    this.splitPayLoadFromDex(dexdata);
                    dexdata = null;
                    sp.edit().putBoolean("initdex",true).commit();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
            // 配置动态加载环境
            Object currentActivityThread = ActivityThreadCompat.instance();
            String packageName = this.getPackageName();//当前apk的包名
            //下面两句不是太理解
            ArrayMap mPackages = Reflect.on(currentActivityThread).field("mPackages").get();
            WeakReference wr = (WeakReference) mPackages.get(packageName);

            //String reallibpath = getLibPath();
            String reallibpath = getSourceApkLibPath();
            String dexPath = getDexpath();
            Log.i("peterLog", " reallibpath===="+reallibpath);
            Log.i("peterLog", " dexPath==="+dexPath);
            //设置父classload为systemclassload 这个与壳classload完全隔离

            DexClassLoader dLoader = new DexClassLoader(dexPath, odexPath,
                    reallibpath, ClassLoader.getSystemClassLoader());
            Reflect.on(wr.get()).set("mClassLoader", dLoader);

            try {
                Object actObj = dLoader.loadClass("com.peter.example.petershell.MainActivity");
                Log.i("peterLog", "actObj:" + actObj);
            } catch (Exception e) {
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String getSourceApkLibPath(){
        ApplicationInfo info = getApplicationInfo();
        return new File(getApplicationInfo().dataDir,"lib").getPath();
    }

    private String getDexpath(){
        File file = new File(sourcePath);
        File[] subfiles = file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if(name.endsWith(".dex")){
                    return  true;
                }
                return false;
            }
        });
        StringBuffer sb = new StringBuffer();
        if(subfiles != null && subfiles.length >0){
            for(File f:subfiles){
                sb.append(f.getAbsolutePath()).append(File.pathSeparator);
            }
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }





    @Override
    public void onCreate() {
        {
           // loadResources(apkFileName);

            // 如果源应用配置有Appliction对象，则替换为源应用Applicaiton，以便不影响源程序逻辑。
            String appClassName = null;
            try {
                ApplicationInfo ai = this.getPackageManager()
                        .getApplicationInfo(this.getPackageName(),
                                PackageManager.GET_META_DATA);
                Bundle bundle = ai.metaData;
                if (bundle != null && bundle.containsKey("APPLICATION_CLASS_NAME")) {
                    appClassName = bundle.getString("APPLICATION_CLASS_NAME");//className 是配置在xml文件中的。
                    if(appClassName.startsWith(".")){
                        appClassName = getPackageName()+appClassName;
                    }
                } else {
                    Log.i("peterLog", "have no application class name");
                    return;
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.i("peterLog", "error:"+ Log.getStackTraceString(e));
                e.printStackTrace();
            }
            //有值的话调用该Applicaiton

            Object currentActivityThread = null;
            try {
                currentActivityThread = ActivityThreadCompat.instance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Object mBoundApplication = Reflect.on(currentActivityThread).field("mBoundApplication").get();
            Object loadedApkInfo = Reflect.on(mBoundApplication).field("info").get();
            Log.d("peterLog",loadedApkInfo.getClass().toString());
            //把当前进程的mApplication 设置成了null
            Reflect.on(loadedApkInfo).set("mApplication",null);
            Object oldApplication = Reflect.on(currentActivityThread).field("mInitialApplication").get();
            ArrayList<Application> mAllApplications =Reflect.on(currentActivityThread).field("mAllApplications").get();
            mAllApplications.remove(oldApplication);//删除oldApplication
            ApplicationInfo appinfo_In_LoadedApk = Reflect.on(loadedApkInfo).field("mApplicationInfo").get();
            ApplicationInfo appinfo_In_AppBindData = Reflect.on(mBoundApplication).field("appInfo").get();
            appinfo_In_LoadedApk.className = appClassName;
            appinfo_In_AppBindData.className = appClassName;

            Application app = Reflect.on(loadedApkInfo).call("makeApplication",false,null).get();

            Reflect.on(currentActivityThread).set("mInitialApplication",app);

            ArrayMap mProviderMap = Reflect.on(currentActivityThread).field("mProviderMap").get();
            Iterator it = mProviderMap.keySet().iterator();
            while (it.hasNext()) {
                Object providerClientRecord = it.next();
                Log.d("peterLog",providerClientRecord.toString());
                try {
                    ContentProvider contentProvider = Reflect.on(providerClientRecord).field("mLocalProvider").get();
                    Reflect.on(contentProvider).set("mContext",app);
                } catch (ReflectException e) {
                    e.printStackTrace();
                }

            }
            Log.i("peterLog", "app:"+app);
            app.onCreate();
        }
    }

    /**
     * 解密原始的dex文件
     * @param apkdata
     */
    private void splitPayLoadFromDex(byte[] apkdata) throws IOException {
        int ablen = apkdata.length;
        //取被加壳apk的长度   这里的长度取值，对应加壳时长度的赋值都可以做些简化
        byte[] dexlen = new byte[4];
        System.arraycopy(apkdata, ablen - 4, dexlen, 0, 4);
        ByteArrayInputStream bais = new ByteArrayInputStream(dexlen);
        DataInputStream in = new DataInputStream(bais);
        int readInt = in.readInt();
        byte[] newdex = new byte[readInt];
        //把被加壳apk内容拷贝到newdex中
        System.arraycopy(apkdata, ablen - 4 - readInt, newdex, 0, readInt);
        //这里应该加上对于apk的解密操作，若加壳是加密处理的话
        //?

        //对源程序Apk进行解密
        newdex = decrypt(newdex);
        bais = new ByteArrayInputStream(newdex);
        in = new DataInputStream(bais);
        int dexnum = in.read();
        Log.d("peterlog","classdex 个数====="+dexnum);
        for(int i=0;i<dexnum;i++){
            File file = new File(sourcePath,"source"+i+".dex");
            FileOutputStream localFileOutputStream = new FileOutputStream(file);
            int length = in.readInt();
            byte[] tem = new byte[length];
            in.read(tem);
            localFileOutputStream.write(tem);
            localFileOutputStream.close();
        }
        in.close();
        bais.close();
    }

    /**
     * 从apk包里面获取dex文件内容（byte）
     * @return
     * @throws IOException
     */
    private byte[] readDexFileFromApk() throws IOException {
        ByteArrayOutputStream dexByteArrayOutputStream = new ByteArrayOutputStream();
        ZipInputStream localZipInputStream = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(
                        this.getApplicationInfo().sourceDir)));
        while (true) {
            ZipEntry localZipEntry = localZipInputStream.getNextEntry();
            if (localZipEntry == null) {
                localZipInputStream.close();
                break;
            }
            if (localZipEntry.getName().equals("classes.dex")) {
                byte[] arrayOfByte = new byte[1024];
                while (true) {
                    int i = localZipInputStream.read(arrayOfByte);
                    if (i == -1)
                        break;
                    dexByteArrayOutputStream.write(arrayOfByte, 0, i);
                }
            }
            localZipInputStream.closeEntry();
        }
        localZipInputStream.close();
        return dexByteArrayOutputStream.toByteArray();
    }

    // //直接返回数据，读者可以添加自己解密方法
    private byte[] decrypt(byte[] srcdata) {
        for(int i=0;i<srcdata.length;i++){
            srcdata[i] = (byte)(0xFF ^ srcdata[i]);
        }
        return srcdata;
    }




}
