package com.xuecheng.manage_media_process;

import com.xuecheng.framework.utils.Mp4VideoUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * @author Administrator
 * @version 1.0
 * @create 2018-07-12 9:11
 **/
@SpringBootTest
@RunWith(SpringRunner.class)
public class TestProcessBuilder {

    @Test
    public void testProcessBuilder() throws IOException {
        //创建processBuilder命令
        ProcessBuilder processBuilder = new ProcessBuilder();
        //设置第三方程序命令
        //processBuilder.command("ping", "127.0.0.1");
        processBuilder.command("ipconfig");
        //将标准输入流和错误输入流合并，通过标准输入读取信息
        processBuilder.redirectErrorStream(true);
        //启动一个进程
        Process start = processBuilder.start();
        //通过标准输入流来拿到正常和错误信息
        InputStream inputStream = start.getInputStream();
        //转成字符流（带中文支持）
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream,"gbk");
        //缓冲
        int len=-1;
        char[] chars=new char[1024];
        //遍历打印出来
        while ((len=inputStreamReader.read(chars))!=-1){
            String s = new String(chars, 0, len);
            System.out.println(s);
        }
        inputStreamReader.close();
        inputStream.close();
    }
    @Test
    public void testFFmpeg() throws IOException {
        //创建processBuilder命令
        ProcessBuilder processBuilder = new ProcessBuilder();
        //定义命令内容
        ArrayList<String> command = new ArrayList<>();
        command.add("D:\\Develop\\ffmpeg\\ffmpeg-20180227-fa0c9d6-win64-static\\bin\\ffmpeg.exe");
        command.add("-i");
        command.add("D:\\test\\video\\lucene.avi");
        command.add("-y");//覆盖输出文件
        command.add("-c:v");
        command.add("libx264");
        command.add("-s");
        command.add("1280x720");
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add("-b:a");
        command.add("63k");
        command.add("-b:v");
        command.add("753k");
        command.add("-r");
        command.add("18");
        command.add("D:\\test\\video\\haixingba.mp4");
        processBuilder.command(command);
        //将标准输入流和错误输入流合并，通过标准输入读取信息
        processBuilder.redirectErrorStream(true);
        //启动一个进程
        Process start = processBuilder.start();
        //通过标准输入流来拿到正常和错误信息
        InputStream inputStream = start.getInputStream();
        //转成字符流（带中文支持）
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream,"gbk");
        //缓冲
        int len=-1;
        char[] chars=new char[1024];
        //遍历打印出来
        while ((len=inputStreamReader.read(chars))!=-1){
            String s = new String(chars, 0, len);
            System.out.println(s);
        }
        inputStreamReader.close();
        inputStream.close();
    }

    @Test
    public void test00() {
        //ffmpeg的安装路径
        String ffmpeg_path="D:\\Develop\\ffmpeg\\ffmpeg-20180227-fa0c9d6-win64-static\\bin\\ffmpeg.exe";
        //源avi视频的路径
        String video_path = "D:\\test\\video\\lucene.avi";
        //转换成mp4文件的名称
        String mp4_name = "233.mp4";
        //转换后的mp4的路径
        String mp4_path="D:\\test\\video\\";
        //创建工具类对象
        Mp4VideoUtil mp4VideoUtil = new Mp4VideoUtil(ffmpeg_path, video_path, mp4_name, mp4_path);
        //开始视频转换，成功了返回success;
        String s = mp4VideoUtil.generateMp4();
        System.out.println(s);
    }


}
