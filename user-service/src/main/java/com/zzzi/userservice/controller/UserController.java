package com.zzzi.userservice.controller;


import com.zzzi.userservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/douyin/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;


    @PostMapping("/register")
    public void register(String username,String password){
    }

    @PostMapping("/login")
    public void login(String username,String password){

    }

    @GetMapping
    public void userInfo(String user_id,String token){
    }

}

