package com.gativah.admin.legal.query;

import java.util.List;

import com.gativah.admin.legal.dto.DisclosureRegisterRow;
import com.gativah.admin.legal.dto.LegalRequestSummary;
import com.gativah.admin.legal.dto.LegalStats;
import com.gativah.admin.legal.dto.LegalTaskListRow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Native-SQL read side for the Legal & Disclosure dashboards. */
public interface LegalQuery {

    Page<LegalRequestSummary> searchRequests(String q, List<String> statuses, List<String> types,
                                             boolean overdueOnly, Pageable pageable);

    LegalStats stats();

    Page<LegalTaskListRow> openTasks(Pageable pageable);

    Page<DisclosureRegisterRow> disclosureRegister(Pageable pageable);
}
