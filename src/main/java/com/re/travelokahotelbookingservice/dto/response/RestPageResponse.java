package com.re.travelokahotelbookingservice.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom Page implementation hỗ trợ Jackson serialization/deserialization cho Redis Cache.
 * Giải quyết triệt để lỗi "Cannot construct instance of PageImpl" và "PageRequest" khi đọc dữ liệu phân trang từ Redis.
 */
@JsonIgnoreProperties(value = {"pageable", "sort"}, ignoreUnknown = true)
public class RestPageResponse<T> extends PageImpl<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RestPageResponse(@JsonProperty("content") List<T> content,
                            @JsonProperty("number") int number,
                            @JsonProperty("size") int size,
                            @JsonProperty("totalElements") Long totalElements) {
        super(content != null ? content : new ArrayList<>(),
              PageRequest.of(Math.max(number, 0), size > 0 ? size : 10),
              totalElements != null ? totalElements : 0);
    }

    public RestPageResponse(List<T> content, Pageable pageable, long total) {
        super(content, pageable, total);
    }

    public RestPageResponse(Page<T> page) {
        super(page.getContent(), page.getPageable(), page.getTotalElements());
    }

    @Override
    @JsonIgnore
    public Pageable getPageable() {
        return super.getPageable();
    }

    @Override
    @JsonIgnore
    public Sort getSort() {
        return super.getSort();
    }
}
