package com.zzzi.userservice.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.zzzi.common.exception.UserException;
import com.zzzi.common.exception.VideoException;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
/**@author zzzi
 * @date 2024/3/27 19:25
 * 在这里进行基本的属性回显
 * 更加复杂的属性回显后期操作
 */
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        try {
            Object createTime = metaObject.getValue("createTime");
            if (createTime == null) {
                this.setFieldValByName("createTime", new Date(), metaObject);
            }
            //更新时间不管什么时候都自动更新
            this.setFieldValByName("updateTime", new Date(), metaObject);
            Object followCount = metaObject.getValue("followCount");
            if (followCount == null) {
                this.setFieldValByName("followCount", 0, metaObject);
            }

            Object followerCount = metaObject.getValue("followerCount");
            if (followerCount == null) {
                this.setFieldValByName("followerCount", 0, metaObject);
            }
            Object avatar = metaObject.getValue("avatar");
            if (avatar == null) {
                this.setFieldValByName("avatar",
                        "https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/tiktok_avatar.jpg",
                        metaObject);
            }
            Object backgroundImage = metaObject.getValue("backgroundImage");
            if (backgroundImage == null) {
                this.setFieldValByName("backgroundImage",
                        "https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/background_image.jpg",
                        metaObject);
            }

            Object signature = metaObject.getValue("signature");
            if (signature == null) {
                this.setFieldValByName("signature", "谢谢你的关注", metaObject);
            }

            Object totalFavorited = metaObject.getValue("totalFavorited");
            if (totalFavorited == null) {
                this.setFieldValByName("totalFavorited", 0L, metaObject);
            }

            Object workCount = metaObject.getValue("workCount");
            if (workCount == null) {
                this.setFieldValByName("workCount", 0, metaObject);
            }

            Object favoriteCount = metaObject.getValue("favoriteCount");
            if (favoriteCount == null) {
                this.setFieldValByName("favoriteCount", 0, metaObject);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new UserException("属性自动填充失败");
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        try {
            //更新时间不管如何都直接填充
            this.setFieldValByName("updateTime", new Date(), metaObject);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new UserException("属性自动填充失败");
        }
    }
}
