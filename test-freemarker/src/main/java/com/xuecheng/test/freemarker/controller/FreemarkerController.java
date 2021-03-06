package com.xuecheng.test.freemarker.controller;


import com.xuecheng.test.freemarker.model.Student;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RequestMapping("/freemarker")
@Controller//不要使用RestController 不然直接把return的值当json  返回到页面上了
public class FreemarkerController {

   @Autowired
   RestTemplate restTemplate;
    //根据id获取数据，并返回到页面上
    @RequestMapping("/banner")
    public String index_banner(Map<String,Object>map){
        ResponseEntity<Map> forEntity
                = restTemplate.getForEntity("http://localhost:31001/cms/config/getmodel/5a791725dd573c3574ee333f", Map.class);
        Map body = forEntity.getBody();
       // map.putAll(body);
        return "index_banner";
    }


    /*@Autowired
    RestTemplate restTemplate;*/

    /*@RequestMapping("/test1")
    public String freemarker(Map<String,Object>map){
        map.put("name", "黑马程序员");
        //返回模板文件名称
        return "test1";111
    }*/
    @RequestMapping("/test1")
    public String freemarker(Map<String, Object> map) {
        map.put("name", "黑马程序员");
        Student stu1 = new Student();
        stu1.setName("小明");
        stu1.setAge(18);
        stu1.setMoney(1000.86f);
        stu1.setBirthday(new Date());
        Student stu2 = new Student();
        stu2.setName("小周");
        stu2.setMoney(1000000000000000000.1f);
        stu2.setAge(22);
        List<Student> friends = new ArrayList<>();
        friends.add(stu1);
        stu2.setFriends(friends);
        stu2.setBestFriend(stu1);
        List<Student> stus = new ArrayList<>();
        stus.add(stu1);
        stus.add(stu2);
        //向数据模型放数据
        map.put("stus", stus);
        //准备map数据
        HashMap<String, Student> stuMap = new HashMap<>();
        stuMap.put("stu1", stu1);
        stuMap.put("stu2", stu2);
        //向数据模型放数据
        map.put("stu1", stu1);
        //向数据模型放数据
        map.put("stuMap", stuMap);
        //返回模板文件名称 check 当前
        return "test1";
    }

    @RequestMapping("/course")
    public String course(Map<String,Object>map){
        //11111  566
        ResponseEntity<Map> forEntity
                = restTemplate.getForEntity("http://localhost:31200/course/courseview/297e7c7c62b888f00162b8a965510001", Map.class);
        Map body = forEntity.getBody();
        map.putAll(body);
        return "course";
    }

}
