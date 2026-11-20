package io.streamshub.console.api.support;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.streamshub.console.api.model.FetchFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobPatternTest {

    @ParameterizedTest
    @CsvSource(delimiter = '~', value = {
        // pattern ~ text ~ expected
        "abc       ~ abc       ~ true",
        "abc       ~ abd       ~ false",
        "abc       ~ ab        ~ false",
        "abc       ~ abcd      ~ false",
        "a?c       ~ abc       ~ true",
        "a?c       ~ ac        ~ false",
        "a?c       ~ abbc      ~ false",
        "*         ~ ''        ~ true",
        "*         ~ anything  ~ true",
        "**        ~ anything  ~ true",
        "a*        ~ a         ~ true",
        "a*        ~ abc       ~ true",
        "a*        ~ ba        ~ false",
        "*c        ~ abc       ~ true",
        "*c        ~ abcd      ~ false",
        "a*c       ~ ac        ~ true",
        "a*c       ~ abbbc     ~ true",
        "a*c       ~ abbbd     ~ false",
        "*a*b*c*   ~ xaxbxcx   ~ true",
        "*a*b*c*   ~ xcxbxax   ~ false",
        // the last '*' must be able to extend to a later occurrence of the trailing literal
        "*b        ~ abxb      ~ true",
        "a*b*b     ~ abxb      ~ true",
        "a*bc      ~ abcbc     ~ true",
        "*aab      ~ aaab      ~ true",
        "a*?c      ~ abc       ~ true",
        "a*?c      ~ ac        ~ false",
        "a?*       ~ ab        ~ true",
        "a?*       ~ a         ~ false",
        "''        ~ ''        ~ true",
        "''        ~ a         ~ false",
        // regex metacharacters are literal
        "a.b       ~ a.b       ~ true",
        "a.b       ~ axb       ~ false",
        "a+b       ~ a+b       ~ true",
        "a+b       ~ aab       ~ false",
        "[a-z]     ~ [a-z]     ~ true",
        "[a-z]     ~ b         ~ false",
        "(a|b)     ~ (a|b)     ~ true",
        "(a|b)     ~ a         ~ false",
        "a\\d      ~ a\\d      ~ true",
        "a\\d      ~ a1        ~ false",
        "^a$       ~ ^a$       ~ true",
        "^a$       ~ a         ~ false",
        // '*' and '?' in the text are ordinary characters, they must not be taken as a match for a literal
        "a*b       ~ a*xb      ~ true",
        "a*b       ~ a**b      ~ true",
        "a*b       ~ a?b       ~ true",
        "*         ~ *abc      ~ true",
        "*b        ~ *b        ~ true",
        "*b        ~ **b       ~ true",
        "*b        ~ *c        ~ false",
        "a*        ~ a*        ~ true",
        "*x*       ~ *x        ~ true",
        "*x*       ~ *y        ~ false",
        "?b        ~ ?b        ~ true",
        "?b        ~ *b        ~ true",
        "a?c       ~ a*c       ~ true",
        "a?c       ~ a?c       ~ true",
        "a*c*d     ~ a*c*d     ~ true",
        "a*c*d     ~ a*x*c*d   ~ true",
        "a*c*d     ~ a*x*c*e   ~ false",
        // wildcard characters in the text count as one character each
        "a**b      ~ a*b       ~ true",
        "a?b       ~ a**b      ~ false",
        // a trailing '*' matches the rest of the text, including '*' and '?'
        "ab*       ~ ab        ~ true",
        "ab*       ~ ab*?*     ~ true",
        "ab?*      ~ ab        ~ false",
        "ab?*      ~ ab*       ~ true",
        "*a*       ~ xxa       ~ true",
        "*a*       ~ xxb       ~ false"
    })
    void testMatches(String pattern, String text, boolean expected) {
        assertEquals(expected, GlobPattern.compile(pattern, false).matches(text));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "\n", "\r", "\r\n", "\u0085", "\u2028", "\u2029", "\u000B", "\u000C"
    })
    void testWildcardsMatchLineTerminators(String terminator) {
        // '*' in every position: start, middle, end, and alone
        assertTrue(GlobPattern.compile("*", false).matches(terminator));
        assertTrue(GlobPattern.compile("*", false).matches("a" + terminator + "b"));
        assertTrue(GlobPattern.compile("a*b", false).matches("a" + terminator + "b"));
        assertTrue(GlobPattern.compile("a*", false).matches("a" + terminator));
        assertTrue(GlobPattern.compile("*b", false).matches(terminator + "b"));
        assertTrue(GlobPattern.compile("*a*b*", false).matches(terminator + "a" + terminator + "b" + terminator));

        // '?' matches exactly one code point, and a two-char terminator ("\r\n") is two code points
        int codePoints = terminator.codePointCount(0, terminator.length());
        assertTrue(GlobPattern.compile("a" + "?".repeat(codePoints) + "b", false).matches("a" + terminator + "b"));
        assertFalse(GlobPattern.compile("a" + "?".repeat(codePoints + 1) + "b", false).matches("a" + terminator + "b"));

        // Literal text is still compared exactly
        assertFalse(GlobPattern.compile("a*b", false).matches("a" + terminator + "c"));
        assertTrue(GlobPattern.compile("a" + terminator + "b", false).matches("a" + terminator + "b"));
        assertFalse(GlobPattern.compile("a" + terminator + "b", false).matches("ab"));
    }

    @Test
    void testQuestionMarkMatchesSupplementaryCharacterAsOne() {
        String emoji = new String(Character.toChars(0x1F600));
        assertTrue(GlobPattern.compile("a?b", false).matches("a" + emoji + "b"));
        assertFalse(GlobPattern.compile("a??b", false).matches("a" + emoji + "b"));
        assertTrue(GlobPattern.compile("a*b", false).matches("a" + emoji + emoji + "b"));
        assertTrue(GlobPattern.compile("a" + emoji + "b", false).matches("a" + emoji + "b"));
        assertFalse(GlobPattern.compile("a" + emoji + "b", false).matches("a" + new String(Character.toChars(0x1F601)) + "b"));
    }

    @Test
    void testLoneSurrogatesDoNotFail() {
        // Malformed UTF-16 is treated as individual code points rather than rejected
        assertTrue(GlobPattern.compile("a?b", false).matches("a\uD83Db"));
        assertTrue(GlobPattern.compile("a*", false).matches("a\uDE00"));
        assertFalse(GlobPattern.compile("a??b", false).matches("a\uD83Db"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '~', value = {
        // pattern ~ text ~ expected
        "caf\u00e9           ~ caf\u00e9              ~ true",
        "caf?               ~ caf\u00e9              ~ true",
        "caf\u00e9           ~ cafe                   ~ false",
        "\u65e5\u672c\u8a9e  ~ \u65e5\u672c\u8a9e     ~ true",
        "\u65e5?\u8a9e      ~ \u65e5\u672c\u8a9e     ~ true",
        "*\u8a9e            ~ \u65e5\u672c\u8a9e     ~ true",
        "\u65e5*            ~ \u65e5\u672c\u8a9e     ~ true",
        "\u65e5??           ~ \u65e5\u672c            ~ false",
        "*\u0434*           ~ \u043f\u0440\u0438\u0432\u0435\u0442 \u0434\u0440\u0443\u0433 ~ true",
        // precomposed e-acute is not equal to e + combining acute (no normalization)
        "\u00e9             ~ e\u0301                ~ false",
        "?                  ~ e\u0301                ~ false",
        "??                 ~ e\u0301                ~ true"
    })
    void testUnicodeCaseSensitive(String pattern, String text, boolean expected) {
        assertEquals(expected, GlobPattern.compile(pattern, false).matches(text));
    }

    @Test
    void testCaseSensitivity() {
        assertFalse(GlobPattern.compile("a?C*", false).matches("ABCD"));
        assertTrue(GlobPattern.compile("a?C*", true).matches("ABcD"));
        assertTrue(GlobPattern.compile("ABC", true).matches("abc"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '~', value = {
        // pattern ~ text, case-insensitive matches in both directions
        "\u00e9              ~ \u00c9",              // Latin-1: e-acute / E-acute
        "\u00fcber           ~ \u00dcBER",           // u-umlaut
        "\u03b1\u03b2\u03b3  ~ \u0391\u0392\u0393",  // Greek alpha beta gamma
        "\u03c3              ~ \u03a3",              // Greek sigma / capital sigma
        "\u03c3              ~ \u03c2",              // Greek final sigma folds to sigma
        "\u043f\u0440\u0438  ~ \u041f\u0420\u0418",  // Cyrillic
        "\u0561              ~ \u0531",              // Armenian
        "\u1e9e              ~ \u00df"               // capital sharp s / sharp s (simple fold)
    })
    void testUnicodeCaseInsensitive(String pattern, String text) {
        assertTrue(GlobPattern.compile(pattern, true).matches(text), "pattern -> text");
        assertTrue(GlobPattern.compile(text, true).matches(pattern), "text -> pattern");
        assertFalse(GlobPattern.compile(pattern, false).matches(text), "case-sensitive must not match");
    }

    @Test
    void testUnicodeCaseInsensitiveSupplementaryPlane() {
        // Deseret small/capital long I, both outside the BMP
        String lower = new String(Character.toChars(0x10428));
        String upper = new String(Character.toChars(0x10400));

        assertTrue(GlobPattern.compile(lower, true).matches(upper));
        assertTrue(GlobPattern.compile(upper, true).matches(lower));
        assertTrue(GlobPattern.compile("a*" + lower, true).matches("A" + upper + upper));
        assertFalse(GlobPattern.compile(lower, false).matches(upper));
    }

    @Test
    void testCaseInsensitiveDoesNotChangeTextLength() {
        // Only simple (one code point to one code point) folding is performed
        assertFalse(GlobPattern.compile("SS", true).matches("\u00df"));
        assertFalse(GlobPattern.compile("\u00df", true).matches("SS"));
        assertFalse(GlobPattern.compile("?", true).matches("SS"));
    }

    @Test
    void testCaseInsensitiveIsLocaleIndependent() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            // Under a Turkish locale String.toLowerCase would map 'I' to dotless i; this must not
            assertTrue(GlobPattern.compile("title", true).matches("TITLE"));
            assertTrue(GlobPattern.compile("TITLE", true).matches("title"));
        } finally {
            Locale.setDefault(original);
        }
    }

    @ParameterizedTest
    @CsvSource(delimiter = '~', value = {
        "a**b       ~ a*b",
        "a***b      ~ a*b",
        "***        ~ *",
        "**a        ~ *a",
        "a**        ~ a*",
        "*a**b***c* ~ *a*b*c*",
        // '*' separated by '?' are not consecutive and must not be collapsed
        "*?*        ~ *?*",
        "a*?*b      ~ a*?*b"
    })
    void testConsecutiveStarsAreEquivalentToOne(String collapsible, String canonical) {
        String[] texts = {"", "a", "b", "ab", "ac", "aXb", "abc", "aXbYc", "XaYbZc", "cba", "aa", "aab", "\n", "a\nb"};

        for (String text : texts) {
            assertEquals(
                    GlobPattern.compile(canonical, false).matches(text),
                    GlobPattern.compile(collapsible, false).matches(text),
                    () -> "pattern=[%s] vs [%s], text=[%s]".formatted(collapsible, canonical, text));
        }
    }

    @Test
    void testLongRunOfStarsIsCollapsed() {
        // Would be a large polynomial amount of work for a regex ".*.*.*...", and is trivial after collapsing
        String pattern = "*".repeat(100_000) + "b";
        String text = "a".repeat(100_000);
        GlobPattern target = GlobPattern.compile(pattern, false);

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            assertFalse(target.matches(text));
            assertTrue(target.matches(text + "b"));
        });
    }

    @Test
    void testManyWildcardsDoNotBacktrackExponentially() {
        // Pathological for a backtracking regex: ".*a.*a.*a...b" against a string without 'b'
        String pattern = "*a".repeat(30) + "*b";
        String text = "a".repeat(10_000);
        GlobPattern target = GlobPattern.compile(pattern, false);

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            assertFalse(target.matches(text));
        });
    }

    @Test
    void testSharpSIsCaseInsensitiveInBothDirections() {
        String sharp = "\u00df";
        String capital = "\u1e9e";

        // Unlike java.util.regex, the result does not depend on whether other characters surround the literal
        assertTrue(GlobPattern.compile(sharp, true).matches(capital));
        assertTrue(GlobPattern.compile(capital, true).matches(sharp));
        assertTrue(GlobPattern.compile(sharp + "a", true).matches(capital + "A"));
        assertTrue(GlobPattern.compile(sharp + "*a", true).matches(capital + "xA"));
        assertFalse(GlobPattern.compile(sharp, false).matches(capital));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void testAgreesWithRegexTranslationOnRandomInput(boolean ignoreCase) {
        Random random = new Random(42);
        // ASCII, Latin-1 and Greek case pairs, a supplementary plane case pair, and line terminators
        // Includes characters whose case mapping is irregular: y-diaeresis, micro sign, Greek mu, dotless/dotted i, Kelvin sign, long s.
        // Sharp s (U+00DF) and capital sharp s (U+1E9E) are deliberately excluded: java.util.regex is inconsistent for them (a lone
        // sharp s does not match U+1E9E while a longer literal does), see testSharpSIsCaseInsensitiveInBothDirections.
        String[] patternAlphabet = {"a", "A", "b", "*", "*", "?", ".", "\u00e9", "\u00c9", "\u03c3", "\u03a3", "\ud801\udc28", "\ud801\udc00", "\n",
            "\u00ff", "\u0178", "\u00b5", "\u03bc", "\u0131", "\u0130", "i", "I", "k", "K", "\u212a", "s", "\u017f"};
        String[] textAlphabet = {"a", "A", "b", ".", "*", "?", "\u00e9", "\u00c9", "\u03c3", "\u03a3", "\u03c2", "\ud801\udc28", "\ud801\udc00", "\n", "\r\n",
            "\u00ff", "\u0178", "\u00b5", "\u039c", "\u03bc", "\u0131", "\u0130", "i", "I", "k", "K", "\u212a", "s", "S", "\u017f"};
        int flags = Pattern.DOTALL | (ignoreCase ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0);

        for (int i = 0; i < 50_000; i++) {
            String glob = random(random, patternAlphabet, random.nextInt(8));
            String text = random(random, textAlphabet, random.nextInt(10));
            boolean expected = Pattern.compile(toRegex(glob), flags).matcher(text).matches();

            assertEquals(expected, GlobPattern.compile(glob, ignoreCase).matches(text),
                    () -> "glob=[%s], text=[%s], ignoreCase=%s".formatted(glob, text, ignoreCase));
        }
    }

    @ParameterizedTest
    @CsvSource(delimiter = '~', value = {
        "like,a*b?  ~ aXXbY  ~ true",
        "like,a*b?  ~ AXXbY  ~ false",
        "ilike,a*b? ~ AXXbY  ~ true",
        "ilike,a*b? ~ AXXbYZ ~ false"
    })
    void testFetchFilterPredicateLikeOperators(String filter, String value, boolean expected) {
        FetchFilter fetchFilter = FetchFilter.valueOf(filter);
        FetchFilterPredicate<String, String> predicate = new FetchFilterPredicate<>(fetchFilter, s -> s);
        assertEquals(expected, predicate.test(value));
    }

    @Test
    void testFetchFilterPredicateLikeNullFieldDoesNotMatch() {
        FetchFilter fetchFilter = FetchFilter.valueOf("like,*");
        FetchFilterPredicate<String, String> predicate = new FetchFilterPredicate<>(fetchFilter, s -> null);
        assertFalse(predicate.test("ignored"));
    }

    @Test
    void testFetchFilterPredicateOperatorAndOperands() {
        FetchFilter fetchFilter = FetchFilter.valueOf("like,a*");
        FetchFilterPredicate<String, String> predicate = new FetchFilterPredicate<>(fetchFilter, s -> s);
        assertEquals(FetchFilterPredicate.Operator.LIKE_PATTERN, predicate.operator());
        assertEquals(List.of("a*"), predicate.operands());
    }

    private static String random(Random random, String[] alphabet, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(alphabet[random.nextInt(alphabet.length)]);
        }
        return sb.toString();
    }

    /**
     * The regex translation previously used by {@link FetchFilterPredicate}, used as an oracle.
     * Operates on code points so that supplementary characters are quoted intact.
     */
    private static String toRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        glob.codePoints().forEach(c -> {
            switch (c) {
                case '*' -> regex.append(".*");
                case '?' -> regex.append(".");
                default -> regex.append(Pattern.quote(new String(Character.toChars(c))));
            }
        });
        return regex.toString();
    }
}
