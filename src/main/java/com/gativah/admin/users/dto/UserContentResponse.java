package com.gativah.admin.users.dto;

import java.util.List;

/** A user's recent content for the profile Content tab. */
public record UserContentResponse(List<UserContentRow> items) {
}
