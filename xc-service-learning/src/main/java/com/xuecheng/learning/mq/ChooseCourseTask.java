package com.xuecheng.learning.mq;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.task.XcTask;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.learning.config.RabbitMQConfig;
import com.xuecheng.learning.service.CourseLearningServer;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Component
public class ChooseCourseTask {
    @Autowired
    CourseLearningServer courseLearningServer;

    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     * 接收选课任务
     */
    @RabbitListener(queues = {RabbitMQConfig.XC_LEARNING_ADDCHOOSECOURSE})
    public void receiveChoosecourseTask(XcTask xcTask) {
        String requestBody = xcTask.getRequestBody();
        Map map = JSON.parseObject(requestBody, Map.class);
        String userId = (String) map.get("userId");
        String courseId = (String) map.get("courseId");
        String valid = (String) map.get("valid");
        Date startTime = null;
        Date endTime = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY‐MM‐dd HH:mm:ss");
        if (map.get("startTime") != null) {
            try {
                startTime = dateFormat.parse((String) map.get("startTime"));
            } catch (ParseException e) {
                e.printStackTrace();
            }

        }
        if (map.get("endTime") != null) {
            try {
                endTime = dateFormat.parse((String) map.get("endTime"));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        //添加选课
        ResponseResult addcourse = courseLearningServer.addcourse(userId, courseId, valid, startTime, endTime, xcTask);
        //选课成功发送响应消息
        if (addcourse.isSuccess()){
            //发送响应消息
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EX_LEARNING_ADDCHOOSECOURSE,
                    RabbitMQConfig.XC_LEARNING_FINISHADDCHOOSECOURSE_KEY,
                    xcTask );
        }
    }

}
