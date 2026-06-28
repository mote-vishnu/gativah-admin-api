package com.gativah.admin.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import java.util.List;

import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Controller endpoints never return raw {@code List<T>} (directly or wrapped in
 * {@code ResponseEntity<List<T>>}) — collection endpoints must return
 * {@code Page<T>} so the client can paginate. Mirrors pacegrit-service.
 */
@AnalyzeClasses(packages = "com.gativah.admin", importOptions = ImportOption.DoNotIncludeTests.class)
public class ApiPaginationArchitectureTest {

    @ArchTest
    static final ArchRule controller_endpoints_must_not_return_list =
            methods()
                    .that().areDeclaredInClassesThat().resideInAPackage("..controller..")
                    .and().arePublic()
                    .should(notReturnList())
                    .as("Controller methods must return Page<T>, not List<T> — "
                            + "collection endpoints must be paginated at the database level");

    private static ArchCondition<JavaMethod> notReturnList() {
        return new ArchCondition<>("not return List directly or inside ResponseEntity") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                JavaType returnType = method.getReturnType();
                if (returnType.toErasure().isEquivalentTo(List.class)) {
                    events.add(SimpleConditionEvent.violated(method,
                            method.getFullName() + " returns List<T> directly — use Page<T>"));
                    return;
                }
                if (returnType instanceof JavaParameterizedType paramType) {
                    for (JavaType arg : paramType.getActualTypeArguments()) {
                        if (arg.toErasure().isEquivalentTo(List.class)) {
                            events.add(SimpleConditionEvent.violated(method,
                                    method.getFullName()
                                            + " returns ResponseEntity<List<T>> — use Page<T>"));
                            return;
                        }
                    }
                }
            }
        };
    }
}
