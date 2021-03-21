package com.ithcast;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class haixingba {
    //把文件分块
    @Test
    public void testChunk() throws IOException{
        //找到原始   文件
        File sourceFile = new File("D:\\test\\video\\lucene.mp4");
        // 生成块文件存储的目录
        String chunkPath = "D:\\test\\video\\chunk\\";


        //分块的大小
        long chunkSize=1*1024*1024;

        //分块的数量
        double chunkNum = Math.ceil(sourceFile.length() * 1.0 / chunkSize);

        //缓存区大小
        byte[] b=new byte[1024];
        //已只  读的方式 读取文件
        RandomAccessFile raf_read = new RandomAccessFile(sourceFile, "r");
        for (int i = 0; i < chunkNum; i++) {
            File file = new File(chunkPath + i);
            //向分块文件写数据
            RandomAccessFile raf_write = new RandomAccessFile(file, "rw");
            int len=-1;
            while ((len = raf_read.read(b))!=-1){
                raf_write.write(b,0,len);
                //如果块文件大小 大于 自己所设置的限制，则 进行下个文件的设置
                if (file.length()>=chunkSize){
                    break;
                }

            }
            raf_write.close();
        }
        raf_read.close();

    }

    //文件合并
    @Test
    public void testMerge() throws IOException{
        //块文件目录 对象
        File chunkFolder = new File("D:\\test\\video\\chunk\\");
        //块文件  列表
        File[] files = chunkFolder.listFiles();
        //将块文件排序，按名称排序
        List<File> fileList = Arrays.asList(files);
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            //大于 返回1 ，升序排列
            public int compare(File o1, File o2) {
                if (Integer.parseInt(o1.getName())>Integer.parseInt(o2.getName())){
                    return 1;
                }
                return -1;
            }
        });
        //文件合并
        File mergeFile = new File("D:\\test\\video\\lucene_11.avi");
        //创建新的文件
        mergeFile.createNewFile();

        //创建写对象
        RandomAccessFile raf_write = new RandomAccessFile(mergeFile, "rw");

        //缓冲区
        byte[] bytes=new byte[1024];
        for (File file : fileList) {
            //创建一个块进行读取
            RandomAccessFile  raf_read = new RandomAccessFile(file, "r");
            int len=-1;
            //每一个写到 总文件里
            while ((len= raf_read.read(bytes))!=-1){
                raf_write.write(bytes,0,len);
            }
            raf_read.close();
        }
        raf_write.close();
    }
}
