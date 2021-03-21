package com.xuecheng.auth;

import com.alibaba.fastjson.JSON;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
@SpringBootTest
@RunWith(SpringRunner.class)
public class Test02 {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void test01(){
        //定义key
        String key = "user_token:56a9bcd6-ff15-4f40-ae16-64da0ebb4392";
        //定义Map
        HashMap<String, String> map = new HashMap<>();
        map.put("access_token", "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb21wYW55SWQiOm51bGwsInVzZXJwaWMiOm51bGwsInVzZXJfbmFtZSI6Iml0Y2FzdCIsInNjb3BlIjpbImFwcCJdLCJuYW1lIjpudWxsLCJ1dHlwZSI6bnVsbCwiaWQiOm51bGwsImV4cCI6MTU3NzMxNTAyNiwianRpIjoiNTZhOWJjZDYtZmYxNS00ZjQwLWFlMTYtNjRkYTBlYmI0MzkyIiwiY2xpZW50X2lkIjoiWGNXZWJBcHAifQ.LZsHgUKh54hgniXHnq3_UgSYBca4wdZKBldiWYswAcchnaIxjjzSF10_hyKM8ZPNEI7ir3QmLUVTEEeyXWLO67RuFeipIvMsNre7TSVjmEE2bleBRSHeY8uUbpwIHdOSarXzDZDTbkSIu_uLeRFkvUKGEigzm5dAmU2WzJChnujwkyxCq3KulgULAvJL0LAQkEgeS8ijqtXA_L11Wp8VO-cy1hH7pG2X9Ygb37yll9OieWBTmdyeS3LQOjISEzbE2QgRoKL4Weq-yNTeZtHB_IOhcb3TLxBoBucqNUznpduuE3a6Ugl0qTdI7BmpUNFP-iXQJQuoJOA6FYr-VYMVoQ");
        map.put("refresh_token", "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb21wYW55SWQiOm51bGwsInVzZXJwaWMiOm51bGwsInVzZXJfbmFtZSI6Iml0Y2FzdCIsInNjb3BlIjpbImFwcCJdLCJhdGkiOiI1NmE5YmNkNi1mZjE1LTRmNDAtYWUxNi02NGRhMGViYjQzOTIiLCJuYW1lIjpudWxsLCJ1dHlwZSI6bnVsbCwiaWQiOm51bGwsImV4cCI6MTU3NzMxNTAyNiwianRpIjoiM2M2MWM5NmItNmVhNi00ODg5LTk2ODgtYTA3NWMzMmNlNTI1IiwiY2xpZW50X2lkIjoiWGNXZWJBcHAifQ.jicSXeOT62npai2KN4XVDh-Vy2rxTNCy24USzdz3VEd9G771XgCRAmxSOuNY7JBqSZU7PyGA7jQQPM3wwS7p4uiZGiBS2r6B7bUl-g50IexXY2ItVqtmP90K-fktnYyZ9MQcaBVONHUOrszTN3sJnkujWzY7gO9cyjiAgB6IY8HG01bgc_ZWPI_BH13bAgT_fDufZfyJqojGQk5JxO6G04kRWmMw2qMdUsGuOR55oDb64Hz1iJBSnUrivfoTyztXFw0kQ4284rNpKuBI7bzqEsRgmuV0xttzJLXLQGwG8bPJ333y3-FFsjSZA10aPGobxU1g647cNOzb3cFDcVC88w");
        String value = JSON.toJSONString(map);
        //向redis中存储字符串
        stringRedisTemplate.boundValueOps(key).set(value, 60, TimeUnit.SECONDS);
        //读取国企时间，已经过期就会返回-2
        Long expire = stringRedisTemplate.getExpire(key);
        System.out.println(expire);
        //根据key来获取value
        String s = stringRedisTemplate.opsForValue().get(key);
        System.out.println(s);
    }
}
