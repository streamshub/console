package com.github.streamshub.console.api.support;

import java.util.Comparator;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SizeLimitedSortedSetTest {

    static final Comparator<Integer> INTEGER_SORT = Integer::compare;

    @Test
    void testSetSizeLimited() {
        SizeLimitedSortedSet<Integer> target = new SizeLimitedSortedSet<>(INTEGER_SORT, 10);
        assertEquals(10, target.limit());
        IntStream.range(0, 11).forEach(target::add);
        assertEquals(10, target.size());
        assertEquals(0, target.first());
        assertEquals(9, target.last());
        target.limit(5);
        assertEquals(5, target.size());
        assertEquals(0, target.first());
        assertEquals(4, target.last());
    }

    @Test
    void testSetSizeLimitedReversed() {
        SizeLimitedSortedSet<Integer> target = new SizeLimitedSortedSet<>(INTEGER_SORT.reversed(), 10);
        assertEquals(10, target.limit());
        IntStream.range(0, 11).forEach(target::add);
        assertEquals(10, target.size());
        assertEquals(10, target.first());
        assertEquals(1, target.last());
        target.limit(5);
        assertEquals(5, target.size());
        assertEquals(10, target.first());
        assertEquals(6, target.last());
    }

    @Test
    void testSetEqualsWithDifferentLimit() {
        SizeLimitedSortedSet<Integer> t1 = new SizeLimitedSortedSet<>(INTEGER_SORT, 10);
        SizeLimitedSortedSet<Integer> t2 = new SizeLimitedSortedSet<>(INTEGER_SORT, 20);
        IntStream.range(0, 10).forEach(i -> {
            t1.add(i);
            t2.add(i);
        });
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }
}
