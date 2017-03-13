package com.atlassian.clover.util.format;

/**

 */
public class StringFormatting {

    /**
     * a quick and dirty replaceAll
     * @param src   the source string
     * @param match  the string to look for
     * @param subs  the string to replace it with
     * @return
     */
    public static String replaceAll(String src, String match, String subs) {
        StringBuilder buf = new StringBuilder(src.length());
        int i = 0;
        while (i < src.length()) {
            if (src.substring(i).startsWith(match)) {
                buf.append(subs);
                i += match.length();
            }
            else {
                buf.append(src.charAt(i));
                i++;
            }
        }
        return buf.toString();
    }

}
