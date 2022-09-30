package ltd.icecold.course.utils;

import ltd.icecold.course.Main;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public class IOUtils {
    public static InputStream getResource(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        }
        try {
            URL url = Main.class.getClassLoader().getResource(filename);

            if (url == null) {
                return null;
            }
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }

    public static String readFile(File f) {
        if (!f.exists()) {
            throw new NullPointerException("Could not find file \"" + f.getName() + "\"");
        }
        try {
            BufferedInputStream biss = new BufferedInputStream(new FileInputStream(f));
            byte[] b = new byte[biss.available()];
            biss.read(b);
            biss.close();
            return new String(b, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static byte[] readFileByte(File f) {
        if (!f.exists()) {
            throw new NullPointerException("Could not find file \"" + f.getName() + "\"");
        }
        try {
            BufferedInputStream biss = new BufferedInputStream(new FileInputStream(f));
            byte[] b = new byte[biss.available()];
            biss.read(b);
            biss.close();
            return b;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static void writeFile(File f, String data) {
        writeFile(f, data.getBytes(StandardCharsets.UTF_8));
    }

    public static void writeFile(File f, byte[] data) {
        try {
            if (!f.exists()) {
                f.createNewFile();
            }
            BufferedOutputStream boss = new BufferedOutputStream(new FileOutputStream(f));
            boss.write(data);
            boss.flush();
            boss.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte[] byteMerger(byte[] bt1, byte[] bt2) {
        byte[] bt3 = new byte[bt1.length + bt2.length];
        System.arraycopy(bt1, 0, bt3, 0, bt1.length);
        System.arraycopy(bt2, 0, bt3, bt1.length, bt2.length);
        return bt3;
    }
}
