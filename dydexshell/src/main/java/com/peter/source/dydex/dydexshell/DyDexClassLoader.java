package com.peter.source.dydex.dydexshell;

import android.content.Context;
import android.util.Log;

import com.peter.source.dydex.dydexshell.util.Reflect;
import com.peter.source.dydex.jni.DyLoadDex;

import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;

/**
 * Created by Administrator on 2017/7/7.
 */

public class DyDexClassLoader extends DexClassLoader {
    static String TAG="peterLog";

    private Context context;
    int cookies;
    public DyDexClassLoader(Context context,byte[] dexArrays,String dexPath, String optimizedDirectory, String librarySearchPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, librarySearchPath, parent);
        this.context = context;
        if (dexArrays != null){
            cookies = DyLoadDex.loadDexArray(dexArrays,dexArrays.length);
            Log.e("peterLog","cookie="+cookies+" "+context.getClassLoader());
        }
    }

    private String[] getClassNameList(int cookie) {
        return  (String[])(Reflect.on(DexFile.class).call("getClassNameList",cookie).get());
    }

    private Class defineClass(String name, ClassLoader loader, int cookie) {
        return  (Class) (Reflect.on(DexFile.class).call("defineClassNative",name,loader,cookie).get());
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String slashName = name.replace('.', '/');
      //  return defineClass(slashName,this,cookies);
        Class<?> cls = null;
//        String as[] = getClassNameList(cookies);
//        Log.d(TAG, "getClassNameList" + as.length);
//        for (int z = 0; z < as.length; z++) {
//            if (as[z].equals(name)) {
//                cls = defineClass(slashName,
//                       context.getClassLoader(), cookies);
//            } else {
//                //加载其他类
//                defineClass(slashName, context.getClassLoader(),
//                        cookies);
//            }
//        }
        cls = defineClass(slashName,context.getClassLoader(),cookies);
        if (null == cls) {
            cls = super.findClass(name);
        }

        return cls;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = super.loadClass(name, resolve);
        if (null == clazz) {
            Log.e(TAG, "loadClass fail,maybe get a null-point exception.");
        }
        return clazz;
    }
}
