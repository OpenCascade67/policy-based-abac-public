package com.pap.web;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Builds {@code Content-Disposition: attachment} values carrying both {@code filename} and the RFC 5987
 * {@code filename*} parameter, the same form ASP.NET Core's file results produce.
 */
public final class ContentDispositions {

    private static final String TOKEN_SEPARATORS = "()<>@,;:\\\"/[]?={} \t";
    private static final String ATTR_CHAR_EXTRAS = "!#$&+-.^_`|~";

    private ContentDispositions() {
    }

    public static String attachment(String fileName) {
        return "attachment; filename=" + filenameParameter(fileName) + "; filename*=UTF-8''" + encodeRfc5987(fileName);
    }

    private static String filenameParameter(String fileName) {
        boolean ascii = fileName.chars().allMatch(c -> c < 0x80);
        if (!ascii) {
            String encoded = Base64.getEncoder().encodeToString(fileName.getBytes(StandardCharsets.UTF_8));
            return "\"=?utf-8?B?" + encoded + "?=\"";
        }
        boolean token = !fileName.isEmpty() && fileName.chars().allMatch(c -> c > 0x20 && c < 0x7F && TOKEN_SEPARATORS.indexOf(c) < 0);
        return token ? fileName : "\"" + fileName.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String encodeRfc5987(String value) {
        StringBuilder sb = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xFF;
            if (Character.isLetterOrDigit(c) && c < 0x80 || ATTR_CHAR_EXTRAS.indexOf(c) >= 0) {
                sb.append((char) c);
            } else {
                sb.append('%').append(String.format("%02X", c));
            }
        }
        return sb.toString();
    }
}
