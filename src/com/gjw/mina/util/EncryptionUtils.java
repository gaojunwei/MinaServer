package com.gjw.mina.util;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * 常用  编码/解码  工具类
 *
 * @author gjw
 * @create 2017-12-02 20:33
 **/
public class EncryptionUtils {

    //得到文件的字节数组
    public static byte[] getFileByte(String path) throws Exception {
        File file = new File(path);
        FileInputStream inputFile = new FileInputStream(file);
        int offset = 0;
        int bytesRead = 0;
        byte[] data = new byte[(int) file.length()];
        while ((bytesRead = inputFile.read(data, offset, data.length - offset)) != -1)
        {
            offset += bytesRead;
            if (offset >= data.length) {
                break;
            }
        }
        return data;
    }


    /**
     * 将文件转为base64字符串
     * @param path
     * @return
     * @throws Exception
     */
    public static String base64EncodeFile(String path) throws Exception {
        byte[] buffer = EncryptionUtils.getFileByte(path);
        String s = new String(java.util.Base64.getEncoder().encode(buffer));
        return s;
    }
    
    public static String base64Encode(String data) {
        return base64Encode(data.getBytes());
    }

    public static String base64Encode(byte[] bytes) {
        return Base64.encodeBase64String(bytes);
    }

    public static byte[] base64Decode(String data) {
        return Base64.decodeBase64(data.getBytes());
    }

    //MD5
    public static String md5(String data) {
        return DigestUtils.md5Hex(data);
    }

    //sha1
    public static String sha1(String data) {
        return DigestUtils.sha1Hex(data);
    }

    //sha256Hex
    public static String sha256Hex(String data) {
        return DigestUtils.sha256Hex(data);
    }
}
