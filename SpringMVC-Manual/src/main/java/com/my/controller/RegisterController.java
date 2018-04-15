package com.my.controller;


import com.my.annotation.MyAutoWired;
import com.my.annotation.MyController;
import com.my.annotation.MyRequestMapping;
import com.my.annotation.MyRequestParam;
import com.my.service.RegisterService;

@MyController
@MyRequestMapping("/user")
public class RegisterController {

    @MyAutoWired
    public RegisterService registerService;

    @MyRequestMapping("/register")
    public void regist(){
        registerService.regist();
    }

    @MyRequestMapping("/registWithParam")
    public void registWithParam(@MyRequestParam("userName") String userName, @MyRequestParam("password") String password){
        registerService.registWithParam(userName, password);
    }
}
