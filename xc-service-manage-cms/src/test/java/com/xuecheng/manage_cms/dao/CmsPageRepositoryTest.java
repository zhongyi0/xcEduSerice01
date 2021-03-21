package com.xuecheng.manage_cms.dao;


import com.xuecheng.framework.domain.cms.CmsPage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@SpringBootTest
@RunWith(SpringRunner.class)
public class CmsPageRepositoryTest {

    @Autowired
    CmsPageRepository cmsPageRepository;

    @Autowired
    RestTemplate restTemplate;

    @Test
    public void findall() {
        List<CmsPage> all = cmsPageRepository.findAll();
        System.out.println(all);
    }

    @Test
    public void findPage() {
        int page = 0;//页码是从0开始
        int size = 10;
        Pageable request = PageRequest.of(page, size);

        Page<CmsPage> all = cmsPageRepository.findAll(request);
        System.out.println(all);
    }

    @Test
    public void test03() {
        Optional<CmsPage> optional = cmsPageRepository.findById("5a754adf6abb500ad05688d9");
        if (optional.isPresent()) {
            CmsPage cmsPage = optional.get();
            cmsPage.setPageAliase("test001");
            CmsPage save = cmsPageRepository.save(cmsPage);
            System.out.println(save);
        }

    }


    @Test
    public void test04() {
        //设置分页信息
        int size = 0;
        int page = 10;
        PageRequest of = PageRequest.of(size, page);

        //创建空实体类
        CmsPage cmsPage = new CmsPage();
        //设置站点id
       // cmsPage.setSiteId("5a751fab6abb5044e0d19ea1");
        //设置模板ID
        //cmsPage.setTemplateId("5a925be7b00ffc4b3c1578b5");

        cmsPage.setPageAliase("轮播图");

        //页面别名模糊匹配查询
        //建立一个空的 条件匹配器
        ExampleMatcher exampleMatcher = ExampleMatcher.matching();
        exampleMatcher.withMatcher("pageAliase", ExampleMatcher.GenericPropertyMatchers.contains());
        //页面别名模糊查询，需要自定义字符串的匹配器实现模糊查询
        // ExampleMatcher.GenericPropertyMatchers.contains() 包含
        // ExampleMatcher.GenericPropertyMatchers.startsWith()//开头匹配
        Example<CmsPage> example = Example.of(cmsPage);

        Page<CmsPage> all = cmsPageRepository.findAll(example, of);
        System.out.println(all);


    }
}
