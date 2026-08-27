package com.futurekawa.backendcentral.dto.envelope;

import java.util.List;

// Spring's Page<T> is an interface, so Jackson cannot deserialize a backend-local
// response into it without a mixin. This record carries the contract's fields verbatim.
public record PageDto<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int number,
        int size,
        int numberOfElements,
        boolean first,
        boolean last,
        boolean empty
) {
    public static <T> PageDto<T> empty(int size) {
        return new PageDto<>(List.of(), 0L, 0, 0, size, 0, true, true, true);
    }
}
