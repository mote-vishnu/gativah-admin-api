package com.gativah.admin.content.query;

import java.util.List;

import com.gativah.admin.content.dto.ContentRow;
import com.gativah.admin.content.dto.StoryRow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Native-SQL content browser: posts + comments unioned, joined to their author. */
@Repository
public class ContentQueryJdbc implements ContentQuery {

    private static final String UNION = """
            from (
                select p.id, 'POST' as ctype, p.author_user_id, left(p.content, 200) as snippet,
                       p.created_at, (p.deleted_at is not null) as removed
                from post p
                union all
                select c.id, 'COMMENT' as ctype, c.author_user_id, left(c.content, 200) as snippet,
                       c.created_at, (c.deleted_at is not null) as removed
                from post_comment c
            ) u
            left join user_account au on au.id = u.author_user_id
            """;

    // The q-clause is static (cast for type inference); type + removed multi-value
    // filters are appended as in-clauses only when values are supplied.
    private static final String FILTER = """
            where (cast(:q as varchar) is null or u.snippet ilike :q or au.username ilike :q)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ContentQueryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Page<ContentRow> search(List<String> types, String q, List<Boolean> removed, Pageable pageable) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("q", q);
        String filter = FILTER;
        if (types != null && !types.isEmpty()) {
            filter = filter + " and u.ctype in (:types)";
            params.addValue("types", types);
        }
        if (removed != null && !removed.isEmpty()) {
            filter = filter + " and u.removed in (:removed)";
            params.addValue("removed", removed);
        }

        Long total = jdbc.queryForObject("select count(*) " + UNION + filter, params, Long.class);
        long count = total == null ? 0 : total;
        if (count == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        String sql = "select u.id, u.ctype, u.author_user_id, au.username as author_username, "
                + "u.snippet, u.created_at, u.removed "
                + UNION + filter
                + " order by u.created_at desc limit :limit offset :offset";
        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());

        List<ContentRow> rows = jdbc.query(sql, params, (rs, i) -> new ContentRow(
                rs.getLong("id"),
                rs.getString("ctype"),
                (Long) rs.getObject("author_user_id"),
                rs.getString("author_username"),
                rs.getString("snippet"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getBoolean("removed")));
        return new PageImpl<>(rows, pageable, count);
    }

    @Override
    public Page<StoryRow> stories(String q, Pageable pageable) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("q", q);
        String filter = "where (cast(:q as varchar) is null or s.content ilike :q or au.username ilike :q)";

        Long total = jdbc.queryForObject(
                "select count(*) from story s left join user_account au on au.id = s.author_user_id " + filter,
                params, Long.class);
        long count = total == null ? 0 : total;
        if (count == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        String sql = "select s.id, s.author_user_id, au.username, s.kind, left(s.content, 160) as snippet, "
                + "s.created_at, s.expires_at, (s.deleted_at is not null) as removed, "
                + "(select count(*) from story_view v where v.story_id = s.id) as views, "
                + "(select count(*) from story_reaction r where r.story_id = s.id) as reactions "
                + "from story s left join user_account au on au.id = s.author_user_id " + filter
                + " order by s.created_at desc limit :limit offset :offset";
        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());

        List<StoryRow> rows = jdbc.query(sql, params, (rs, i) -> new StoryRow(
                rs.getLong("id"),
                (Long) rs.getObject("author_user_id"),
                rs.getString("username"),
                rs.getString("kind"),
                rs.getString("snippet"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").toLocalDateTime(),
                rs.getLong("views"),
                rs.getLong("reactions"),
                rs.getBoolean("removed")));
        return new PageImpl<>(rows, pageable, count);
    }
}
