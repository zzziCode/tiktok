package com.zzzi.userservice.interceptor;

import com.zzzi.common.exception.FollowException;
import com.zzzi.common.exception.UserException;
import com.zzzi.common.result.CommonVO;
import com.zzzi.userservice.result.UserRegisterLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.LoginException;


/**
 * @author zzzi
 * @date 2024/3/26 22:34
 * 在这里处理userservice中的所有异常
 */
@ControllerAdvice(annotations = {RestController.class})
@ResponseBody
@Slf4j
public class GlobalExceptionHandler {


    @ExceptionHandler(UserException.class)
    public UserRegisterLoginVO RegisterExceptionHandler(UserException ex) {
        log.error(ex.getMessage());

        if (ex.getMessage().contains("邮箱格式不正确")) {
            return UserRegisterLoginVO.fail("邮箱格式不正确");
        }
        if (ex.getMessage().contains("用户名被占用，请重新输入用户名")) {
            return UserRegisterLoginVO.fail("用户名被占用，请重新输入用户名");
        }
        if (ex.getMessage().contains("登录失败，请确认是否注册或者用户名和密码是否正确")) {
            return UserRegisterLoginVO.fail("登录失败，请确认是否注册或者用户名和密码是否正确");
        }
        if (ex.getMessage().contains("用户未登录，请先去登录")) {
            return UserRegisterLoginVO.fail("用户未登录，请先去登录");
        }
        if (ex.getMessage().contains("更新用户信息失败")) {
            return UserRegisterLoginVO.fail("更新用户信息失败");
        }
        if (ex.getMessage().contains("当前用户已经登录，请不要重复登录")) {
            return UserRegisterLoginVO.fail("当前用户已经登录，请不要重复登录");
        }
        return UserRegisterLoginVO.fail("未知错误");
    }

    @ExceptionHandler(LoginException.class)
    public CommonVO LoginExceptionHandler(LoginException ex) {
        log.error(ex.getMessage());
        return CommonVO.fail("请先登录");
    }

    @ExceptionHandler(Exception.class)
    public CommonVO CommonExceptionHandler(Exception ex) {
        log.error(ex.getMessage());
        return CommonVO.fail("未知错误");
    }

    @ExceptionHandler(FollowException.class)
    public CommonVO FollowExceptionHandler(FollowException ex) {
        log.error(ex.getMessage());
        if (ex.getMessage().contains("关注失败，不能重复关注")) {
            return CommonVO.fail("关注失败，不能重复关注");
        }
        if (ex.getMessage().contains("取消关注失败")) {
            return CommonVO.fail("取消关注失败");
        }
        return CommonVO.fail("未知错误");
    }
}
