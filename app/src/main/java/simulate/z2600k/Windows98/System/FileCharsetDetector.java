package simulate.z2600k.Windows98.System;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileCharsetDetector {
    public static String getFileCharsetName(File file) throws IOException {
        InputStream inputStream = new FileInputStream(file);
        byte[] head = new byte[3];
        inputStream.read(head);

        String charsetName = "";
        System.out.println(head[0]);
        System.out.println(head[1]);
        System.out.println(head[2]);
        if (head[0] == (byte)0xFF && head[1] == (byte)0xFE ) //0xFFFE
            charsetName = "UTF-16";//UTF-16LE
        else if (head[0] == (byte)0xFE && head[1] == (byte)0xFF ) //0xFEFF
            charsetName = "Unicode";//UTF-16BE
        else if(head[0] == (byte)0xEF && head[1] == (byte)0xBB && head[2] == (byte)0xBF)
            charsetName = "UTF-8"; //UTF-8 BOM
        else charsetName =detectEncoding(readFileToBytes(file)); ;//UTF-8（不带BOM）或GBK
        inputStream.close();

        System.out.println(charsetName);
        return charsetName;
    }

    public static boolean isUTF8(byte[] bytes) {
        int i = 0;
        while (i < bytes.length) {
            byte b = bytes[i];
            if ((b & 0x80) == 0) { // 单字节字符 (0xxxxxxx)
                i++;
            } else if ((b & 0xE0) == 0xC0) { // 2字节字符 (110xxxxx)
                if (i + 1 >= bytes.length || (bytes[i + 1] & 0xC0) != 0x80) {
                    return false;
                }
                i += 2;
            } else if ((b & 0xF0) == 0xE0) { // 3字节字符 (1110xxxx)
                if (i + 2 >= bytes.length || (bytes[i + 1] & 0xC0) != 0x80 || (bytes[i + 2] & 0xC0) != 0x80) {
                    return false;
                }
                i += 3;
            } else if ((b & 0xF8) == 0xF0) { // 4字节字符 (11110xxx)
                if (i + 3 >= bytes.length || (bytes[i + 1] & 0xC0) != 0x80 || (bytes[i + 2] & 0xC0) != 0x80 || (bytes[i + 3] & 0xC0) != 0x80) {
                    return false;
                }
                i += 4;
            } else {
                return false; // 不符合 UTF-8 规则
            }
        }
        return true;
    }

    public static boolean isGBK(byte[] bytes) {
        try {
            String str = new String(bytes, "GBK");
            byte[] reencoded = str.getBytes("GBK");
            return java.util.Arrays.equals(bytes, reencoded);
        } catch (Exception e) {
            return false;
        }
    }

    public static String detectEncoding(byte[] bytes) {
        if (isUTF8(bytes)) {
            return "UTF-8";
        } else if (isGBK(bytes)) {
            return "GBK";
        } else {
            return "Unknown";
        }
    }

    public static byte[] readFileToBytes(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] bytes = new byte[10];
        fis.read(bytes);
        fis.close();
        return bytes;
    }
}
