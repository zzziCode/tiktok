package com.zzzi.videoservice.interceptor;

import com.zzzi.common.exception.UserException;
import com.zzzi.common.exception.VideoException;
import com.zzzi.common.result.CommonVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author zzzi
 * @date 2024/3/26 22:34
 * 在这里处理videoservice中的所有异常
 */
@ControllerAdvice(annotations = {RestController.class})
@ResponseBody
@Slf4j
public class GlobalExceptionHandler {


    @ExceptionHandler(VideoException.class)
    public CommonVO VideoExceptionHandler(UserException ex) {
        log.error(ex.getMessage());

        if (ex.getMessage().contains("视频上传失败")) {
            return CommonVO.fail("视频上传失败");
        }

        if (ex.getMessage().contains("视频保存失败")) {
            return CommonVO.fail("视频保存失败");
        }
        if (ex.getMessage().contains("属性自动填充失败")) {
            return CommonVO.fail("属性自动填充失败");
        }
        if (ex.getMessage().contains("视频已经存在")) {
            return CommonVO.fail("视频已经存在");
        }

        if (ex.getMessage().contains("获取用户作品列表失败")) {
            return CommonVO.fail("获取用户作品列表失败");
        }

        if (ex.getMessage().contains("当前用户未登录")) {
            return CommonVO.fail("当前用户未登录");
        }


        return CommonVO.fail("未知错误");
    }
    @ExceptionHandler(UserException.class)
    public CommonVO UserExceptionHandler(UserException ex) {
        log.error(ex.getMessage());

        if (ex.getMessage().contains("当前用户未登录")) {
            return CommonVO.fail("当前用户未登录");
        }


        return CommonVO.fail("未知错误");
    }
}
