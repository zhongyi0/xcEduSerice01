package com.xuecheng.manage_cms.dao;


import com.xuecheng.manage_cms.service.PageService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)
public class PageServiceTest {

    @Autowired
    PageService pageService;


    @Test
    public void findall() {
        String pageHtml = pageService.getPageHtml("5dea6876e63e5d2ff51fc6e9");
        Map<String,Integer> map=new HashMap();
        map.put("id",123);
        map.put("id2",123);
        map.put("id3",123);
        for (String s : map.keySet()) {
            System.out.println(s);
        }
        System.out.println(pageHtml);
    }
}
