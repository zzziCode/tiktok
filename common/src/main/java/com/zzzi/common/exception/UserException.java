package com.zzzi.common.exception;

/**@author zzzi
 * @date 2024/3/26 21:33
 * 自定义的注册异常类，需要继承运行时异常
 */
public class UserException extends RuntimeException{
    public UserException(String message) {
        super(message);
    }

}
