package com.xuecheng.manage_cms.dao;

import com.xuecheng.framework.domain.cms.CmsSite;
import org.springframework.data.mongodb.repository.MongoRepository;

//创建Dao，继承MongoRepository，并指定实体类型和主键类型。
public interface CmsSiteRepository extends MongoRepository<CmsSite,String> {
}
