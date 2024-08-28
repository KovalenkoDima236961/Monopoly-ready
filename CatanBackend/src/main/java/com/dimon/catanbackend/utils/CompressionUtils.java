package com.dimon.catanbackend.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CompressionUtils {

    public static String compress(String data) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try(GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(data.getBytes(StandardCharsets.UTF_8));
        }

        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
    }

    public static String decompress(String compressedData) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(compressedData);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipInputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, len);
            }
            return byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
        }
    }
}
