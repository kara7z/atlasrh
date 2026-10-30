package com.example.atlasrh.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import org.springframework.data.domain.Page;

@Schema(description = "Stable page envelope. number is zero-based; totalElements/totalPages count all matching records, even for an empty out-of-range page.")
public record PageResponse<T>(List<T> content, int number, int size, long totalElements, int totalPages,
    boolean first, boolean last) {
  public static <T> PageResponse<T> from(Page<T> page) {
    return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
        page.getTotalPages(), page.isFirst(), page.isLast());
  }
}
