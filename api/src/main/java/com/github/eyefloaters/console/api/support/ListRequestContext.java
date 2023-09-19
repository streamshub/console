package com.github.eyefloaters.console.api.support;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import jakarta.json.JsonObject;

import com.github.eyefloaters.console.api.errors.client.InvalidPageCursorException;
import com.github.eyefloaters.console.api.model.ListFetchParams;

public class ListRequestContext<T> {

    final ComparatorBuilder<T> comparatorBuilder;
    final URI requestUri;
    final ListFetchParams listParams;
    final Comparator<T> sortComparator;

    final int pageSize;

    final T pageBeginExclusive;
    final Comparator<T> beforePageComparator;

    final T pageEndExclusive;
    final Comparator<T> afterPageComparator;

    int totalRecords = 0;
    int candidateRecords = 0;
    int recordsIncluded = 0;
    boolean rangeTruncated = false;

    T firstDatasetEntry;
    T finalDatasetEntry;

    T firstPageEntry;
    T finalPageEntry;

    public ListRequestContext(ComparatorBuilder<T> comparatorBuilder, URI requestUri, ListFetchParams listParams, Function<JsonObject, T> cursorMapper) {
        this.comparatorBuilder = comparatorBuilder;
        this.requestUri = requestUri;
        this.listParams = listParams;

        sortComparator = comparatorBuilder.fromSort(listParams.getSortEntries());
        pageSize = listParams.getPageSize();

        List<String> badCursors = new ArrayList<>(2);
        pageBeginExclusive = mapCursor("page[after]", listParams.getPageAfter(), cursorMapper, badCursors);
        pageEndExclusive = mapCursor("page[before]", listParams.getPageBefore(), cursorMapper, badCursors);

        if (!badCursors.isEmpty()) {
            throw new InvalidPageCursorException("One or more page cursors were invalid", badCursors);
        }

        beforePageComparator = Comparator.nullsFirst(sortComparator);
        afterPageComparator = Comparator.nullsLast(sortComparator);
    }

    static <C> C mapCursor(String name, JsonObject source, Function<JsonObject, C> mapper, List<String> badCursors) {
        try {
            return mapper.apply(source);
        } catch (Exception e) {
            badCursors.add(name);
            return null;
        }
    }

    public T tally(T item) {
        totalRecords++;
        return item;
    }

    public Comparator<T> getSortComparator() {
        return sortComparator;
    }

    public List<String> getSortEntries() {
        return listParams.getSortEntries();
    }

    public List<String> getSortNames() {
        return listParams.getSortNames();
    }

    public boolean betweenCursors(T item) {
        if (isLowestEntry(item)) {
            firstDatasetEntry = item;
        }

        if (isHighestEntry(item)) {
            finalDatasetEntry = item;
        }

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

    boolean isLowestEntry(T item) {
        return afterPageComparator.compare(item, firstDatasetEntry) <= 0;
    }

    boolean isHighestEntry(T item) {
        return beforePageComparator.compare(item, finalDatasetEntry) >= 0;
    }

    /**
     * Determine whether the provided item occurs prior to the start of the page.
     *
     * @param item current item being processed
     * @return true when the provided item occurs prior to the start of the page,
     *         else false.
     */
    public boolean beforePageBegin(T item) {
        if (Objects.isNull(pageBeginExclusive) && candidateRecords > pageSize) {
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

        rangeTruncated = pageBeginExclusive != null && pageEndExclusive != null;

        return false;
    }

    public Map<String, Object> buildPageMeta(Function<List<String>, String> cursorBuilder) {
        return Map.of("cursor", cursorBuilder.apply(getSortNames()));
    }

    public Map<String, Object> buildPageMeta() {
        Map<String, Object> pageMeta = new LinkedHashMap<>();

        pageMeta.put("total", totalRecords);

        if (rangeTruncated) {
            pageMeta.put("rangeTruncated", true);
        }

        return pageMeta;
    }
}
