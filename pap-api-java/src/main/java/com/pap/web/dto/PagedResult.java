package com.pap.web.dto;

import java.util.List;
import java.util.function.Function;

/** One page of results with the total count across all pages. Pages are zero-based. */
public record PagedResult<T>(List<T> items, int totalCount, int page, int pageSize) {

    /** Slices {@code all} into the requested page and maps each item. */
    public static <E, T> PagedResult<T> of(List<E> all, int page, int pageSize, Function<E, T> mapper) {
        long skip = Math.max(0L, (long) page * pageSize);
        List<T> items = all.stream()
                .skip(skip)
                .limit(Math.max(0, pageSize))
                .map(mapper)
                .toList();
        return new PagedResult<>(items, all.size(), page, pageSize);
    }
}
