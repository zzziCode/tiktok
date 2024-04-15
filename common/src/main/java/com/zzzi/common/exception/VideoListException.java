package com.zzzi.common.exception;

/**@author zzzi
 * @date 2024/3/27 14:59
 * 获取用户作品异常类
 */
public class VideoListException extends RuntimeException{
    public VideoListException(String message) {
        super(message);
    }
}
