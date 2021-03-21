package com.xuecheng.order.service;

import com.xuecheng.framework.domain.task.XcTask;
import com.xuecheng.framework.domain.task.XcTaskHis;
import com.xuecheng.order.dao.XcTaskHisRepository;
import com.xuecheng.order.dao.XcTaskRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {
    @Autowired
    XcTaskRepository xcTaskRepository;

    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    XcTaskHisRepository xcTaskHisRepository;




    //通过分页查询前分钟前的数据
    public List<XcTask> findXcTask(Date updataTime, int size) {
        //设置分页数据
        Pageable pageable = new PageRequest(0, size);
        //取出前n条任务,取出指定时间之前处理的任务
        Page<XcTask> xcTasks = xcTaskRepository.findByUpdateTimeBefore(pageable, updataTime);
        //取出所有的数据
        List<XcTask> list = xcTasks.getContent();
        return list;
    }

    /**
     * 发送消息
     *
     * @param xcTask     任务对象
     * @param ex         交换机id
     * @param routingKey routingKey
     */
    @Transactional
    public void publish(XcTask xcTask, String ex, String routingKey) {
        //查询任务
        Optional<XcTask> optional = xcTaskRepository.findById(xcTask.getId());
        if (optional.isPresent()) {
            rabbitTemplate.convertAndSend(ex, routingKey, xcTask);
            //更新任务时间为当前时间
            xcTask.setUpdateTime(new Date());
            xcTaskRepository.save(xcTask);
        }
    }

    /**
     * 通过对版本的版本更新控制，来限制谁进入
     *
     * @param version 版本
     * @param id
     * @return
     */
    @Transactional
    public int getXcTask(int version, String id) {
        //  返回的i，是正数的话就代表dao可操作，负数的话  就不代表dao里的条件不匹配，不能使用
        int i = xcTaskRepository.updateVersion(version, id);
        return i;
    }

    //完成任务
    @Transactional
    public void finishTask(String taskId) {
        Optional<XcTask> taskOptional = xcTaskRepository.findById(taskId);
        if (taskOptional.isPresent()) {
            XcTask xcTask = taskOptional.get();
            xcTask.setDeleteTime(new Date());
            XcTaskHis xcTaskHis = new XcTaskHis();
            BeanUtils.copyProperties(xcTask, xcTaskHis);
            xcTaskHisRepository.save(xcTaskHis);
            xcTaskRepository.delete(xcTask);
        }
    }
}
