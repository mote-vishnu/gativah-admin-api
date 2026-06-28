package com.gativah.admin;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Gativah platform admin API — a service separate from pacegrit-service that
 * shares its Postgres. It reads the live platform tables and owns the admin_*
 * tables; side-effectful changes are delegated to pacegrit-service's internal
 * admin hooks so domain logic (entitlements, notifications, caches) runs.
 *
 * <p>Like pacegrit-service, the whole app runs in UTC so every timestamp is
 * stored/serialized in UTC regardless of host zone.
 */
@SpringBootApplication
public class AdminApiApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(AdminApiApplication.class, args);
    }
}
