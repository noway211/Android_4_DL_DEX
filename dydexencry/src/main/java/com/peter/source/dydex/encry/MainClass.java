package com.peter.source.dydex.encry;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.zip.Adler32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class MainClass {


    public static void main(String[] args) {

        Map<String,String> paramg = ParamOpt.parseParam(args);
        String curdir = System.getProperty("user.dir");
        if(paramg.containsKey("")){
            System.out.println("cmd like this:  java -jar xxxx.jar -s file.apk -o dir");
            return ;
        }

        String sourceapk = paramg.get("s");
        String desDir = paramg.get("o");

        try {
            saveSourceDexFile(sourceapk,desDir);
            InputStream dexFileStream = Class.class.getResourceAsStream("/resource/petershell.dex");
            if(dexFileStream == null){
                throw new Exception("shell dex file is not find");
            }
            byte[] payloadArray = encrpt(readFileBytes(new File(desDir,"/tmp/classes")));
            byte[] unShellDexArray = readDexfileBytes(dexFileStream);
            dexFileStream.close();
            int payloadLen = payloadArray.length;
            int unShellDexLen = unShellDexArray.length;
            int totalLen = payloadLen + unShellDexLen + 4;
            byte[] newdex = new byte[totalLen];
            System.arraycopy(unShellDexArray, 0, newdex, 0, unShellDexLen);
            System.arraycopy(payloadArray, 0, newdex, unShellDexLen, payloadLen);
            System.arraycopy(intToByte(payloadLen), 0, newdex, totalLen - 4, 4);
            fixFileSizeHeader(newdex);
            fixSHA1Header(newdex);
            fixCheckSumHeader(newdex);

            String str =desDir+"/classes.dex";
            File file = new File(str);
            File parentfile = file.getParentFile();
            if(!parentfile.exists()){
                parentfile.mkdirs();
            }
            FileOutputStream localFileOutputStream = new FileOutputStream(file);
            localFileOutputStream.write(newdex);
            localFileOutputStream.flush();
            localFileOutputStream.close();
            deleteFile(new File(desDir,"tmp"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void deleteFile(File file){
        if(file.isFile()){
            file.delete();
        }else{
            String[] childFilePaths = file.list();
            for(String childFilePath : childFilePaths){
                File childFile=new File(file.getAbsolutePath()+"\\"+childFilePath);
                deleteFile(childFile);
            }
            file.delete();
        }
    }
    private static byte[] encrpt(byte[] srcdata) {
        for (int i = 0; i < srcdata.length; i++) {
            srcdata[i] = (byte) (0xFF ^ srcdata[i]);
        }
        return srcdata;
    }



    private static void saveSourceDexFile(String sourcefilepath,String outputDir) {
        ZipInputStream localZipInputStream = null;
        try {
            localZipInputStream = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(new File(sourcefilepath))));
            while (true){
                ZipEntry localZipEntry = localZipInputStream.getNextEntry();
                if (localZipEntry == null) {
                    localZipInputStream.close();
                    break;
                }
                String name = localZipEntry.getName();
                if(name.startsWith("classes")&&name.endsWith(".dex")){
                    File storeFile = new File(outputDir + "/tmp/"
                            + name);
                    File pfile = storeFile.getParentFile();
                    if(!pfile.exists()){
                        pfile.mkdirs();
                    }
                    storeFile.createNewFile();
                    FileOutputStream fos = new FileOutputStream(storeFile);
                    byte[] arrayOfByte = new byte[2048];
                    while (true) {
                        int i = localZipInputStream.read(arrayOfByte);
                        if (i == -1)
                            break;
                        fos.write(arrayOfByte, 0, i);
                    }
                    fos.flush();
                    fos.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                localZipInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File file = new File(outputDir,"tmp");
        File[] fileslist = file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if(name.endsWith(".dex")){
                    return  true;
                }
                return false;
            }
        });
        if(file != null && fileslist.length > 0){
            try {
                File mulDexFile = new File(outputDir,"/tmp/classes");
                RandomAccessFile savedFile = new RandomAccessFile(mulDexFile, "rw");
                savedFile.seek(0);
                savedFile.writeByte(fileslist.length);
                for(File subfile:fileslist){
                    writeFile(savedFile,subfile);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
        }


    }

    private static  void writeFile(RandomAccessFile accessFile, File file){
        try {
            FileInputStream fis = new FileInputStream(file);
            accessFile.writeInt(fis.available());
            byte[] tempb = new byte[2048];
            int length = 0;
            while((length = fis.read(tempb)) >0){
                accessFile.write(tempb,0,length);
            }
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }


    }
    /**
     *
     *
     * @param dexBytes
     */
    private static void fixCheckSumHeader(byte[] dexBytes) {
        Adler32 adler = new Adler32();
        adler.update(dexBytes, 12, dexBytes.length - 12);
        long value = adler.getValue();
        int va = (int) value;
        byte[] newcs = intToByte(va);
        //高位在前，低位在前掉个个
        byte[] recs = new byte[4];
        for (int i = 0; i < 4; i++) {
            recs[i] = newcs[newcs.length - 1 - i];
            System.out.println(Integer.toHexString(newcs[i]));
        }
        System.arraycopy(recs, 0, dexBytes, 8, 4);
        System.out.println(Long.toHexString(value));
        System.out.println();
    }


    /**
     * int 转byte[]
     *
     * @param number
     * @return
     */
    public static byte[] intToByte(int number) {
        byte[] b = new byte[4];
        for (int i = 3; i >= 0; i--) {
            b[i] = (byte) (number % 256);
            number >>= 8;
        }
        return b;
    }

    /**
     *
     *
     * @param dexBytes
     * @throws NoSuchAlgorithmException
     */
    private static void fixSHA1Header(byte[] dexBytes)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(dexBytes, 32, dexBytes.length - 32);
        byte[] newdt = md.digest();
        System.arraycopy(newdt, 0, dexBytes, 12, 20);
        String hexstr = "";
        for (int i = 0; i < newdt.length; i++) {
            hexstr += Integer.toString((newdt[i] & 0xff) + 0x100, 16)
                    .substring(1);
        }
        System.out.println(hexstr);
    }

    /**
     *
     *
     * @param dexBytes
     */
    private static void fixFileSizeHeader(byte[] dexBytes) {
        byte[] newfs = intToByte(dexBytes.length);
        System.out.println(Integer.toHexString(dexBytes.length));
        byte[] refs = new byte[4];
        for (int i = 0; i < 4; i++) {
            refs[i] = newfs[newfs.length - 1 - i];
            System.out.println(Integer.toHexString(newfs[i]));
        }
        System.arraycopy(refs, 0, dexBytes, 32, 4);

    }


    /**
     * 以二进制读出文件内容
     *
     * @param file
     * @return
     * @throws IOException
     */
    private static byte[] readFileBytes(File file) throws IOException {
        byte[] arrayOfByte = new byte[1024];
        ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
        FileInputStream fis = new FileInputStream(file);
        while (true) {
            int i = fis.read(arrayOfByte);
            if (i != -1) {
                localByteArrayOutputStream.write(arrayOfByte, 0, i);
            } else {
                fis.close();
                return localByteArrayOutputStream.toByteArray();
            }
        }
    }

    private static byte[] readDexfileBytes(InputStream fis) throws IOException {
        byte[] arrayOfByte = new byte[1024];
        ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
        //  FileInputStream fis = new FileInputStream(file);
        while (true) {
            int i = fis.read(arrayOfByte);
            if (i != -1) {
                localByteArrayOutputStream.write(arrayOfByte, 0, i);
            } else {
                return localByteArrayOutputStream.toByteArray();
            }
        }
    }
}
