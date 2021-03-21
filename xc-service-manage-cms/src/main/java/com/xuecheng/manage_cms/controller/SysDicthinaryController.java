package com.xuecheng.manage_cms.controller;

import com.xuecheng.api.course.SysDicthinaryControllerApi;
import com.xuecheng.framework.domain.system.SysDictionary;
import com.xuecheng.manage_cms.service.SysDicthinaryServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sysDictionary")
public class SysDicthinaryController implements SysDicthinaryControllerApi {
    @Autowired
    SysDicthinaryServer sysDicthinaryServer;

    @Override
    @GetMapping("/getByType/{type}")
    public SysDictionary getByType(@RequestParam("type")String type) {
        return sysDicthinaryServer.getByType(type);
    }
}
