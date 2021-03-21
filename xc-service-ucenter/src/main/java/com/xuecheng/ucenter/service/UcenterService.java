package com.xuecheng.ucenter.service;

import com.xuecheng.framework.domain.ucenter.XcCompanyUser;
import com.xuecheng.framework.domain.ucenter.XcMenu;
import com.xuecheng.framework.domain.ucenter.XcUser;
import com.xuecheng.framework.domain.ucenter.ext.XcUserExt;
import com.xuecheng.ucenter.dao.XcCompantUserRepository;
import com.xuecheng.ucenter.dao.XcMenuMapper;
import com.xuecheng.ucenter.dao.XcUserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UcenterService {
    @Autowired
    XcCompantUserRepository xcCompantUserRepository;

    @Autowired
    XcUserRepository xcUserRepository;

    @Autowired
    XcMenuMapper xcMenuMapper;

    //根据名字查到，用户的基础信息
    public XcUser findByUsername(String username){
       return xcUserRepository.findXcUserByUsername(username);
    }

    //根据用户的基本信息，返回用户的扩展信息（如：公司id）
    public XcUserExt getUserExt(String username) {
        XcUser byUsername = this.findByUsername(username);
        if (byUsername==null){
            return null;
        }
        //得到用户的id
        String id1 = byUsername.getId();
        //通过用户id获取权限
        List<XcMenu> xcMenus = xcMenuMapper.selectPermissionByUserId(id1);

        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(byUsername,xcUserExt);

        //设置权限进去
        xcUserExt.setPermissions(xcMenus);
        //得到用户的id
        String id = xcUserExt.getId();
        //通过用户id查找 ，扩展信息，并塞进去
        XcCompanyUser companyId = xcCompantUserRepository.findByUserId(id);
        //有些人是没有公司id的
        if (companyId!=null){
            xcUserExt.setCompanyId(companyId.getCompanyId());
            System.out.println(xcUserExt);
            return xcUserExt;
        }
        return xcUserExt;
    }
}
