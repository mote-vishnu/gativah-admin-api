package com.gativah.admin.staff.service;

import java.util.List;

import com.gativah.admin.staff.dto.AdminLite;
import com.gativah.admin.staff.dto.InviteStaffRequest;
import com.gativah.admin.staff.dto.SessionRow;
import com.gativah.admin.staff.dto.StaffRow;
import com.gativah.admin.staff.dto.UpdateStaffRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StaffService {

    Page<StaffRow> list(Pageable pageable);

    /** All admins as id+name, for resolving actor ids to names across the console. */
    List<AdminLite> directory();

    StaffRow invite(Long actorAdminId, InviteStaffRequest req);

    StaffRow update(Long actorAdminId, Long id, UpdateStaffRequest req);

    /** Clear an operator's MFA enrollment and force re-login (admin reset). */
    StaffRow resetMfa(Long actorAdminId, Long id);

    /** Active + recent sessions (issued tokens) for an operator. */
    List<SessionRow> sessions(Long adminUserId);

    /** Revoke a single session — its token is rejected on the next request. */
    void revokeSession(Long actorAdminId, Long adminUserId, Long sessionId);

    StaffRow setRoles(Long actorAdminId, Long id, List<Long> roleIds);
}
