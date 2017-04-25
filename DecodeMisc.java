package org.linuxsogood.misc;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.IOException;

/**
 * Created by honway on 2017/4/25 16:05.
 *
 */
public class DecodeMisc {


    public static void main(String[] args) throws IOException {
        //encode();
        decode();

    }

    private static void encode() {
        String str = "那年";
        BASE64Encoder encoder = new BASE64Encoder();
        String encode = encoder.encode(str.getBytes());
        char[] chars = encode.toCharArray();
        for (char aChar : chars) {
            String s = Integer.toOctalString(aChar);
            System.out.print((s.length() == 3 ? s : "0"+s) + " ");
        }
    }

    private static void decode() throws IOException {
        //String line1 = "126 062 126 163 142 103 102 153 142 062 065 154 111 121 157 113 122 155 170 150 132 172 157 147 123 126 144 067 124 152 102 146 115 107 065 154 130 062 116 150 142 154 071 172 144 104 102 167 130 063 153 167 144 130 060 113 012";
        String line1 = "065 131 062 102 065 142 155 060 145 063 116 172 117 152 116 071";
        String[] split = line1.split(" ");
        StringBuilder result = new StringBuilder();
        for (String string : split) {
            result.append((char) Integer.valueOf(string, 8).intValue());
        }
        BASE64Decoder decoder = new BASE64Decoder();
        byte[] bytes = decoder.decodeBuffer(result.toString());
        String s = new String(bytes, "UTF-8");
        System.out.println(s);
    }
}
