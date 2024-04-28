package com.zzzi.common.exception;

/**@author zzzi
 * @date 2024/3/30 15:17
 * 用户关系的异常，好友列表，粉丝列表，关注列表
 */
public class RelationException extends RuntimeException{
    public RelationException(String message) {
        super(message);
    }

}
