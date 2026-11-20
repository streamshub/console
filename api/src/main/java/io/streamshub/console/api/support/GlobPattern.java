package io.streamshub.console.api.support;

import java.util.Arrays;
import java.util.Objects;

/**
 * A simple wildcard matcher supporting {@code *} (zero or more characters) and
 * {@code ?} (exactly one character). All other characters are matched literally;
 * there is no escape syntax.
 *
 * <p>
 * The pattern is compiled into an array of code points, with consecutive
 * {@code *} collapsed into one and, when case-insensitive, the other code points
 * already case-folded. Matching is a single pass over the text with at most one
 * backtrack point, the position of the most recent {@code *}. When the text does
 * not match at the current position, the {@code *} is extended by one character
 * and matching resumes after it. This never takes exponential time, unlike a
 * regular expression translated from the same pattern, and allocates no memory.
 * A {@code *} that ends the pattern matches the rest of the text immediately.
 *
 * <p>
 * The worst case is {@code O(text length * pattern length)}, e.g. a pattern of a
 * {@code *} followed by a long literal that almost, but never quite, matches at
 * every position of the text. This class does not limit the length of the
 * pattern; callers handling untrusted input should validate the pattern length
 * before compiling.
 *
 * <p>
 * {@code ?} matches a Unicode code point, so it matches a supplementary
 * character (e.g. an emoji) as a single unit. Matching does not operate on
 * grapheme clusters or normalize the text, so a base character followed by a
 * combining mark is two code points and {@code é} (U+00E9) does not equal
 * {@code e} + U+0301. Wildcards match any character, including line terminators.
 * A {@code *} or {@code ?} in the text is an ordinary character and is not
 * treated as a wildcard.
 *
 * <p>
 * When case-insensitive, code points are compared after applying the simple
 * Unicode case mapping {@code toLowerCase(toUpperCase(cp))}, independent of the
 * default locale. Case mappings that change the length of the text (e.g.
 * {@code ß} to {@code SS}) are not applied.
 */
public final class GlobPattern {

    /**
     * The pattern code points. Every {@code *} and {@code ?} is a wildcard, as there
     * is no escape syntax, and all other code points are case-folded when matching
     * case-insensitively.
     */
    private final int[] patternCodePoints;
    private final boolean ignoreCase;

    private GlobPattern(int[] patternCodePoints, boolean ignoreCase) {
        this.patternCodePoints = patternCodePoints;
        this.ignoreCase = ignoreCase;
    }

    public static GlobPattern compile(String glob, boolean ignoreCase) {
        int[] codePoints = Objects.requireNonNull(glob).codePoints().toArray();
        int length = 0;

        for (int cp : codePoints) {
            if (cp == '*') {
                // Consecutive '*' are equivalent to a single '*'
                if (length == 0 || codePoints[length - 1] != '*') {
                    codePoints[length++] = cp;
                }
            } else if (cp == '?' || !ignoreCase) {
                codePoints[length++] = cp;
            } else {
                codePoints[length++] = fold(cp);
            }
        }

        return new GlobPattern(Arrays.copyOf(codePoints, length), ignoreCase);
    }

    private static int fold(int cp) {
        return Character.toLowerCase(Character.toUpperCase(cp));
    }

    private boolean matchesOne(int patternCp, int textCp) {
        if (patternCp == '?' || patternCp == textCp) {
            return true;
        }

        return ignoreCase && patternCp == fold(textCp);
    }

    /**
     * Test whether the entire given text matches this pattern.
     *
     * @param text the text to match, must not be null
     */
    public boolean matches(String text) {
        final int[] pattern = this.patternCodePoints;
        final int patternLen = pattern.length;
        final int textLen = text.length();

        int textIdx = 0;
        int patternIdx = 0;
        int lastWildcardIdx = -1;
        int textBacktrackIdx = -1;

        while (textIdx < textLen) {
            int textCp = text.codePointAt(textIdx);

            if (patternIdx < patternLen) {
                int patternCp = pattern[patternIdx];

                // '*' must be checked first so that a '*' in the text is not taken as a literal match
                if (patternCp == '*') {
                    // A '*' that ends the pattern matches all remaining text
                    if (patternIdx == patternLen - 1) {
                        return true;
                    }
                    // Save the backtrack state and advance the pattern
                    lastWildcardIdx = patternIdx;
                    textBacktrackIdx = textIdx;
                    patternIdx++;
                    continue;
                } else if (matchesOne(patternCp, textCp)) {
                    // Single character wildcard or exact code point match
                    textIdx += Character.charCount(textCp);
                    patternIdx++;
                    continue;
                }
            }

            if (lastWildcardIdx != -1) {
                // Backtrack: extend the last '*' by one full code point and resume after it
                patternIdx = lastWildcardIdx + 1;
                textBacktrackIdx += Character.charCount(text.codePointAt(textBacktrackIdx));
                textIdx = textBacktrackIdx;
            } else {
                // Mismatch with no '*' to fall back on
                return false;
            }
        }

        // The text is exhausted, only '*' may remain in the pattern
        while (patternIdx < patternLen && pattern[patternIdx] == '*') {
            patternIdx++;
        }

        return patternIdx == patternLen;
    }
}
