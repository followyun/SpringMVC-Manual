package com.my.service.impl;

import com.my.annotation.MyService;
import com.my.service.RegisterService;

/**
 */
@MyService
public class CustomerRegisterService implements RegisterService{

    public void regist() {
        System.out.println("注册。。。");
    }

    public void registWithParam(String userName, String password) {
        System.out.println("注册！userName："+userName +"\n password："+password);
    }

}
