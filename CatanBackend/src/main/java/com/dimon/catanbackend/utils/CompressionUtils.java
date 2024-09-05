package com.dimon.catanbackend.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility class providing methods for compressing and decompressing string data using GZIP and Base64 encoding.
 *
 * The class is designed to convert a string into a compressed format, suitable for storage or transmission,
 * and to decompress it back into its original form. It uses the GZIP algorithm for compression and
 * Base64 encoding for safe transfer of binary data as a string.
 *
 * Methods:
 * - {@code compress}: Compresses a string using GZIP and encodes the result in Base64.
 * - {@code decompress}: Decodes a Base64-encoded string and decompresses it using GZIP.
 *
 * Example usage:
 * <pre>
 * {@code
 * String compressed = CompressionUtils.compress("Sample data to compress");
 * String decompressed = CompressionUtils.decompress(compressed);
 * }
 * </pre>
 *
 * Exception handling:
 * - Both methods throw {@link IOException} if an error occurs during compression or decompression.
 *
 */
public class CompressionUtils {

    /**
     * Compresses the given string data using GZIP and encodes the compressed data in Base64.
     *
     * @param data the string to be compressed
     * @return the compressed data as a Base64-encoded string
     * @throws IOException if an I/O error occurs during compression
     */
    public static String compress(String data) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try(GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(data.getBytes(StandardCharsets.UTF_8));
        }

        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
    }

    /**
     * Decompresses the given Base64-encoded and GZIP-compressed string back to its original form.
     *
     * @param compressedData the Base64-encoded string to be decompressed
     * @return the decompressed original string
     * @throws IOException if an I/O error occurs during decompression
     */
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
