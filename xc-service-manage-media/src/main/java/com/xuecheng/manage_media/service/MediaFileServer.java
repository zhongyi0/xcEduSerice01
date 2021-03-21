package com.xuecheng.manage_media.service;

import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.request.QueryMediaFileRequest;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.manage_media.dao.MediaFileRepository;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MediaFileServer {
    @Autowired
    MediaFileRepository mediaFileRepository;

    //文件列表分页查询
    public QueryResponseResult findList(int page, int size, QueryMediaFileRequest queryMediaFileRequest) {
        MediaFile mediaFile = new MediaFile();
        if (queryMediaFileRequest==null){
            queryMediaFileRequest=new QueryMediaFileRequest();
        }
        //条件查询匹配器
        ExampleMatcher exampleMatcher = ExampleMatcher.matching().withMatcher("tag", ExampleMatcher.GenericPropertyMatchers.contains())//对于tag字段模糊查询
                .withMatcher("fileOriginalName", ExampleMatcher.GenericPropertyMatchers.contains())//原始名模糊查询
                .withMatcher("processStatus", ExampleMatcher.GenericPropertyMatchers.exact());//状态精确查询

        if (StringUtils.isNotEmpty(queryMediaFileRequest.getTag())){
            mediaFile.setTag(queryMediaFileRequest.getTag());
        }
        if (StringUtils.isNotEmpty(queryMediaFileRequest.getFileOriginalName())){
            mediaFile.setFileOriginalName(queryMediaFileRequest.getFileOriginalName());
        }
        if (StringUtils.isNotEmpty(queryMediaFileRequest.getProcessStatus())){
            mediaFile.setProcessStatus(queryMediaFileRequest.getProcessStatus());
        }
        //定义Example实例     将查询的内容，和查询条件塞进去
        Example<MediaFile> ex = Example.of(mediaFile, exampleMatcher);

        if (page<=0){
            page=1;
        }
        page=page-1;
        //把分页页数和数目塞进去
        Pageable pageable=new PageRequest(page,size);
        //分页查询
        Page<MediaFile> all = mediaFileRepository.findAll(ex, pageable);
        //数量
        long totalElements = all.getTotalElements();
        //数据列表
        List<MediaFile> content = all.getContent();
        QueryResult<MediaFile> queryResult = new QueryResult<>();
        queryResult.setTotal(totalElements);
        queryResult.setList(content);
        return new QueryResponseResult(CommonCode.SUCCESS,queryResult);

    }
}
