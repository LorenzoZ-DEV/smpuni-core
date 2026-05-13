package dev.lorenzz.solum.utils;

public final class Text {

    private static final char COLOR_CHAR = '\u00A7';
    private static final String COLOR_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";

    private Text() {}

    public static String color(String input) {
        if (input == null || input.isEmpty()) return "";

        int firstAmp = input.indexOf('&');
        if (firstAmp < 0) return input;

        final int len = input.length();
        StringBuilder sb = new StringBuilder(len + 16);
        sb.append(input, 0, firstAmp);

        for (int i = firstAmp; i < len; i++) {
            char c = input.charAt(i);
            if (c != '&' || i + 1 >= len) {
                sb.append(c);
                continue;
            }

            char next = input.charAt(i + 1);

            if (next == '#' && i + 7 < len && isHex6(input, i + 2)) {
                sb.append(COLOR_CHAR).append('x');
                for (int j = 0; j < 6; j++) {
                    sb.append(COLOR_CHAR).append(input.charAt(i + 2 + j));
                }
                i += 7;
                continue;
            }

            if (COLOR_CODES.indexOf(next) >= 0) {
                sb.append(COLOR_CHAR).append(Character.toLowerCase(next));
                i++;
                continue;
            }

            sb.append(c);
        }
        return sb.toString();
    }

    private static boolean isHex6(String s, int start) {
        for (int i = 0; i < 6; i++) {
            char c = s.charAt(start + i);
            if (!((c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }
}

