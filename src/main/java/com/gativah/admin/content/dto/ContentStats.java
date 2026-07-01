package com.gativah.admin.content.dto;

/** KPI band for the content browser. {@code flagged} = items with open reports. */
public record ContentStats(long posts, long comments, long stories, long removed, long flagged) {
}
