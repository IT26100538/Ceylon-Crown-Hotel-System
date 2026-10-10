package com.ceyloncrown.util;

/** Encodes/decodes one record as a pipe-delimited line. Escapes so user text can never break the file format. */
public final class RecordCodec {
    private RecordCodec() {}

    public static String join(Object... parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append('|');
            sb.append(escape(String.valueOf(parts[i])));
        }
        return sb.toString();
    }

    public static String[] split(String line) {
        String[] raw = line.split("\\|", -1);
        for (int i = 0; i < raw.length; i++) raw[i] = unescape(raw[i]);
        return raw;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n").replace("\r", "");
    }

    private static String unescape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                sb.append(n == 'p' ? '|' : n == 'n' ? '\n' : n);
            } else sb.append(c);
        }
        return sb.toString();
    }
}