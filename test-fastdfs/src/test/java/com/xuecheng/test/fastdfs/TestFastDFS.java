package com.xuecheng.test.fastdfs;

import org.csource.common.MyException;
import org.csource.fastdfs.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@SpringBootTest
@RunWith(SpringRunner.class)
public class TestFastDFS {

    @Test
    public void testUpload(){
        try {
            //加载配置文件
            ClientGlobal.initByProperties("config/fastdfs-client.properties");
            //定义TrackerClient,请求TrackerServer
            TrackerClient trackerClient = new TrackerClient();
            //连接tracker
            TrackerServer connection = trackerClient.getConnection();
            //获取storeStorage
            StorageServer storeStorage = trackerClient.getStoreStorage(connection);
            //创建storageClient1
            StorageClient1 storageClient1 = new StorageClient1(connection, storeStorage);
            //向stroage服务器上传文件
            //本地路径
            String path="C:\\Users\\HH\\Desktop\\IMG_2900.JPG";
            //上传成功后拿到文件id
            String s = storageClient1.upload_file1(path, "JPG", null);
            //  s =  group1/M00/00/00/wKhlhV3wilyAUY_uAATESuT8KsU805.JPG
            System.out.println(s);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }
    }
    @Test
    //文件查询
    public void testQueryFile(){
        try {
            ClientGlobal.initByProperties("config/fastdfs-client.properties");
            TrackerClient trackerClient=new TrackerClient();
            TrackerServer trackerServer = trackerClient.getConnection();
            StorageServer storeStorage = null;
            StorageClient storageClient =new StorageClient(trackerServer,storeStorage);
            FileInfo fileInfo = storageClient.query_file_info("group1", "M00/00/00/wKhlhV3wilyAUY_uAATESuT8KsU805.JPG");
            System.out.println(fileInfo);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }
    }
    @Test
    public void testDowloadFile(){
        try {
            ClientGlobal.initByProperties("config/fastdfs-client.properties");
            TrackerClient trackerClient = new TrackerClient();
            TrackerServer trackerServer = trackerClient.getConnection();
            StorageServer storeStorage = trackerClient.getStoreStorage(trackerServer);
            StorageClient1 storageClient1 = new StorageClient1(trackerServer, storeStorage);
            //下载文件
            //文件id
            String fileId= "group1/M00/00/00/wKhlhV3wilyAUY_uAATESuT8KsU805.JPG";
            byte[] bytes = storageClient1.download_file1(fileId);
            //使用输出流保存文件
            FileOutputStream fileOutputStream = new FileOutputStream(new File("D:\\手机备份\\1.jpg"));
            fileOutputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }
    }
}
