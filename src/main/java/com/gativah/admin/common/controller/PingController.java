package com.gativah.admin.common.controller;

import com.gativah.admin.common.dto.PingResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public liveness probe — confirms the admin API is up without auth. */
@RestController
@RequestMapping("/api/v1")
public class PingController {

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("gativah-admin-api", "ok");
    }
}
