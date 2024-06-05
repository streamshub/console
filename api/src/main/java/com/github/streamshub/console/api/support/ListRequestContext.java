package com.github.streamshub.console.api.support;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import jakarta.json.JsonObject;
import jakarta.ws.rs.core.UriBuilder;

import com.github.streamshub.console.api.errors.client.InvalidPageCursorException;
import com.github.streamshub.console.api.model.ListFetchParams;

public class ListRequestContext<T> implements Predicate<T> {

    List<Predicate<T>> filters;
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
    final Map<String, Object> meta = new LinkedHashMap<>();

    T firstPageEntry;
    T finalPageEntry;

    public ListRequestContext(List<Predicate<T>> filters, ComparatorBuilder<T> comparatorBuilder, URI requestUri, ListFetchParams listParams, Function<JsonObject, T> cursorMapper) {
        this.filters = Objects.requireNonNull(filters);
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

    public ListRequestContext(ComparatorBuilder<T> comparatorBuilder, URI requestUri, ListFetchParams listParams, Function<JsonObject, T> cursorMapper) {
        this(Collections.emptyList(), comparatorBuilder, requestUri, listParams, cursorMapper);
    }

    static <C> C mapCursor(String name, JsonObject source, Function<JsonObject, C> mapper, List<String> badCursors) {
        try {
            return mapper.apply(source);
        } catch (Exception e) {
            badCursors.add(name);
            return null;
        }
    }

    public List<Predicate<T>> filters() {
        return Collections.unmodifiableList(filters);
    }

    @Override
    public boolean test(T t) {
        return filters.isEmpty() || filters.stream().allMatch(filter -> filter.test(t));
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

    public Map<String, Object> meta() {
        return meta;
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

        if (totalRecords > 0) {
            pageMeta.put("pageNumber", (recordsBeforePage / pageSize) + 1);
        }

        return pageMeta;
    }

    public Map<String, String> buildPageLinks(BiFunction<T, List<String>, String> cursorBuilder) {
        Map<String, String> links = new LinkedHashMap<>(4);

        UriBuilder builder = UriBuilder.fromUri(requestUri)
                .replaceQueryParam(ListFetchParams.PAGE_SORT_PARAM)
                .replaceQueryParam(ListFetchParams.PAGE_SIZE_PARAM)
                .replaceQueryParam(ListFetchParams.PAGE_AFTER_PARAM)
                .replaceQueryParam(ListFetchParams.PAGE_BEFORE_PARAM);

        List<String> sortEntries = comparatorBuilder.knownSortKeys(listParams.getSortEntries());

        if (!sortEntries.isEmpty()) {
            builder.queryParam(ListFetchParams.PAGE_SORT_PARAM, String.join(",", sortEntries));
        }

        if (listParams.getRawPageSize() != null) {
            builder.queryParam(ListFetchParams.PAGE_SIZE_PARAM, listParams.getRawPageSize());
        }

        if (totalRecords > pageSize) {
            links.put("first", builder.build().toString());
        } else {
            // No link for a single page
            links.put("first", null);
        }

        T firstDatasetEntry = firstPageData.isEmpty() ? null : firstPageData.first();

        if (Objects.isNull(firstPageEntry) || Objects.equals(firstPageEntry, firstDatasetEntry)) {
            links.put("prev", null);
        } else {
            String prevCursor = cursorBuilder.apply(firstPageEntry, getSortNames());
            links.put("prev", builder.clone().queryParam(ListFetchParams.PAGE_BEFORE_PARAM, prevCursor).build().toString());
        }

        /*
         * We need to potentially resize the final page for cases when the last page
         * size is less than the full page size. The value of `totalRecords` will not
         * be completely calculated when this class is used in a stream until the stream
         * is completely consumed and `tally` has been called for each entry.
         */
        int finalPageRemainder = totalRecords % pageSize;
        if (finalPageRemainder > 0) {
            int finalPageSize = finalPageRemainder + 1;
            finalPageData.limit(finalPageSize);
        }

        // final page was stored in reverse order
        T finalDatasetEntry = finalPageData.isEmpty() ? null : finalPageData.first();

        if (Objects.isNull(finalPageEntry) || Objects.equals(finalPageEntry, finalDatasetEntry)) {
            links.put("next", null);
        } else {
            String nextCursor = cursorBuilder.apply(finalPageEntry, getSortNames());
            links.put("next", builder.clone().queryParam(ListFetchParams.PAGE_AFTER_PARAM, nextCursor).build().toString());
        }

        if (totalRecords > pageSize) {
            /*
             * Because finalPageData is sorted in descending order from the end of the
             * dataset and it's size is one greater than the actual page size, the final
             * entry of the set is the last record on the previous page. This is used to
             * create the cursor for the page[after] parameter.
             */
            String lastCursor = cursorBuilder.apply(finalPageData.last(), getSortNames());
            links.put("last", builder.clone().queryParam(ListFetchParams.PAGE_AFTER_PARAM, lastCursor).build().toString());
        } else {
            // No link for a single page
            links.put("last", null);
        }

        return links;
    }
}
