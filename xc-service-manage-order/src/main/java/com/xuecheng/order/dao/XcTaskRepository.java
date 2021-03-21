package com.xuecheng.order.dao;

import com.xuecheng.framework.domain.task.XcTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;

public interface XcTaskRepository extends JpaRepository<XcTask,String> {
    //查询一分钟之前的数据
    Page<XcTask> findByUpdateTimeBefore(Pageable pageable, Date updatetime);

    //更新数据
    @Modifying
    @Query("update XcTask t set t.updateTime = :updateTime where t.id=:id")
    public int updateTaskTime(@Param(value = "id") String id, @Param(value = "updateTime") Date updateTime);

    //根据版本号的更新，限制只有一个能够进去
    @Modifying
    @Query("update XcTask t set t.version= :version where t.id= :id and t.version=version")
    public int updateVersion(@Param(value = "version")int  version,@Param("id") String id);
}
