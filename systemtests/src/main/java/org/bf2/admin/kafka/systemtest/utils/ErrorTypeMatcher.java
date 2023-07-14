package org.bf2.admin.kafka.systemtest.utils;

import org.bf2.admin.kafka.admin.model.ErrorType;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.Map;

import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

public class ErrorTypeMatcher extends TypeSafeMatcher<Map<String, ?>> {

    private ErrorType error;

    private Matcher<?> kindMatcher;
    private Matcher<?> idMatcher;
    private Matcher<?> hrefMatcher;
    private Matcher<?> reasonMatcher;

    private Matcher<?> codeMatcher;
    private Matcher<?> messageMatcher;
    private Matcher<?> detailMatcher;

    public ErrorTypeMatcher(ErrorType error, Matcher<?> detailMatcher) {
        this.error = error;
        this.kindMatcher = hasEntry("kind", "Error");
        this.idMatcher = hasEntry("id", error.getId());
        this.hrefMatcher = hasEntry("href", "/api/v1/errors/" + error.getId());
        this.reasonMatcher = hasEntry("reason", error.getReason());
        this.detailMatcher = detailMatcher;

        // Matchers for deprecated fields
        this.codeMatcher = hasEntry("code", error.getHttpStatus().getStatusCode());
        this.messageMatcher = hasEntry("error_message", error.getReason());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("matchesError(").appendValue(error).appendText(")");
    }

    @Override
    protected boolean matchesSafely(Map<String, ?> map) {
        return kindMatcher.matches(map)
                && idMatcher.matches(map)
                && hrefMatcher.matches(map)
                && reasonMatcher.matches(map)
                && detailMatcher.matches(map)
                && codeMatcher.matches(map)
                && messageMatcher.matches(map);
    }

    public static Matcher<Map<String, ?>> matchesError(ErrorType error) {
        return matchesError(error, (String) null);
    }

    public static Matcher<Map<String, ?>> matchesError(ErrorType error, String detail) {
        Matcher<?> detailMatcher;

        if (detail != null) {
            detailMatcher = hasEntry("detail", detail);
        } else {
            detailMatcher = either(not(hasKey("detail"))).or(hasEntry(equalTo("detail"), nullValue()));
        }

        return new ErrorTypeMatcher(error, detailMatcher);
    }

    public static Matcher<Map<String, ?>> matchesError(ErrorType error, Matcher<?> detailMatcher) {
        return new ErrorTypeMatcher(error, hasEntry(equalTo("detail"), detailMatcher));
    }
}
