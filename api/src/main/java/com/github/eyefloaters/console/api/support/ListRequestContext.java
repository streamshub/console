package com.github.eyefloaters.console.api.support;

import java.net.URI;
import java.util.Comparator;
import java.util.function.Function;

import jakarta.json.JsonObject;

import com.github.eyefloaters.console.api.model.ListFetchParams;

public class ListRequestContext<T> {

    final ComparatorBuilder<T> comparatorBuilder;
    final URI requestUri;
    final String sort;
    final Comparator<T> sortComparator;

    final int pageSize;

    final T pageBeginExclusive;
    final Comparator<T> beforePageComparator;

    final T pageEndExclusive;
    final Comparator<T> afterPageComparator;

    int totalRecords = 0;
    int candidateRecords = 0;
    int recordsIncluded = 0;

    T firstPageEntry;
    T finalPageEntry;

    public ListRequestContext(ComparatorBuilder<T> comparatorBuilder, URI requestUri, ListFetchParams listParams, Function<JsonObject, T> cursorMapper) {
        this.comparatorBuilder = comparatorBuilder;
        this.requestUri = requestUri;
        sort = listParams.getSort();
        sortComparator = comparatorBuilder.fromSort(sort);
        pageSize = listParams.getPageSize();

        pageBeginExclusive = cursorMapper.apply(listParams.getPageAfter());
        beforePageComparator = Comparator.nullsFirst(sortComparator);

        pageEndExclusive = cursorMapper.apply(listParams.getPageBefore());
        afterPageComparator = Comparator.nullsLast(sortComparator);
    }

    public T tally(T item) {
        totalRecords++;
        return item;
    }

    public Comparator<T> getSortComparator() {
        return sortComparator;
    }

    public String getSort() {
        return sort;
    }

    public boolean betweenCursors(T item) {
        if (beforePageCursor(item) || afterPageCursor(item)) {
            return false;
        }

        candidateRecords++;
        return true;
    }

    boolean beforePageCursor(T item) {
        return beforePageComparator.compare(item, pageBeginExclusive) <= 0;
    }

    boolean afterPageCursor(T item) {
        return afterPageComparator.compare(item, pageEndExclusive) >= 0;
    }

    public boolean beforePageBegin(T item) {
        if (pageEndExclusive == null) {
            return false;
        }

        if (candidateRecords > pageSize) {
            candidateRecords--;
            return true;
        }

        return false;
    }

    public boolean pageCapacityAvailable(T item) {
        if (firstPageEntry == null) {
            firstPageEntry = item;
        }

        if (++recordsIncluded <= pageSize) {
            finalPageEntry = item;
            return true;
        }

        return false;
    }
}
