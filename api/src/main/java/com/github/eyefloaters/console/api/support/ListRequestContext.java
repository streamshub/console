package com.github.eyefloaters.console.api.support;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.function.BiFunction;
import java.util.function.Function;

import jakarta.json.JsonObject;
import jakarta.ws.rs.core.UriBuilder;

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

    final boolean pageBackRequest;
    final boolean rangeRequest;

    int totalRecords = 0;
    int candidateRecords = 0;
    int recordsIncluded = 0;
    int recordsBeforePage = 0;
    boolean rangeTruncated = false;

    final SortedSet<T> firstPageData;
    final SizeLimitedSortedSet<T> finalPageData;

    T firstPageEntry;
    T finalPageEntry;

    public ListRequestContext(ComparatorBuilder<T> comparatorBuilder, URI requestUri, ListFetchParams listParams, Function<JsonObject, T> cursorMapper) {
        this.comparatorBuilder = comparatorBuilder;
        this.requestUri = requestUri;
        this.listParams = listParams;

        sortComparator = comparatorBuilder.fromSort(listParams.getSortEntries());
        pageSize = listParams.getPageSize();

        List<String> badCursors = new ArrayList<>(2);
        pageBeginExclusive = mapCursor(ListFetchParams.PAGE_AFTER_PARAM, listParams.getPageAfter(), cursorMapper, badCursors);
        pageEndExclusive = mapCursor(ListFetchParams.PAGE_BEFORE_PARAM, listParams.getPageBefore(), cursorMapper, badCursors);

        if (!badCursors.isEmpty()) {
            throw new InvalidPageCursorException("One or more page cursors were invalid", badCursors);
        }

        beforePageComparator = Comparator.nullsFirst(sortComparator);
        afterPageComparator = Comparator.nullsLast(sortComparator);

        pageBackRequest = Objects.isNull(pageBeginExclusive) && Objects.nonNull(pageEndExclusive);
        rangeRequest = Objects.nonNull(pageBeginExclusive) && Objects.nonNull(pageEndExclusive);

        firstPageData = new SizeLimitedSortedSet<>(sortComparator, pageSize);
        /*
         * Records kept for final page is one larger than the actual number of
         * records that would be returned for that page to support rendering a
         * `page[after]` link.
         */
        finalPageData = new SizeLimitedSortedSet<>(sortComparator.reversed(), pageSize + 1);
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
        firstPageData.add(item);
        finalPageData.add(item);

        if (beforePageCursor(item)) {
            recordsBeforePage++;
            return false;
        }

        if (afterPageCursor(item)) {
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

    /**
     * Determine whether the provided item occurs prior to the start of the page.
     *
     * @param item current item being processed
     * @return true when the provided item occurs prior to the start of the page,
     *         else false.
     */
    public boolean beforePageBegin(T item) {
        if (pageBackRequest && candidateRecords > pageSize) {
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

        rangeTruncated = rangeRequest;

        return false;
    }

    /**
     * Build the {@code meta} object with a single {@code cursor} entry provided by
     * the given cursorBuilder. This meta object applies to a single record/entry in
     * a paginated list response.
     *
     * @param cursorBuilder a function used to build the cursor string from the list
     *                      of sort keys for the current request.
     * @return read-only map to be placed in the {@code page} entry of the
     *         {@code meta} object for a single list result record.
     */
    public Map<String, Object> buildPageMeta(Function<List<String>, String> cursorBuilder) {
        return Map.of("cursor", cursorBuilder.apply(getSortNames()));
    }


    public Map<String, Object> buildPageMeta() {
        Map<String, Object> pageMeta = new LinkedHashMap<>();

        pageMeta.put("total", totalRecords);

        if (rangeTruncated) {
            pageMeta.put("rangeTruncated", true);
        }

        pageMeta.put("pageNumber", (recordsBeforePage / pageSize) + 1);

        return pageMeta;
    }

    public Map<String, String> buildPageLinks(BiFunction<T, List<String>, String> cursorBuilder) {
        Map<String, String> links = new LinkedHashMap<>(4);

        UriBuilder builder = UriBuilder.fromUri(requestUri)
                .replaceQueryParam(ListFetchParams.PAGE_SORT_PARAM)
                .replaceQueryParam(ListFetchParams.PAGE_SIZE_PARAM)
                .replaceQueryParam(ListFetchParams.PAGE_AFTER_PARAM)
                .replaceQueryParam(ListFetchParams.PAGE_BEFORE_PARAM);

        if (listParams.getRawSort() != null) {
            builder.queryParam(ListFetchParams.PAGE_SORT_PARAM, listParams.getRawSort());
        }

        if (listParams.getRawPageSize() != null) {
            builder.queryParam(ListFetchParams.PAGE_SIZE_PARAM, listParams.getRawPageSize());
        }

        T firstDatasetEntry = firstPageData.first();

        if (Objects.equals(firstPageEntry, firstDatasetEntry)) {
            links.put("first", null);
            links.put("prev", null);
        } else {
            links.put("first", builder.build().toString());
            String prevCursor = cursorBuilder.apply(firstPageEntry, getSortNames());
            links.put("prev", builder.clone().queryParam(ListFetchParams.PAGE_BEFORE_PARAM, prevCursor).build().toString());
        }

        /*
         * We need to potentially resize the final page for cases when the last page
         * size is less than the full page size. The value of `totalRecords` will not
         * be completely calculated when this class is used in a stream until the stream
         * is completely consumed and `tally` has been called for each entry.
         */
        int finalPageSize = (totalRecords % pageSize) + 1;
        finalPageData.limit(finalPageSize);
        // final page was stored in reverse order
        T finalDatasetEntry = finalPageData.first();

        if (Objects.equals(finalPageEntry, finalDatasetEntry)) {
            links.put("next", null);
            links.put("last", null);
        } else {
            String nextCursor = cursorBuilder.apply(finalPageEntry, getSortNames());
            links.put("next", builder.clone().queryParam(ListFetchParams.PAGE_AFTER_PARAM, nextCursor).build().toString());
            String lastCursor = cursorBuilder.apply(finalPageData.last(), getSortNames());
            links.put("last", builder.clone().queryParam(ListFetchParams.PAGE_AFTER_PARAM, lastCursor).build().toString());
        }

        return links;
    }
}
