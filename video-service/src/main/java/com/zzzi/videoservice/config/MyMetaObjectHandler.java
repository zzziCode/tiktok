package com.zzzi.videoservice.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.zzzi.common.exception.VideoException;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**@author zzzi
 * @date 2024/3/27 19:25
 * 在这里进行基本的属性回显
 * 更加复杂的属性回显后期操作
 * 所有实体的内容都可以在这里重新赋值，相当于共用一个填充器
 */
import java.util.Date;
@Component
@Slf4j
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        try {
            Object createTime = metaObject.getValue("createTime");
            if(createTime==null){
                this.setFieldValByName("createTime",new Date(),metaObject);
            }
            //更新时间不管什么时候都自动更新
            this.setFieldValByName("updateTime",new Date(),metaObject);
            Object title = metaObject.getValue("title");
            if(title==null){
                this.setFieldValByName("title","抖音记录美好生活",metaObject);
            }
            Object favoriteCount = metaObject.getValue("favoriteCount");
            if(favoriteCount==null){
                this.setFieldValByName("favoriteCount",0,metaObject);
            }
            Object commentCount = metaObject.getValue("commentCount");
            if(commentCount==null){
                this.setFieldValByName("commentCount",0,metaObject);
            }
        }catch (Exception e){
            log.error(e.getMessage());
            throw new VideoException("属性自动填充失败");
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        try {
            //更新时间不管如何都直接填充
            this.setFieldValByName("updateTime",new Date(),metaObject);
        }catch (Exception e){
            log.error(e.getMessage());
            throw new VideoException("属性自动填充失败");
        }
    }
}
