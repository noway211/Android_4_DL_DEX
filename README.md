# Android_4_DL_DEX
android4.0动态加载dex
1. 这个是个加固的demo
2.这个项目是是用android4.x系统中通过 dexFile源码有 openDexFile(byte[]) 来加载dex文件。通过自定义classloader的findClass方法，实现类加载。
  这个保证在脱壳后，本地不会保存dex方法。
  
  
运行方法：
     gradle :pythonlaunch:exec
  




