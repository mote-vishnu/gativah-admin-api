package com.gativah.admin.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The admin platform is single-tenant, like pacegrit-service. No tenant/member
 * scoping, no Hibernate filters, no imports from the legacy hyrox-api packages.
 */
@AnalyzeClasses(packages = "com.gativah.admin", importOptions = ImportOption.DoNotIncludeTests.class)
public class TenantFreeArchitectureTest {

    @ArchTest
    static final ArchRule no_imports_from_hyrox_api =
            noClasses().should().dependOnClassesThat().resideInAPackage("com.pacegrid..")
                    .as("admin API must not import from hyrox-api's com.pacegrid.* packages");

    @ArchTest
    static final ArchRule no_TenantContext_class =
            noClasses().should().haveSimpleName("TenantContext")
                    .as("TenantContext must not exist — the platform is tenant-free");

    @ArchTest
    static final ArchRule no_TenantAware_class =
            noClasses().should().haveSimpleName("TenantAware")
                    .as("TenantAware must not exist");

    @ArchTest
    static final ArchRule no_tenant_id_field =
            noFields().should().haveNameMatching("(?i)tenant[_]?id")
                    .as("Entities must not carry a tenant_id field");

    @ArchTest
    static final ArchRule no_member_id_field =
            noFields().should().haveNameMatching("(?i)member[_]?id")
                    .as("Entities must not carry a member_id field");

    @ArchTest
    static final ArchRule no_hibernate_filter_annotations =
            noClasses().should().beAnnotatedWith("org.hibernate.annotations.Filter")
                    .as("@Filter is forbidden — it was the vehicle for tenant scoping in hyrox-api");

    @ArchTest
    static final ArchRule no_hibernate_filterdef_annotations =
            noClasses().should().beAnnotatedWith("org.hibernate.annotations.FilterDef")
                    .as("@FilterDef is forbidden");

    @ArchTest
    static final ArchRule no_runWithoutTenant_methods =
            noMethods().should().haveNameMatching("runWithoutTenant|runWithTenant")
                    .as("runWithoutTenant / runWithTenant must not exist");
}
