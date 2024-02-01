package com.atlassian.clover.instr.java;

import clover.antlr.CommonHiddenStreamToken;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openclover.util.Lists.newLinkedList;
import static org.openclover.util.Maps.newHashMap;
import static org.openclover.util.Sets.newHashSet;

public class TokenListUtil {
    /**
     * Pattern to split a string into multiple lines based on embedded NL/CR characters
     */
    private static final Pattern ML_PATTERN =
            Pattern.compile("\r\n|\r|\n");

    /**
     * Will match any line of javadoc (in a single-line or multi-line javadoc comment) and will return group 1 as the
     * text of the line excluding any beginning or trailing javadoc line noise (e.g. "/** ", " * ", " * /")
     */
    private static final Pattern MATCH_JAVADOC_LINE =
        Pattern.compile(
            "^" + //the beginning
            "\\s*" + //optional starting white space
            "(?:/\\*\\*)?" + //optional /**
            "(?:\\*)*(?!/)" + //some option * but only up until (but excluding) one that preceeds a / ie */
            "(.*?)" + //the meat of the line
            "(?:\\**/)?" + //optional */
            "$"); //the end

    /**
     * compiled regexp to match first part of multilineJavadoc tags *
     */
    private static final Pattern MATCH_JAVADOC_TAG_LINE_START =
        Pattern.compile(
            "^" + //the beginning
            "\\s*" + //optional starging white space
            "@(\\S+)" + //@tag with capture group 1 holding the tag name
            "(:?\\s+(.*))?" + //optional trailing text with capature group 2 holding the text
            "$"); //the end

    private static final Set<String> IGNORED_TAGS = Collections.unmodifiableSet(newHashSet(
        "deprecated",
        "param",
        "throws",
        "see",
        "serialField",
        "serialData",
        "author",
        "since",
        "version",
        "exception"
    ));

    /**
     * @param begin first token to consider
     * @param end   last token to consider
     * @return a normalised string representation of the token sequence. Whitespace sequences are normalised to a single
     * space. Comments are not included in the string. Hidden tokens before the first token and after the last token are
     * not included.
     */
    public static String getNormalisedSequence(CloverToken begin, CloverToken end) {
        return getNormalisedSequence(begin, end, true);
    }

    /**
     * @param begin      first token to consider
     * @param end        last token to consider
     * @param whitespace whether or not to include (normalised) whitespace.
     * @return a normalised string representation of the token sequence. if Whitespace is included, whitespace sequences
     * are normalised to a single space. Comments are not included in the string. Hidden tokens before the first token
     * and after the last token are not included.
     */
    public static String getNormalisedSequence(CloverToken begin, CloverToken end, boolean whitespace) {
        StringBuilder buf = new StringBuilder();
        CloverToken curr = begin;
        CloverToken prev = null;
        while (curr != null) {
            if (prev != null && hasWhitespaceAfter(prev) && whitespace) {
                buf.append(" ");
            }
            buf.append(curr.getText());
            if (curr == end) {
                break;
            }
            prev = curr;
            curr = curr.getNext();
        }
        return buf.toString();
    }


    /**
     * @param token to scan
     * @return true if the given token has whitespace after it, false otherwise
     */
    public static boolean hasWhitespaceAfter(CloverToken token) {
        CommonHiddenStreamToken curr = token.getHiddenAfter();
        while (curr != null) {
            if (curr.getType() == JavaTokenTypes.WS) {
                return true;
            }
            curr = curr.getHiddenAfter();
        }
        return false;
    }

    public static Map<String, List<String>> getJDocTagsAndValuesOnBlock(CloverToken startOfField) {
        CloverToken prev = startOfField.getPrev();
        Map<String, List<String>> tags = newHashMap();
        CommonHiddenStreamToken hidden;
        if (prev != null) {
            hidden = prev.getHiddenAfter();
        } else {
            // special case where this field is the first unhidden token in the list.
            hidden = startOfField.getFilter().getInitialHiddenToken();
        }
        while (hidden != null) {
            if (JavaTokenTypes.ML_COMMENT == hidden.getType() && hidden.getText().startsWith("/**")) {
                getJDocTagsOnComment(tags, hidden.getText());
            }
            hidden = hidden.getHiddenAfter();
        }
        return tags;
    }

    public static void getJDocTagsOnComment(Map<String, List<String>> tags, String comment) {
        String[] lines = ML_PATTERN.split(comment);

        boolean inTag = false;
        String tagName = null;
        String tagContents = "";

        for (String line : lines) {
            Matcher lineMatcher = MATCH_JAVADOC_LINE.matcher(line);
            if (lineMatcher.matches()) {
                String lineContents = lineMatcher.group(1);
                Matcher tagMatcher = MATCH_JAVADOC_TAG_LINE_START.matcher(lineContents);

                if (tagMatcher.matches()) {
                    if (!inTag) {
                        inTag = true;
                    } else if (!ignore(tagName)) {
                        addTag(tags, tagName, tagContents.trim());
                    }
                    tagName = tagMatcher.group(1);
                    tagContents = emptyStringForNull(tagMatcher.group(2));
                } else {
                    if (inTag) {
                        tagContents += lineContents;
                    }
                }
            }
        }
        if (tagName != null && !ignore(tagName)) {
            addTag(tags, tagName, tagContents.trim());
        }
    }

    private static boolean ignore(String tagName) {
        return IGNORED_TAGS.contains(tagName);
    }

    private static String emptyStringForNull(String string) {
        return string == null ? "" : string;
    }

    private static void addTag(Map<String, List<String>> tags, String tagName, String tagContents) {
        List<String> tagValues = tags.computeIfAbsent(tagName, k -> newLinkedList());
        tagValues.add(tagContents);
    }
}
