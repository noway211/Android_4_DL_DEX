set classpath=.\\build\\intermediates\\classes\\debug
set jniclasspath=.\\src\\main\\java\\com\\peter\\source\\dydex\\jni
set jnidir=.\\src\\main\\jni

if not exist %jnidir% mkdir %jnidir%
if not exist %classpath% mkdir %classpath%

echo "gen DyLoadDex.java jni"
javac -d  %classpath%  %jniclasspath%\\DyLoadDex.java
javah -classpath %classpath% -d %jnidir% -jni com.peter.source.dydex.jni.DyLoadDex
