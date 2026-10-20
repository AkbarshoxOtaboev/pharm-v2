package uz.uwon.pharm.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageRequests {

    private static final int MAX_SIZE = 500;

    private PageRequests() {
    }

    /** {@code size <= 0} means "all rows" (unpaged). */
    public static Pageable of(int page, int size, Sort sort) {
        if (size <= 0) {
            return Pageable.unpaged(sort);
        }
        return PageRequest.of(Math.max(page, 0), Math.min(size, MAX_SIZE), sort);
    }
}
