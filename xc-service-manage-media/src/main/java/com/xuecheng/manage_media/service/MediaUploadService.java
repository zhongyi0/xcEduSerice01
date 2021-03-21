package com.xuecheng.manage_media.service;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.response.CheckChunkResult;
import com.xuecheng.framework.domain.media.response.MediaCode;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_media.config.RabbitMQConfig;
import com.xuecheng.manage_media.dao.MediaFileRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@Service
public class MediaUploadService {
    @Autowired
    MediaFileRepository mediaFileRepository;

    @Value("${xc-service-manage-media.upload-location}")
    //是视频存放目录
    String uploadPath;

    @Value("${xc-service-manage-media.mq.routingkey-media-video}")
    //是 消息队列的routingkey 与交换机对于
    String routing_media_video;

    @Autowired
    RabbitTemplate rabbitTemplate;
    /*
     * 根据文件md5得到文件路径
     *规则：
     * 一级目录：md5的第一个字符
     * 二级目录：md5的第二个字符
     * 三级目录：md5
     * 文件名：md5+文件扩展名
     * @param fileMd5 文件md5值
     * @param fileExt 文件扩展名
     *  @return 文件路径
     * */
    //得到文件所属目录路径
    private String getFileFolderPath(String fileMd5) {
        return uploadPath + fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/";
    }

    //得到文件的路径
    private String getFilePath(String fileMd5, String fileExt) {
        return uploadPath + fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + "." + fileExt;
    }

    //得到分块文件路径
    private String getChunkFileFolderPath(String fileMd5) {
        return uploadPath + fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + "chunk" + "/";
    }

    public ResponseResult register(String fileMd5, String fileName, Long fileSize, String minetype, String fileExt) {
        //检查文件在磁盘上是否存在
        String fileFolderPath = this.getFileFolderPath(fileMd5);
        //文件的路径
        String filePath = this.getFilePath(fileMd5, fileExt);
        File file = new File(filePath);
        //文件是否存在
        boolean exists = file.exists();
        //2.检查文件信息在mongoDB中是否存在,  他路径名时和id名是一样的
        Optional<MediaFile> optional = mediaFileRepository.findById(fileMd5);
        if (exists && optional.isPresent()) {
            //文件存在
            ExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_EXIST);
        }
        //文件不存在时做一些准备工作，检查文件所在的目录是否存在，如果不存在就创建他
        File fileFolder = new File(fileFolderPath);
        if (!fileFolder.exists()) {
            fileFolder.mkdirs();
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //校验分块文件是否存在
    public CheckChunkResult checkchunk(String fileMd5, Integer chunk, Integer chunkSize) {
        //得到块文件所在的目录
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        //得到块文件名
        File chunkfile = new File(chunkFileFolderPath + chunk);
        if (chunkfile.exists()) {
            //块文件存在
            return new CheckChunkResult(CommonCode.SUCCESS, true);
        } else {
            //块文件不存在
            return new CheckChunkResult(CommonCode.SUCCESS, false);
        }
    }

    //上传分块
    public ResponseResult uploadchunk(MultipartFile file, String fileMd5, Integer chunk) {
        //得到分块的路径
        String chunkFileFolderPath = this.getChunkFileFolderPath(fileMd5);
        //得到每个分块的路径（加上下标）
        String chunkFilePath = chunkFileFolderPath + chunk;
        //得到目录
        File chunkFileFolder = new File(chunkFileFolderPath);
        //没有的话自动创建
        if (!chunkFileFolder.exists()) {
            chunkFileFolder.mkdirs();
        }
        //得到上传文件的输入流
        InputStream inputStream = null;
        //输出流
        FileOutputStream fileOutputStream = null;

        try {
            inputStream = file.getInputStream();
            fileOutputStream = new FileOutputStream(new File(chunkFilePath));
            //输入输出流的转换
            IOUtils.copy(inputStream, fileOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //合并文件
    public ResponseResult mergechunks(String fileMd5, String fileName, Long fileSize, String minetype, String fileExt) {
        //1.合并所有分块
        //得到分块文件的目录
        String chunkFileFolderPath = this.getChunkFileFolderPath(fileMd5);
        File chunkFileFolder = new File(chunkFileFolderPath);
        //分块文件列表
        File[] files = chunkFileFolder.listFiles();
        List<File> fileList = Arrays.asList(files);

        //创建一个合并文件
        String filePath = this.getFilePath(fileMd5, fileExt);
        File mergeFile = new File(filePath);

        //执行合并
        mergeFile = this.getMergeFile(fileList, mergeFile);
        long length = mergeFile.length();

        if (mergeFile==null){
            return new ResponseResult(MediaCode.MERGE_FILE_FAIL);
        }
        //2.校验合并后的文件md5值 是否与前端传入的md5一致
        boolean checkFileMd5 = this.checkFileMd5(mergeFile, fileMd5);
        if(!checkFileMd5){
            //校验失败
            ExceptionCast.cast(MediaCode.MERGE_FILE_CHECKFAIL);
        }
        //3.将文件的信息写入mongodb
        MediaFile mediaFile = new MediaFile();
        mediaFile.setFileId(fileMd5);
        mediaFile.setFileOriginalName(fileName);
        mediaFile.setFileName(fileMd5 + "." +fileExt);
        //文件路径保存相对路径
        String filePath1 = fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" ;
        mediaFile.setFilePath(filePath1);
        mediaFile.setFileSize(fileSize);
        mediaFile.setUploadTime(new Date());
        mediaFile.setMimeType(minetype);
        mediaFile.setFileType(fileExt);
        //状态为上传成功
        mediaFile.setFileStatus("301002");
        mediaFileRepository.save(mediaFile);

        String mediaId=mediaFile.getFileId();
        sendProcessVideoMsg(mediaId);

        return new ResponseResult(CommonCode.SUCCESS);

    }

    //想MQ发送视频处理消息
    public ResponseResult sendProcessVideoMsg(String mediaId){
        Optional<MediaFile> optional = mediaFileRepository.findById(mediaId);
        if (!optional.isPresent()){
            return new ResponseResult(CommonCode.FAIL);
        }
        //发送视频处理消息
        HashMap<String, String> msgMap = new HashMap<>();
        msgMap.put("mediaId",mediaId);
        //发送的消息
        String jsonString = JSON.toJSONString(msgMap);

        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EX_MEDIA_PROCESSTASK,routing_media_video,jsonString);
        } catch (AmqpException e) {
            e.printStackTrace();
            return  new ResponseResult(CommonCode.FAIL);
        }
        return new ResponseResult(CommonCode.SUCCESS);

    }

    //合并文件
    private File getMergeFile(List<File> fileList, File mergeFile) {
        try {
            //如果文件存在的话就直接删除
            if (mergeFile.exists()) {
                mergeFile.delete();
            }
                //没有的话，就直接创建文件
                mergeFile.createNewFile();

            //对块文件进行排序  升序排序
            Collections.sort(fileList, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    if (Integer.parseInt(o1.getName())>Integer.parseInt(o2.getName())){
                        return 1;
                    }
                    return -1;
                }
            });
            //创建一个写对象
            RandomAccessFile raf_write = new RandomAccessFile(mergeFile, "rw");
            byte[] bytes=new byte[1024];
            for (File file : fileList) {
                //把分模块读出来
                RandomAccessFile raf_read = new RandomAccessFile(file, "r");
                int len=-1;
                while ((len=raf_read.read(bytes))!=-1){
                    raf_write.write(bytes,0,len);
                }
                raf_read.close();
            }
            raf_write.close();
            return mergeFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }
    //校验文件
    private boolean checkFileMd5(File mergeFile,String md5){

        try {
            //创建文件输入流
            FileInputStream fileInputStream = new FileInputStream(mergeFile);
            //得到文件的md5
            String md5Hex = DigestUtils.md5Hex(fileInputStream);
            if (md5.equalsIgnoreCase(md5Hex)){
                return true;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
