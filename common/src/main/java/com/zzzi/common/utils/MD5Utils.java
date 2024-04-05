package com.zzzi.common.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author zzzi
 * @date 2024/3/25 18:57
 * 将密码使用MD5加密，保证数据的安全性
 * 查询时先将密码加密，然后传入数据库比对
 */
public class MD5Utils {
    //定义一个随机的盐值
    public static final String SALT = "fdfa5e5a88bebae640a5d88e7c84708";

    public static String parseStrToMd5L32(String str) {
        // 将字符串转换为32位小写MD5
        String reStr = null;
        try {
            str += SALT;
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            //得到加密后的值
            byte[] bytes = md5.digest(str.getBytes());
            StringBuffer stringBuffer = new StringBuffer();
            for (byte b : bytes) {
                //转换成无符号整型
                int bt = b & 0xff;
                if (bt < 16) {
                    stringBuffer.append(0);
                }
                stringBuffer.append(Integer.toHexString(bt));
            }
            reStr = stringBuffer.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return reStr;
    }
}
