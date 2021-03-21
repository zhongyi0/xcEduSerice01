package com.xuecheng.manage_cms.service;

import com.xuecheng.framework.domain.system.SysDictionary;
import com.xuecheng.manage_cms.dao.SysDicthinaryDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SysDicthinaryServer {
    @Autowired
    SysDicthinaryDao sysDicthinaryDao;

    public SysDictionary getByType(String type) {
        return sysDicthinaryDao.findByDType(type);
    }


}
