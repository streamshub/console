package com.github.streamshub.console.test;

import java.util.Map;
import java.util.Map.Entry;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static org.hamcrest.core.IsAnything.anything;
import static org.hamcrest.core.IsEqual.equalTo;

// XXX: consider contributing this to Hamcrest
public class EveryEntry<K, V> extends TypeSafeMatcher<Map<? extends K, ? extends V>> {

    private final Matcher<? super K> keyMatcher;
    private final Matcher<? super V> valueMatcher;

    public EveryEntry(Matcher<? super K> keyMatcher, Matcher<? super V> valueMatcher) {
        this.keyMatcher = keyMatcher;
        this.valueMatcher = valueMatcher;
    }

    @Override
    public boolean matchesSafely(Map<? extends K, ? extends V> map) {
        for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
            if (!keyMatcher.matches(entry.getKey()) || !valueMatcher.matches(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void describeMismatchSafely(Map<? extends K, ? extends V> map, Description mismatchDescription) {
        mismatchDescription.appendText("map was ").appendValueList("[", ", ", "]", map.entrySet());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("map with every entry [")
                   .appendDescriptionOf(keyMatcher)
                   .appendText("->")
                   .appendDescriptionOf(valueMatcher)
                   .appendText("]");
    }

    public static <K, V> Matcher<Map<? extends K, ? extends V>> everyEntry(Matcher<? super K> keyMatcher, Matcher<? super V> valueMatcher) {
        return new EveryEntry<>(keyMatcher, valueMatcher);
    }

    public static <K, V> Matcher<Map<? extends K, ? extends V>> everyEntry(K key, V value) {
        return new EveryEntry<>(equalTo(key), equalTo(value));
    }

    public static <K> Matcher<Map<? extends K, ?>> everyKey(Matcher<? super K> keyMatcher) {
        return new EveryEntry<>(keyMatcher, anything());
    }

    public static <K> Matcher<Map<? extends K, ?>> everyKey(K key) {
        return new EveryEntry<>(equalTo(key), anything());
    }

    public static <V> Matcher<Map<?, ? extends V>> everyValue(Matcher<? super V> valueMatcher) {
        return new EveryEntry<>(anything(), valueMatcher);
    }

    public static <V> Matcher<Map<?, ? extends V>> everyValue(V value) {
        return new EveryEntry<>(anything(), equalTo(value));
    }
}