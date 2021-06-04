package org.hzero.starter.call.util;

import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;

/**
 * Created by wushuai on 2021/5/27
 */
public class Base64Utils {

    /**
     * Encode str.
     *
     * @param b the b
     * @return the string
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public static String encode(byte[] b) throws UnsupportedEncodingException {
        Base64 base64 = new Base64();
        b = base64.encode(b);
        String s = new String(b, "utf-8");
        return s;
    }

    /**
     * Decode str.
     *
     * @param encodeStr the encode str
     * @return the string
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public static byte[] decode(String encodeStr) throws UnsupportedEncodingException {
        byte[] b = encodeStr.getBytes();
        Base64 base64 = new Base64();
        b = base64.decode(b);
        return b;
    }
}
