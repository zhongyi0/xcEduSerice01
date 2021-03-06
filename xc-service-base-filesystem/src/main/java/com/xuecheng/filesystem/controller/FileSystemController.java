package com.xuecheng.filesystem.controller;

import com.xuecheng.api.filesystem.FileSystemControllerApi;
import com.xuecheng.filesystem.service.FileSystemService;
import com.xuecheng.framework.domain.filesystem.response.UploadFileResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/filesystem")
public class FileSystemController implements FileSystemControllerApi {
    @Autowired
    FileSystemService fileSystemService;

    @Override
    @PostMapping("/upload")
    public UploadFileResult upload(@RequestParam("multipartFile") MultipartFile multipartFile,
                                   @RequestParam(value = "filetag",required = true)String filetag,
                                   @RequestParam(value = "businesskey",required = false)String businesskey,
                                   @RequestParam(value = "metadata",required = false)String metadata) {
        UploadFileResult upload = fileSystemService.upload(multipartFile, filetag, businesskey, metadata);
        return upload;
    }
}
