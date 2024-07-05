package top.weixiansen574.bilibiliArchive.util;

import com.alibaba.fastjson.JSON;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class FileUtil {
    public static void outputToFile(InputStream inputStream, File file) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, read);
        }
        fileOutputStream.flush();
        fileOutputStream.close();
        inputStream.close();
    }

    public static void copyFile(File source, File target) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(target)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }
    }

    public static void outputToFile(InputStream inputStream, String path) throws IOException {
        outputToFile(inputStream, new File(path));
    }

    public static boolean outputToFile(byte[] bytes, File path) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(path);
            fileOutputStream.write(bytes);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void deleteDirs(File file) {
        if (file.isFile()) {
            file.delete();
        } else {
            File[] childFilePath = file.listFiles();
            if (childFilePath != null) {
                for (File child : childFilePath) {
                    deleteDirs(child);
                }
                file.delete();
            }
        }
    }

    public static byte[] readAll(File file) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file));
        return dataInputStream.readAllBytes();
    }

    public static String readAllToString(File file) throws IOException {
        return new String(readAll(file), StandardCharsets.UTF_8);
    }

    public static <T> T deserializeJSONFromFile(File file, Class<T> type) {
        try {
            FileInputStream fis = new FileInputStream(file);
            String jsonText = new String(fis.readAllBytes(), StandardCharsets.UTF_8);
            fis.close();
            return JSON.parseObject(jsonText, type);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static File getOrCreateDir(File parent, String child) throws IOException {
        File file = new File(parent, child);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new IOException("mkdir " + file.getAbsolutePath() + " failed");
            }
        }
        return file;
    }

    public static void deleteOneFile(File file) throws IOException {
        if (file.exists()) {
            if (!file.delete()) {
                throw new IOException("delete " + file.getAbsolutePath() + " failed");
            }
        }
    }

}
