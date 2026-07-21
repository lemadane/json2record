package io.lemonade.json2record.naming;

import io.lemonade.json2record.exceptions.NameEncodingException;

import java.util.Objects;

/**
 * Reversible codec for mapping external XML element, attribute, and JSON property names
 * to and from valid Java record and component identifiers using the '$' escape convention.
 */
public final class DataNameCodec {

    private DataNameCodec() {
    }

    /**
     * Encodes an external XML/JSON name into a valid Java identifier.
     *
     * @param externalName the external name to encode.
     * @return the encoded Java identifier.
     */
    public static String encode(String externalName) {
        Objects.requireNonNull(externalName, "externalName must not be null");
        if (externalName.isEmpty()) {
            return externalName;
        }

        StringBuilder sb = new StringBuilder();
        int length = externalName.length();
        int codePointIndex = 0;

        for (int i = 0; i < length; i += Character.charCount(externalName.codePointAt(i))) {
            int cp = externalName.codePointAt(i);

            if (cp == ':') {
                sb.append('$');
            } else if (cp == '-') {
                sb.append("$hyphen$");
            } else if (cp == '.') {
                sb.append("$dot$");
            } else if (cp == ' ') {
                sb.append("$space$");
            } else if (cp == '$') {
                sb.append("$dollar$");
            } else if ((codePointIndex == 0 && Character.isJavaIdentifierStart(cp))
                    || (codePointIndex > 0 && Character.isJavaIdentifierPart(cp))) {
                sb.appendCodePoint(cp);
            } else {
                String hex = Integer.toHexString(cp).toUpperCase();
                if (hex.length() < 4) {
                    hex = "0".repeat(4 - hex.length()) + hex;
                }
                sb.append("$u").append(hex).append('$');
            }
            codePointIndex++;
        }

        return sb.toString();
    }

    /**
     * Decodes a Java identifier back into its original external XML/JSON name.
     *
     * @param javaIdentifier the Java identifier to decode.
     * @return the decoded external name.
     */
    public static String decode(String javaIdentifier) {
        Objects.requireNonNull(javaIdentifier, "javaIdentifier must not be null");
        if (javaIdentifier.isEmpty()) {
            return javaIdentifier;
        }

        StringBuilder sb = new StringBuilder();
        int len = javaIdentifier.length();
        int i = 0;

        while (i < len) {
            char c = javaIdentifier.charAt(i);
            if (c != '$') {
                sb.append(c);
                i++;
                continue;
            }

            // Starts with '$'
            if (javaIdentifier.startsWith("$hyphen$", i)) {
                sb.append('-');
                i += 8;
            } else if (javaIdentifier.startsWith("$dot$", i)) {
                sb.append('.');
                i += 5;
            } else if (javaIdentifier.startsWith("$space$", i)) {
                sb.append(' ');
                i += 7;
            } else if (javaIdentifier.startsWith("$dollar$", i)) {
                sb.append('$');
                i += 8;
            } else if (i + 2 < len && javaIdentifier.charAt(i + 1) == 'u') {
                int endDollar = javaIdentifier.indexOf('$', i + 2);
                if (endDollar != -1) {
                    String hex = javaIdentifier.substring(i + 2, endDollar);
                    try {
                        int cp = Integer.parseInt(hex, 16);
                        sb.appendCodePoint(cp);
                        i = endDollar + 1;
                    } catch (NumberFormatException e) {
                        throw new NameEncodingException("Invalid Unicode escape sequence: " + javaIdentifier.substring(i, endDollar + 1), e);
                    }
                } else {
                    throw new NameEncodingException("Unterminated Unicode escape sequence in identifier: " + javaIdentifier);
                }
            } else {
                // Single '$' represents ':'
                sb.append(':');
                i++;
            }
        }

        return sb.toString();
    }
}
