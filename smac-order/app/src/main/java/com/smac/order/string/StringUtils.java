package com.smac.order.string;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * Created by Huynh Quang Thao on 10/19/15.
 */
public class StringUtils {
    public static String normalizeString(String s) {
        s = s.toLowerCase().trim().replaceAll("\\s+", " ");
        // s = s.replaceAll("\\.", "");
        s = AccentRemoval.removeAccent(s);
        return s;
    }

    public static String normalizeFileCache(String s) {
        s = s.toLowerCase().trim().replaceAll("\\s+", " ");
        // s = s.replaceAll("\\.", "");
        s = AccentRemoval.removeAccent(s);
        s = s.replaceAll(" ", "_");
        s = s.replaceAll("[^a-z0-9_-]", "");
        return s;
    }

    public static byte[] convertToByteArray(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String convertToString(byte[] encode) {
        try {
            return new String(encode, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] convertInputStreamToByteArray(InputStream inputStream) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        try {
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer.toByteArray();
    }

}
