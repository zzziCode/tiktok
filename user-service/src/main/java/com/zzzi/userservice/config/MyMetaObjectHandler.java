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
            this.strictInsertFill(metaObject, "createTime", Date.class, new Date());
            //更新时间不管什么时候都自动更新
            this.strictInsertFill(metaObject, "updateTime", Date.class, new Date());
            this.strictInsertFill(metaObject, "followCount", Integer.class, 0);
            this.strictInsertFill(metaObject, "followerCount", Integer.class, 0);
            this.strictInsertFill(metaObject, "avatar", String.class, "https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/tiktok_avatar.jpg");
            this.strictInsertFill(metaObject, "backgroundImage", String.class, "https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/background_image.jpg");
            this.strictInsertFill(metaObject, "signature", String.class, "谢谢你的关注");
            this.strictInsertFill(metaObject, "totalFavorited", Long.class, 0L);
            this.strictInsertFill(metaObject, "workCount", Integer.class, 0);
            this.strictInsertFill(metaObject, "favoriteCount", Integer.class, 0);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new UserException("属性自动填充失败");
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        try {
            //更新时间不管如何都直接填充
            this.strictUpdateFill(metaObject, "updateTime", Date.class, new Date());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new UserException("属性自动填充失败");
        }
    }
}
