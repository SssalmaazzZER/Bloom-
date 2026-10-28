package ma.bloome.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Stable pagination envelope (independent of Spring Data's internal Page JSON format). */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
                page.isFirst(), page.isLast());
    }

    public static <T> PageResponse<T> ofMapped(Page<?> page, List<T> content) {
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
