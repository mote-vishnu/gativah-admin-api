package com.gativah.admin.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import java.util.Map;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import org.springframework.web.bind.annotation.RestController;

/**
 * Layering + structural invariants for the admin API, mirroring
 * pacegrit-service: controllers → services → repos, constructor injection only,
 * package ↔ suffix alignment, and no Map fields on DTOs.
 */
@AnalyzeClasses(packages = "com.gativah.admin", importOptions = ImportOption.DoNotIncludeTests.class)
public class LayeringArchitectureTest {

    @ArchTest
    static final ArchRule controllers_end_with_Controller =
            classes().that().resideInAPackage("..controller..")
                    .should().haveSimpleNameEndingWith("Controller")
                    .as("Anything in a *.controller package must be named *Controller");

    @ArchTest
    static final ArchRule controllers_are_rest_controllers =
            classes().that().haveSimpleNameEndingWith("Controller")
                    .should().beAnnotatedWith(RestController.class)
                    .as("*Controller types must be @RestController");

    @ArchTest
    static final ArchRule rest_controllers_live_in_controller_packages =
            classes().that().areAnnotatedWith(RestController.class)
                    .should().resideInAPackage("..controller..")
                    .as("@RestController types belong in a ..controller.. package");

    @ArchTest
    static final ArchRule repositories_end_with_Repository =
            classes().that().resideInAPackage("..repo..")
                    .should().haveSimpleNameEndingWith("Repository")
                    .as("Anything in a *.repo package must be named *Repository");

    @ArchTest
    static final ArchRule controllers_dont_touch_repositories =
            noClasses().that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat().resideInAPackage("..repo..")
                    .as("Controllers must go through services, not call repositories directly");

    @ArchTest
    static final ArchRule no_autowired_field_injection =
            noFields().should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                    .as("Constructor injection only — no @Autowired on fields");

    @ArchTest
    static final ArchRule no_map_fields_on_dto_records =
            fields().that().areDeclaredInClassesThat().resideInAPackage("..dto..")
                    .should().notHaveRawType(Map.class)
                    .as("DTOs in ..dto.. must not contain Map fields — use List<{key,value}> instead");

    @ArchTest
    static final ArchRule no_object_array_returns_from_repositories =
            methods().that().areDeclaredInClassesThat().resideInAPackage("..repo..")
                    .should().notHaveRawReturnType(Object[].class)
                    .as("Repository methods must not return Object[] — use a concrete projection record");

    @ArchTest
    static final ArchRule no_hibernate_filter_imports =
            noClasses().should()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("org.hibernate.annotations.Filter")
                    .as("admin API is single-tenant — no @Filter / tenant plumbing allowed");
}
