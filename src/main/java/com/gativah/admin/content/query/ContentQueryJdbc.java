package com.gativah.admin.content.query;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.gativah.admin.content.dto.ActivityShare;
import com.gativah.admin.content.dto.ContentCommentRow;
import com.gativah.admin.content.dto.ContentDetail;
import com.gativah.admin.content.dto.ContentReportRef;
import com.gativah.admin.content.dto.GeoPoint;
import com.gativah.admin.content.dto.ContentRow;
import com.gativah.admin.content.dto.ContentStats;
import com.gativah.admin.content.dto.MediaItem;
import com.gativah.admin.content.dto.ReactionCount;
import com.gativah.admin.content.dto.StoryRow;

import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Value;
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
                       p.created_at, (p.deleted_at is not null) as removed,
                       (select pas.activity_type from post_activity_share pas where pas.post_id = p.id) as activity_type
                from post p
                union all
                select c.id, 'COMMENT' as ctype, c.author_user_id, left(c.content, 200) as snippet,
                       c.created_at, (c.deleted_at is not null) as removed, null as activity_type
                from post_comment c
            ) u
            left join user_account au on au.id = u.author_user_id
            """;

    // Report statuses that still need moderator attention (a piece of content is "flagged").
    private static final String OPEN_STATUSES = "('PENDING','REVIEWING')";
    // Correlated count of moderation reports against the union row (matched by type + id).
    private static final String OPEN_REPORTS =
            "(select count(*) from content_report cr where cr.content_type = u.ctype and cr.content_id = u.id "
            + "and cr.status in " + OPEN_STATUSES + ")";
    private static final String TOTAL_REPORTS =
            "(select count(*) from content_report cr where cr.content_type = u.ctype and cr.content_id = u.id)";

    // The q-clause is static (cast for type inference); type + removed multi-value
    // filters are appended as in-clauses only when values are supplied.
    private static final String FILTER = """
            where (cast(:q as varchar) is null or u.snippet ilike :q or au.username ilike :q)
            """;

    private final NamedParameterJdbcTemplate jdbc;
    /** Browser-reachable base for uploaded media (pacegrit serves /uploads/** as public static). */
    private final String mediaBaseUrl;

    public ContentQueryJdbc(NamedParameterJdbcTemplate jdbc,
                            @Value("${pacegrit.media.base-url:http://localhost:8081}") String mediaBaseUrl) {
        this.jdbc = jdbc;
        this.mediaBaseUrl = mediaBaseUrl;
    }

    @Override
    public Page<ContentRow> search(List<String> types, String q, List<Boolean> removed,
                                   boolean reportedOnly, Pageable pageable) {
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
        if (reportedOnly) {
            filter = filter + " and " + OPEN_REPORTS + " > 0";
        }

        Long total = jdbc.queryForObject("select count(*) " + UNION + filter, params, Long.class);
        long count = total == null ? 0 : total;
        if (count == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        String sql = "select u.id, u.ctype, u.author_user_id, au.username as author_username, "
                + "u.snippet, u.created_at, u.removed, u.activity_type, " + OPEN_REPORTS + " as open_reports, "
                + TOTAL_REPORTS + " as total_reports "
                + UNION + filter
                + " order by " + OPEN_REPORTS + " desc, u.created_at desc limit :limit offset :offset";
        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());

        List<ContentRow> rows = jdbc.query(sql, params, (rs, i) -> new ContentRow(
                rs.getLong("id"),
                rs.getString("ctype"),
                (Long) rs.getObject("author_user_id"),
                rs.getString("author_username"),
                rs.getString("snippet"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getBoolean("removed"),
                rs.getString("activity_type"),
                rs.getLong("open_reports"),
                rs.getLong("total_reports")));
        return new PageImpl<>(rows, pageable, count);
    }

    @Override
    public ContentStats stats() {
        String sql = """
                select
                  (select count(*) from post) as posts,
                  (select count(*) from post_comment) as comments,
                  (select count(*) from story where deleted_at is null) as stories,
                  (select count(*) from post where deleted_at is not null)
                    + (select count(*) from post_comment where deleted_at is not null) as removed,
                  (select count(distinct (cr.content_type, cr.content_id)) from content_report cr
                     where cr.status in """ + OPEN_STATUSES + ") as flagged";
        return jdbc.queryForObject(sql, new MapSqlParameterSource(), (rs, i) -> new ContentStats(
                rs.getLong("posts"), rs.getLong("comments"), rs.getLong("stories"),
                rs.getLong("removed"), rs.getLong("flagged")));
    }

    @Override
    public List<ContentReportRef> reportsFor(String type, Long id) {
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("type", type).addValue("id", id);
        String sql = "select cr.id, cr.reason, cr.reporter_user_id, ru.username as reporter_username, "
                + "cr.status, cr.created_at "
                + "from content_report cr left join user_account ru on ru.id = cr.reporter_user_id "
                + "where cr.content_type = :type and cr.content_id = :id order by cr.created_at desc";
        return jdbc.query(sql, p, (rs, i) -> new ContentReportRef(
                rs.getLong("id"), rs.getString("reason"),
                (Long) rs.getObject("reporter_user_id"), rs.getString("reporter_username"),
                rs.getString("status"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()));
    }

    @Override
    public ContentDetail contentDetail(String type, Long id) {
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("id", id);
        return "COMMENT".equals(type) ? commentDetail(p) : postDetail(p);
    }

    private ContentDetail postDetail(MapSqlParameterSource p) {
        String core = "select p.id, p.author_user_id, au.username, p.content, p.kind, p.privacy, p.view_count, "
                + "p.created_at, (p.deleted_at is not null) as removed "
                + "from post p left join user_account au on au.id = p.author_user_id where p.id = :id";
        List<ContentDetail> found = jdbc.query(core, p, (rs, i) -> new ContentDetail(
                "POST", rs.getLong("id"), (Long) rs.getObject("author_user_id"), rs.getString("username"),
                rs.getString("content"), tsl(rs, "created_at"), rs.getBoolean("removed"),
                rs.getString("kind"), rs.getString("privacy"), rs.getLong("view_count"),
                null, null, 0, List.of(), List.of(), 0, List.of(), null));
        if (found.isEmpty()) {
            return null;
        }
        ContentDetail e = found.get(0);

        List<ReactionCount> reactions = jdbc.query(
                "select reaction_type as t, count(*) as c from post_reaction where post_id = :id group by reaction_type order by c desc",
                p, (rs, i) -> new ReactionCount(rs.getString("t"), rs.getLong("c")));
        long reactionTotal = reactions.stream().mapToLong(ReactionCount::count).sum();

        // Media URLs come in two stored shapes: a legacy '/api/v1/files/{fileId}/download' pointer
        // (resolve via uploaded_file to the real bytes path) and a direct '/uploads/...' path. Either
        // way, return a browser-reachable absolute URL against the public media host.
        p.addValue("mbase", mediaBaseUrl);
        List<MediaItem> media = jdbc.query(
                "select pm.media_type, "
                        + resolveUrl("pm.media_url", "uf") + " as url, "
                        + resolveUrl("pm.thumbnail_url", "tuf") + " as thumbnail_url "
                        + "from post_media pm "
                        + "left join uploaded_file uf on uf.id = cast(substring(pm.media_url from '/files/([0-9]+)') as bigint) "
                        + "left join uploaded_file tuf on tuf.id = cast(substring(pm.thumbnail_url from '/files/([0-9]+)') as bigint) "
                        + "where pm.post_id = :id order by pm.display_order",
                p, (rs, i) -> new MediaItem(rs.getString("media_type"), rs.getString("url"), rs.getString("thumbnail_url")));

        Long commentCount = jdbc.queryForObject("select count(*) from post_comment where post_id = :id", p, Long.class);
        List<ContentCommentRow> comments = jdbc.query(
                "select c.id, c.author_user_id, cu.username, c.content, c.created_at, (c.deleted_at is not null) as removed "
                        + "from post_comment c left join user_account cu on cu.id = c.author_user_id "
                        + "where c.post_id = :id order by c.created_at asc limit 100",
                p, (rs, i) -> new ContentCommentRow(rs.getLong("id"), (Long) rs.getObject("author_user_id"),
                        rs.getString("username"), rs.getString("content"), tsl(rs, "created_at"), rs.getBoolean("removed")));

        return new ContentDetail("POST", e.id(), e.authorUserId(), e.authorUsername(), e.content(), e.createdAt(),
                e.removed(), e.kind(), e.privacy(), e.viewCount(), null, null, reactionTotal, reactions, media,
                commentCount == null ? 0 : commentCount, comments, activityShare(p));
    }

    /** The activity summary + downsampled route for an activity post, or null if none. */
    private ActivityShare activityShare(MapSqlParameterSource p) {
        List<ActivityShare> found = jdbc.query(
                "select activity_type, distance_km, duration_secs, pace_min_per_km, calories_burned "
                        + "from post_activity_share where post_id = :id",
                p, (rs, i) -> new ActivityShare(rs.getString("activity_type"), rs.getBigDecimal("distance_km"),
                        (Integer) rs.getObject("duration_secs"), rs.getBigDecimal("pace_min_per_km"),
                        (Integer) rs.getObject("calories_burned"), List.of()));
        if (found.isEmpty()) {
            return null;
        }
        ActivityShare a = found.get(0);
        List<GeoPoint> route = jdbc.query(
                "select latitude, longitude from activity_coordinate "
                        + "where activity_id = (select activity_id from post where id = :id) order by sequence_no",
                p, (rs, i) -> new GeoPoint(rs.getDouble("latitude"), rs.getDouble("longitude")));
        return new ActivityShare(a.activityType(), a.distanceKm(), a.durationSecs(), a.paceMinPerKm(),
                a.caloriesBurned(), downsample(route, 150));
    }

    /** Thin a route to at most {@code max} points (keeps the last), for a lightweight sketch. */
    private static List<GeoPoint> downsample(List<GeoPoint> pts, int max) {
        if (pts.size() <= max) {
            return pts;
        }
        int step = (int) Math.ceil((double) pts.size() / max);
        List<GeoPoint> out = new ArrayList<>();
        for (int i = 0; i < pts.size(); i += step) {
            out.add(pts.get(i));
        }
        GeoPoint last = pts.get(pts.size() - 1);
        if (out.get(out.size() - 1) != last) {
            out.add(last);
        }
        return out;
    }

    private ContentDetail commentDetail(MapSqlParameterSource p) {
        String core = "select c.id, c.author_user_id, cu.username, c.content, c.created_at, "
                + "(c.deleted_at is not null) as removed, c.post_id, left(pp.content, 160) as parent_snippet "
                + "from post_comment c left join user_account cu on cu.id = c.author_user_id "
                + "left join post pp on pp.id = c.post_id where c.id = :id";
        List<ContentDetail> found = jdbc.query(core, p, (rs, i) -> new ContentDetail(
                "COMMENT", rs.getLong("id"), (Long) rs.getObject("author_user_id"), rs.getString("username"),
                rs.getString("content"), tsl(rs, "created_at"), rs.getBoolean("removed"),
                null, null, 0, (Long) rs.getObject("post_id"), rs.getString("parent_snippet"),
                0, List.of(), List.of(), 0, List.of(), null));
        if (found.isEmpty()) {
            return null;
        }
        ContentDetail e = found.get(0);

        List<ReactionCount> reactions = jdbc.query(
                "select emoji as t, count(*) as c from post_comment_reaction where comment_id = :id group by emoji order by c desc",
                p, (rs, i) -> new ReactionCount(rs.getString("t"), rs.getLong("c")));
        long reactionTotal = reactions.stream().mapToLong(ReactionCount::count).sum();

        return new ContentDetail("COMMENT", e.id(), e.authorUserId(), e.authorUsername(), e.content(), e.createdAt(),
                e.removed(), null, null, 0, e.parentPostId(), e.parentSnippet(), reactionTotal, reactions,
                List.of(), 0, List.of(), null);
    }

    private static LocalDateTime tsl(ResultSet rs, String col) throws SQLException {
        return rs.getTimestamp(col) == null ? null : rs.getTimestamp(col).toLocalDateTime();
    }

    /**
     * SQL CASE that turns a stored media URL into a browser-reachable absolute URL:
     * a matched uploaded_file row → its real /uploads path; an absolute URL → as-is;
     * any other relative path → prefixed with the public media host.
     */
    private static String resolveUrl(String col, String fileAlias) {
        return "case"
                + " when " + fileAlias + ".id is not null then :mbase || '/uploads/' || " + fileAlias + ".uploaded_by || '/' || "
                + fileAlias + ".category || '/' || " + fileAlias + ".stored_name"
                + " when " + col + " like 'http%' then " + col
                + " when " + col + " like '/%' then :mbase || " + col
                + " else " + col + " end";
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
