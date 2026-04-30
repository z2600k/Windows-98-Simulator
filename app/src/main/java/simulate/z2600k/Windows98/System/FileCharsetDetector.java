package simulate.z2600k.Windows98.System;

import java.io.*;
import java.util.Arrays;

public class FileCharsetDetector {

    // 检测时读取的字节数（增大到 4KB，足以覆盖绝大多数文本特征）
    private static final int DETECT_BUFFER_SIZE = 4096;

    /**
     * 获取文件最可能的字符集名称。
     */
    public static String getFileCharsetName(File file) throws IOException {
        try (InputStream inputStream = new FileInputStream(file)) {
            byte[] head = new byte[4];  // 最多需要 4 字节判断 BOM
            int readLen = inputStream.read(head);

            // 1. BOM 检测（优先级最高）
            if (readLen >= 2) {
                if (head[0] == (byte) 0xFF && head[1] == (byte) 0xFE) {
                    return "UTF-16LE";
                }
                if (head[0] == (byte) 0xFE && head[1] == (byte) 0xFF) {
                    return "UTF-16BE";
                }
            }
            if (readLen >= 3) {
                if (head[0] == (byte) 0xEF && head[1] == (byte) 0xBB && head[2] == (byte) 0xBF) {
                    return "UTF-8";
                }
            }
            if (readLen >= 4) {
                // UTF-32 检测（可选，但为了完整性保留）
                if (head[0] == 0x00 && head[1] == 0x00 && head[2] == (byte) 0xFE && head[3] == (byte) 0xFF)
                    return "UTF-32BE";
            }

            // 2. 无 BOM，读取完整缓冲区进行启发式检测
            byte[] buffer = readFileBytes(file);
            return detectEncodingHeuristic(buffer);
        }
    }

    /**
     * 读取文件前 maxLen 个字节（实际读取长度可能小于 maxLen）。
     */
    private static byte[] readFileBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[FileCharsetDetector.DETECT_BUFFER_SIZE];
            int actual = fis.read(buffer);
            if (actual <= 0) {
                return new byte[0];
            }
            return Arrays.copyOf(buffer, actual);  // 截取有效部分
        }
    }

    /**
     * 基于字节内容的启发式编码检测（UTF-8 / GB18030 / GBK / 其他）。
     */
    private static String detectEncodingHeuristic(byte[] data) {
        if (data.length == 0) return "UTF-8"; // 空文件默认 UTF-8

        // 2.1 UTF-8 严格合法性校验（涵盖 1~4 字节序列）
        if (isValidUTF8(data)) {
            return "UTF-8";
        }

        // 2.2 尝试按 GB18030 / GBK 解码并检查是否能无损还原（更准确）
        if (isValidGBEncoding(data, "GB18030")) {
            return "GB18030";
        }
        if (isValidGBEncoding(data, "GBK")) {
            return "GBK";
        }

        // 2.3 如果 ASCII 占比极高，可能是 ISO-8859-1 或 Windows-1252
        if (isMostlyAscii(data)) {
            return "windows-1252";   // 或 "ISO-8859-1"
        }

        // 2.4 最终回退
        return "UTF-8";
    }

    /**
     * 校验字节数组是否符合 UTF-8 规范（修复原版 Bug，使用实际长度）。
     */
    private static boolean isValidUTF8(byte[] bytes) {
        int i = 0;
        while (i < bytes.length) {
            byte b = bytes[i];
            if ((b & 0x80) == 0) {          // 1 字节 (ASCII)
                i++;
            } else if ((b & 0xE0) == 0xC0) { // 2 字节
                if (i + 1 >= bytes.length || (bytes[i + 1] & 0xC0) != 0x80) return false;
                i += 2;
            } else if ((b & 0xF0) == 0xE0) { // 3 字节
                if (i + 2 >= bytes.length || (bytes[i + 1] & 0xC0) != 0x80 || (bytes[i + 2] & 0xC0) != 0x80)
                    return false;
                // 排除无效的代理对范围 (U+D800~U+DFFF)
                int codePoint = ((b & 0x0F) << 12) | ((bytes[i + 1] & 0x3F) << 6) | (bytes[i + 2] & 0x3F);
                if (codePoint >= 0xD800 && codePoint <= 0xDFFF) return false;
                i += 3;
            } else if ((b & 0xF8) == 0xF0) { // 4 字节
                if (i + 3 >= bytes.length ||
                    (bytes[i + 1] & 0xC0) != 0x80 ||
                    (bytes[i + 2] & 0xC0) != 0x80 ||
                    (bytes[i + 3] & 0xC0) != 0x80)
                    return false;
                i += 4;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * 验证字节数组能否用指定 GB 系列编码正常解码并还原为相同字节（无损编码）。
     */
    private static boolean isValidGBEncoding(byte[] bytes, String charsetName) {
        try {
            String decoded = new String(bytes, charsetName);
            byte[] reEncoded = decoded.getBytes(charsetName);
            return Arrays.equals(bytes, reEncoded);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断是否为高比例 ASCII 文本（> 95%），常用于识别西欧编码。
     */
    private static boolean isMostlyAscii(byte[] bytes) {
        int asciiCount = 0;
        for (byte b : bytes) {
            if (b >= 0) asciiCount++;  // ASCII 字节值 0~127
        }
        return (asciiCount * 100 / bytes.length) > 95;
    }
}