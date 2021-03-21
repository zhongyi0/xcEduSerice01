package com.xuecheng.ucenter.dao;

import com.xuecheng.framework.domain.ucenter.XcCompanyUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface XcCompantUserRepository extends JpaRepository<XcCompanyUser, String> {
    //根据用户Id,拿到公司id
    public XcCompanyUser findByUserId(String userId);
}
