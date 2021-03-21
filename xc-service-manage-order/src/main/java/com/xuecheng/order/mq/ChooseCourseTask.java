package com.xuecheng.order.mq;

import com.xuecheng.framework.domain.task.XcTask;
import com.xuecheng.order.config.RabbitMQConfig;
import com.xuecheng.order.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sun.plugin2.message.Message;

import java.io.IOException;
import java.nio.channels.Channel;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;


@Component
public class ChooseCourseTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChooseCourseTask.class);

    @Autowired
    TaskService taskService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = {RabbitMQConfig.XC_LEARNING_FINISHADDCHOOSECOURSE})
    public void receiveFinishChoosecourseTask(XcTask task, Message message, Channel channel) throws IOException {
        LOGGER.info("receiveChoosecourseTask...{}", task.getId());
        //接收到 的消息id
        String id = task.getId();
        //删除任务，添加历史任务
        taskService.finishTask(id);
    }

        //每个1分钟扫描消息表，向mq发送消息
        @Scheduled(cron = "0/3 1 * * * *")
        public void sendChoosecoursseTask () {
            //取出当前时间1分钟之前的时间
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(new Date());
            calendar.add(GregorianCalendar.MINUTE, -1);

            Date time = calendar.getTime();
            List<XcTask> xcTask = taskService.findXcTask(time, 1000);
            //遍历任务列表

            for (XcTask task : xcTask) {
                //如果谁先抢到线程的，就能执行他
                if (taskService.getXcTask(task.getVersion(), task.getId()) >= 0) {
                    //发送选课信息
                    String ex = task.getMqExchange();//交换机
                    String routingkey = task.getMqRoutingkey();//routingkey
                    taskService.publish(task, ex, routingkey);
                }
            }
        }
    }
