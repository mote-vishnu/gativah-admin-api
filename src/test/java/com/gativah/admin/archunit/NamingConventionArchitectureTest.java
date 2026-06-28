package com.gativah.admin.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Naming conventions for the admin API (suffix → package), mirroring
 * pacegrit-service. The reverse (package → suffix) rules live in
 * {@link LayeringArchitectureTest}.
 */
@AnalyzeClasses(packages = "com.gativah.admin", importOptions = ImportOption.DoNotIncludeTests.class)
public class NamingConventionArchitectureTest {

    @ArchTest
    static final ArchRule repositories_reside_in_repo_package =
            classes().that().haveSimpleNameEndingWith("Repository")
                    .should().resideInAPackage("..repo..")
                    .as("A *Repository type must live in a ..repo.. package");

    @ArchTest
    static final ArchRule repositories_are_interfaces =
            classes().that().haveSimpleNameEndingWith("Repository")
                    .should().beInterfaces()
                    .as("Spring Data *Repository types must be interfaces");

    @ArchTest
    static final ArchRule service_impls_reside_in_service_package =
            classes().that().haveSimpleNameEndingWith("ServiceImpl")
                    .should().resideInAPackage("..service..")
                    .as("A *ServiceImpl must live in a ..service.. package");

    @ArchTest
    static final ArchRule service_impls_are_concrete_classes =
            classes().that().haveSimpleNameEndingWith("ServiceImpl")
                    .should().notBeInterfaces()
                    .as("A *ServiceImpl is an implementation — it must be a class, not an interface");

    @ArchTest
    static final ArchRule service_interfaces_reside_in_service_package =
            classes().that().areInterfaces().and().haveSimpleNameEndingWith("Service")
                    .should().resideInAPackage("..service..")
                    .as("A *Service interface must live in a ..service.. package");

    // Top-level only: nested private records inside clients/services (e.g. the
    // internal-API request shapes) are not wire DTOs and stay with their caller.
    @ArchTest
    static final ArchRule response_dtos_reside_in_dto_package =
            classes().that().haveSimpleNameEndingWith("Response").and().areTopLevelClasses()
                    .should().resideInAPackage("..dto..")
                    .as("A top-level *Response wire type must live in a ..dto.. package");

    @ArchTest
    static final ArchRule no_hungarian_interface_prefix =
            classes().that().areInterfaces()
                    .should().haveNameNotMatching(".*\\.I[A-Z].*")
                    .as("Interfaces must not use a Hungarian I-prefix (use FooService, not IFooService)");
}
