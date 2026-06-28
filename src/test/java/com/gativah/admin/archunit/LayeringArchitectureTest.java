package com.gativah.admin.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

/**
 * Structural invariants for the admin API, mirroring pacegrit-service:
 * constructor injection only, REST controllers in a {@code ..controller..}
 * package. Failing rules fail the build.
 */
@AnalyzeClasses(packages = "com.gativah.admin", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeringArchitectureTest {

    @ArchTest
    static final ArchRule no_field_injection = noFields()
            .should().beAnnotatedWith(Autowired.class)
            .as("Use constructor injection, never @Autowired fields");

    @ArchTest
    static final ArchRule controllers_are_rest_controllers = classes()
            .that().haveSimpleNameEndingWith("Controller")
            .should().beAnnotatedWith(RestController.class)
            .as("*Controller types must be @RestController");

    @ArchTest
    static final ArchRule controllers_live_in_controller_packages = classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage("..controller..")
            .as("@RestController types belong in a ..controller.. package");
}
