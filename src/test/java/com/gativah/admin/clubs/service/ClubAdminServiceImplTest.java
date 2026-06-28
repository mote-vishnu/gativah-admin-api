package com.gativah.admin.clubs.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.gativah.admin.clubs.query.ClubQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ClubAdminServiceImplTest {

    @Mock ClubQuery query;

    ClubAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClubAdminServiceImpl(query);
    }

    @Test
    void list_wraps_query_and_normalizes_filters() {
        service.list("run", List.of("public"), List.of("active"), Pageable.ofSize(20));
        verify(query).search(eq("%run%"), eq(List.of("PUBLIC")), eq(List.of(false)), any(Pageable.class));
    }

    @Test
    void list_passes_nulls_for_blanks() {
        service.list("  ", List.of(), List.of("weird"), Pageable.ofSize(20));
        verify(query).search(isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void detail_missing_is_404() {
        when(query.detail(404L)).thenReturn(null);
        assertThatThrownBy(() -> service.detail(404L)).isInstanceOf(ResponseStatusException.class);
    }
}
