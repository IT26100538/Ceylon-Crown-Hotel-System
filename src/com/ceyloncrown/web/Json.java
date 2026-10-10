package com.ceyloncrown.web;

import java.util.Collection;
import java.util.Map;

/** Minimal JSON writer (no external libraries needed). */
public final class Json {
    private Json() {}
    public static String toJson(Object o) {
        if (o == null) return "null";
        if (o instanceof Number || o instanceof Boolean) return o.toString();
        if (o instanceof Map<?, ?> m) {
            StringBuilder sb = new StringBuilder("{"); boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) sb.append(','); first = false;
                sb.append(quote(String.valueOf(e.getKey()))).append(':').append(toJson(e.getValue()));
            }
            return sb.append('}').toString();
        }
        if (o instanceof Collection<?> c) {
            StringBuilder sb = new StringBuilder("["); boolean first = true;
            for (Object x : c) { if (!first) sb.append(','); first = false; sb.append(toJson(x)); }
            return sb.append(']').toString();
        }
        return quote(o.toString());
    }
    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\""); case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n"); case '\r' -> sb.append("\\r"); case '\t' -> sb.append("\\t");
                default -> { if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c); }
            }
        }
        return sb.append('"').toString();
    }
}