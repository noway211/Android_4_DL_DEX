#include "com_peter_source_dydex_jni_DyLoadDex.h"
#include "common.h"
#include <stdlib.h>
#include <dlfcn.h>
#include <stdio.h>

#include <android/log.h>

#define  LOG_TAG    "peter"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

JNINativeMethod *dvm_dalvik_system_DexFile;
void (*openDexFile)(const u4* args, union JValue* pResult);


int lookup(JNINativeMethod *table, const char *name, const char *sig,
           void (**fnPtrout)(u4 const *, union JValue *)) {
        int i = 0;
        while (table[i].name != NULL)
        {
                LOGI("lookup %d %s" ,i,table[i].name);
                if ((strcmp(name, table[i].name) == 0)
                    && (strcmp(sig, table[i].signature) == 0))
                {
                        *fnPtrout = table[i].fnPtr;
                        return 1;
                }
                i++;
        }
        return 0;
}

/* This function will be call when the library first be load.
 * You can do some init in the libray. return which version jni it support.
 */
JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {

void *ldvm = (void*) dlopen("libdvm.so", RTLD_LAZY);
        dvm_dalvik_system_DexFile = (JNINativeMethod*) dlsym(ldvm, "dvm_dalvik_system_DexFile");

        //openDexFile
        if(0 == lookup(dvm_dalvik_system_DexFile, "openDexFile", "([B)I",&openDexFile)) {
                openDexFile = NULL;
                LOGI("openDexFile method does not found ");
        }else{
                 LOGI("openDexFile method found ! HAVE_BIG_ENDIAN");
        }

        LOGI("ENDIANNESS is %c" ,ENDIANNESS );
        void *venv;
        LOGI("dufresne----->JNI_OnLoad!");
        if ((*vm)->GetEnv(vm, (void**) &venv, JNI_VERSION_1_4) != JNI_OK) {
                 LOGI("dufresne--->ERROR: GetEnv failed");
                 return -1;
        }
        return JNI_VERSION_1_4;
}

JNIEXPORT jint JNICALL Java_com_peter_source_dydex_jni_DyLoadDex_loadDexArray
  (JNIEnv* env, jclass jv, jbyteArray dexArray, jlong dexLen){
        u1 * olddata = (u1*)(*env)->  GetByteArrayElements(env,dexArray,NULL);
        char* arr;
        arr = (char*)malloc(16 + dexLen);
        ArrayObject *ao=(ArrayObject*)arr;
        ao->length = dexLen;
        memcpy(arr+16,olddata,dexLen);
        u4 args[] = { (u4) ao };
        union JValue pResult;
        jint result;
        if(openDexFile != NULL) {
            openDexFile(args,&pResult);
        }else{
            result = -1;
        }
        result = (jint) pResult.l;
        LOGI("Java_com_peter_source_dydex_jni_DyLoadDex_loadDexArray %d" , result);
        return result;

  }

