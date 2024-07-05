package top.weixiansen574.bilibiliArchive.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class FFmpegUtil {

    public static boolean merge(String audioFilePath, String videoFilePath, String outputFilePath) {
        String[] cmd = {
                "ffmpeg",
                "-i",
                audioFilePath,
                "-i",
                videoFilePath,
                "-acodec",
                "copy",
                "-vcodec",
                "copy",
                outputFilePath,
                "-y"
        };
        //String cmd = "ffmpeg -i \""+audioFilePath+"\" -i \""+videoFilePath+"\" -acodec copy -vcodec copy \""+outputFilePath+"\"";
        File outputFile = new File(outputFilePath);
        if (outputFile.exists()) {
            System.out.println("为保证正常ffmpeg能再次合并视频，已删除视频文件："+outputFilePath);
        }
        System.out.println(Arrays.toString(cmd));
        return executeCmd(cmd);
    }

    public static boolean executeCmd(String[] cmd) {
        try {
            //String cmdBin = "cmd /c ";
            //Process process = Runtime.getRuntime().exec(cmd);
            Process process = new ProcessBuilder(cmd).start();
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            //BufferedReader errorReader = process.errorReader();
            String l;
            while ((l =  errorReader.readLine()) != null){
             //   System.err.println(l);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
               // System.out.println(line);
            }

            boolean exitCode = process.waitFor(1, TimeUnit.MINUTES);
            if (exitCode) {
                System.out.println("视频合并成功");
                return true;
            } else {
                System.err.println("视频未正常合并，因为已超时");
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean checkFFmpegInstalled() {
        // 要执行的命令
        String command = "ffmpeg -version";

        try {
            // 执行系统命令
            Process process = Runtime.getRuntime().exec(command);

            // 读取命令输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // 等待命令执行完毕
            int exitCode = process.waitFor();

            // 输出命令执行结果
            //System.out.println("Command Output:\n" + output);

            // 检查退出代码
            return exitCode == 0;

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false; // 如果发生异常，认为FFmpeg未安装
        }
    }



}
