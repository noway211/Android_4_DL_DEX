package com.peter.source.dydex.jni;

/**
 * Created by Administrator on 2017/7/6.
 */

public class DyLoadDex {

    static {
        System.loadLibrary("dyloaddex");
    }

    public static native int loadDexArray(byte[] dexBytes,long length);
}
