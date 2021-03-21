package com.xuecheng.manage_media.controller;

import com.xuecheng.api.media.MediaUploadControllerApi;
import com.xuecheng.framework.domain.media.response.CheckChunkResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_media.service.MediaUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/media/upload")
public class MediaUploadController implements MediaUploadControllerApi {
    @Autowired
    MediaUploadService mediaUploadService;

    @Override
    @PostMapping("/register")
    //文件上传注册
    public ResponseResult register( String fileMd5,
                                   String fileName,
                                   Long fileSize,
                                   String minetype,
                                   String fileExt) {
        return mediaUploadService.register(fileMd5,fileName,fileSize,minetype,fileExt);
    }

    @Override
    @PostMapping("/checkchunk")
    //校验分块文件是否存在
    public CheckChunkResult checkchunk(String fileMd5,
                                       Integer chunk,
                                      Integer chunkSize) {
        return mediaUploadService.checkchunk(fileMd5,chunk,chunkSize);
    }

    @Override
    @PostMapping("/uploadchunk")
    //上传分块
    public ResponseResult uploadchunk( MultipartFile file,
                                      String fileMd5,
                                     Integer chunk) {
        return mediaUploadService.uploadchunk(file,fileMd5,chunk);
    }

    @Override
    @PostMapping("/mergechunks")
    //合并分块
    public ResponseResult mergechunks( String fileMd5,
                                       String fileName,
                                       Long fileSize,
                                       String minetype,
                                       String fileExt) {
        return mediaUploadService.mergechunks(fileMd5,fileName,fileSize,minetype,fileExt);
    }
}
