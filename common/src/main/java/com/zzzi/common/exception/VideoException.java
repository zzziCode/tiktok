package com.zzzi.common.exception;

/**@author zzzi
 * @date 2024/3/27 14:59
 * 所有的视频模块异常使用这个类抛出
 */
public class VideoException extends RuntimeException{
    public VideoException(String message) {
        super(message);
    }
}
