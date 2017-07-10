package com.peter.source.dydex.dydexshell.util;


public class ActivityThreadCompat {

    private static Object sActivityThread;

    private static Class sClass;

    public synchronized static final Object instance() throws  ReflectException{
        if (sActivityThread == null) {
            sActivityThread = Reflect.on("android.app.ActivityThread").call("currentActivityThread").get();
        }
        return sActivityThread;
    }


}
