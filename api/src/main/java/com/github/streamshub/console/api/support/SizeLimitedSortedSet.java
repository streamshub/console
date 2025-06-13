package com.github.streamshub.console.api.support;

import java.util.Comparator;
import java.util.Objects;
import java.util.TreeSet;

/**
 * A SortedSet that is limited to a fixed size. The "greatest" records beyond
 * the size limit will be removed immediately based on order determined by the
 * {@linkplain Comparator} given in the constructor. This class is useful for
 * building paged data sets.
 *
 * For example, given lexicographic order determined by
 * {@linkplain String#compareTo(String)}, an instance of this set with the
 * elements [ A, C ] and a limit of 3 would become [ A, B, C ] when the elements
 * B and D are {@linkplain #add(Object) added}. Element D is sorted to the end
 * of the set based on the comparator and removed.
 *
 * The {@linkplain #add(Object) add} operation is not atomic and therefore
 * element D may be visible to other threads prior to the set being
 * {@linkplain #truncate() truncated}. If this is a concern, access to the set
 * should be {@linkplain java.util.Collections#synchronizedSet(java.util.Set)
 * synchronized}.
 *
 * @param <E> the type of elements maintained by this set
 */
public class SizeLimitedSortedSet<E> extends TreeSet<E> {

    private static final long serialVersionUID = 1L;

    int limit;

    public SizeLimitedSortedSet(Comparator<E> order, int limit) {
        super(order);
        this.limit = limit;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(limit);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SizeLimitedSortedSet<?> other = (SizeLimitedSortedSet<?>) obj;
        return limit == other.limit;
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
