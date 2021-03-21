package com.xuecheng.ucenter.dao;

import com.xuecheng.framework.domain.ucenter.XcUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface XcUserRepository extends JpaRepository<XcUser, String> {
    //通过姓名，拿到基本信息
    public XcUser findXcUserByUsername(String username);


}
