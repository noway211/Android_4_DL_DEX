#!/usr/bin/env python
# -*- coding: utf-8 -*-

import game_conf
import os
import shutil
import time
import game_cmd
import game_menifest_op
import game_zip

rpaklib = os.path.abspath(game_conf.CACHE_DIR + "/repack")


def del_file(filepath):
    if os.path.isdir(filepath):
        shutil.rmtree(filepath)
    elif os.path.isfile(filepath):
        os.remove(filepath)

def sourcecpy(src, des):
    src = os.path.abspath(src)
    des = os.path.abspath(des)
    if os.path.exists(src) == False:
        print "src dir is not exist:"
        return False
    if not os.path.exists(des):
        os.makedirs(des)
    #获得原始目录中所有的文件，并拼接每个文件的绝对路径
    src_file = [os.path.join(src, file) for file in os.listdir(src)]
    for source in src_file:
        #若是文件
        if os.path.isfile(source):
            shutil.copy(source, os.path.join(des, os.path.basename(source)))
        # 若是目录
        if os.path.isdir(source):
            p, src_name = os.path.split(source)
            newdes = os.path.join(des, src_name)
            #print "re copy dir:"+source,newdes
            sourcecpy(source, newdes)  #第一个参数是目录，第二个参数也是目录
    return True


def cleanEnv():
    del_file(rpaklib+'/'+"source.apk")
    del_file(rpaklib+'/'+"new.apk")
    del_file(rpaklib+'/'+"source")
    del_file(rpaklib+'/'+"classes.dex")
    del_file(rpaklib+'/'+"tmp")

def repackApk(inputApk):
    print "sourceapk==="+inputApk
    #临时目录创建
    if os.path.exists(rpaklib) == False:
        os.makedirs(rpaklib)

    #拷贝文件到临时文件夹
    try:
        cacheApkFile = rpaklib+'/'+"source.apk"
        del_file(cacheApkFile)
        shutil.copy(inputApk, cacheApkFile)
    except:
        game_conf.exit(game_conf.EXIT_COPYING_ERR, "copy apk to temp dir fail")


    #删除反编译的目录
    temp_apk_recompile_dir = rpaklib+"/source"
    del_file(temp_apk_recompile_dir)

    #反编译apk
    if game_cmd.run_shell_cmd_nopipe(game_conf.APKTOOL+ " d "+cacheApkFile + " -o "+temp_apk_recompile_dir) == False:
        game_conf.exit(game_conf.EXIT_DECODE_APK_ERR, "decompile apk fail")


    #解压apk
    temp_apk_upzip_dir = rpaklib+"/unzip"
    del_file(temp_apk_upzip_dir)
    game_zip.unzip_file(os.path.join(os.path.dirname(game_conf.SIGNAPK),"petershell.jar"),temp_apk_upzip_dir)

    #拷贝jni库
    sourcecpy(temp_apk_upzip_dir+'/resource/jni',temp_apk_recompile_dir+"/lib")

    print "begin create new dex"
    #合并新的dex
    if game_cmd.shell_run_ok(game_conf.PETERSHELL+" -s "+cacheApkFile+" -o "+rpaklib) == False:
        game_conf.exit(game_conf.EXIT_CREATE_DEX_ERR,"create new dex fail")


    #删除原工程的无用文件
    file_array = os.listdir(temp_apk_recompile_dir)
    for dex_file in file_array:
        if dex_file.startswith('smali'):
            del_file(temp_apk_recompile_dir+"/"+dex_file);




    #拷贝新的dex到编译目录
    shutil.copy(rpaklib+"/classes.dex", temp_apk_recompile_dir+"/classes.dex")


    #修改androidmenifest
    game_menifest_op.modifypackage(temp_apk_recompile_dir+"/AndroidManifest.xml")



    if game_cmd.run_shell_cmd_nopipe(game_conf.APKTOOL+ " b "+temp_apk_recompile_dir + " -o "+rpaklib+ "/new.apk") == False:
        game_conf.exit(game_conf.EXIT_REPACKAGE_ERR, "apk repack fail")

    #签名
    command = game_conf.SIGNAPK + " "+game_conf.SIGNKEY_PEM +" "+game_conf.SIGNKEY_PK8 +" "+rpaklib+"/new.apk " +rpaklib + "/new_signed.apk"
    if game_cmd.shell_run_ok(command) == False:
        cleanEnv()
        game_conf.exit(game_conf.EXIT_SIGNAPK_ERR, "jarsigner apk fail")

    #添加新new到目标目录
    if game_conf.OUTPUTNAME == '':
        os.rename(rpaklib +"/new_signed.apk", game_conf.OUTPUTDIR + "/repack_" + str(time.time()) + ".apk")
    else:
        if os.path.exists(os.path.join(game_conf.OUTPUTDIR,game_conf.OUTPUTNAME)):
            os.remove(os.path.join(game_conf.OUTPUTDIR,game_conf.OUTPUTNAME))
        os.rename(rpaklib +"/new_signed.apk", os.path.join(game_conf.OUTPUTDIR,game_conf.OUTPUTNAME))
    #清除目录
   # cleanEnv()

