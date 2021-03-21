package com.xuecheng.manage_cms_client.mq;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.manage_cms_client.dao.CmsPageRepository;
import com.xuecheng.manage_cms_client.service.PageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class ConsumerPostPage {
    private static Logger LOOGER = LoggerFactory.getLogger(ConsumerPostPage.class);
    @Autowired
    PageService pageService;
    @Autowired
    CmsPageRepository cmsPageRepository;


    @RabbitListener(queues = {"${xuecheng.mq.queue}"})//要与application.yml配置文件得一致
    public void postPage(String msg) {
        //将传来的信息，转换成map信息
        Map map = JSON.parseObject(msg, Map.class);
        String pageId = (String) map.get("pageId");
        //查询页面信息
        Optional<CmsPage> optional = cmsPageRepository.findById(pageId);
        if (!optional.isPresent()) {
            LOOGER.error("获取到的id错误，找不到页面");
        }
        //将页面保存到服务器的物理路径
        pageService.savePageToServerPath(pageId);
    }
}
