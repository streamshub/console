package com.github.eyefloaters.console.api.support;

import java.util.Comparator;
import java.util.TreeSet;

/**
 * A SortedSet that is limited to a fixed size. Records beyond the size limit
 * will be removed immediately. This class is useful for building paged data
 * sets.
 *
 * @param <E>
 */
@SuppressWarnings("java:S2160")
// Ignore Sonar warning about missing equals override, not necessary for the intended use of this class
public class SizeLimitedSortedSet<E> extends TreeSet<E> {

    private static final long serialVersionUID = 1L;

    int limit;

    public SizeLimitedSortedSet(Comparator<E> order, int limit) {
        super(order);
        this.limit = limit;
    }

    @Override
    public boolean add(E entry) {
        boolean added = super.add(entry);
        truncate();
        return added;
    }

    public int limit() {
        return limit;
    }

    public void limit(int limit) {
        this.limit = limit;
        truncate();
    }

    private void truncate() {
        while (size() > limit) {
            pollLast();
        }
    }
}
