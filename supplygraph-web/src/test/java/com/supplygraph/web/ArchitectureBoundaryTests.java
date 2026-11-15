package com.supplygraph.web;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Architectural boundary verification using ArchUnit.
 * Enforces pure domain principles, hexagonal layer isolation, and acyclic modules.
 */
@AnalyzeClasses(packages = "com.supplygraph", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureBoundaryTests {

    @ArchTest
    public static final ArchRule domain_must_be_pure_java =
            classes().that().resideInAPackage("..domain..")
                    .should().onlyDependOnClassesThat().resideInAnyPackage(
                            "java..",
                            "com.supplygraph.domain.."
                    )
                    .because("The core domain module must remain pure Java with zero external framework dependencies");

    @ArchTest
    public static final ArchRule no_cycles_between_modules =
            slices().matching("com.supplygraph.(*)..")
                    .should().beFreeOfCycles()
                    .because("Circular package dependencies violate clean architecture");

    @ArchTest
    public static final ArchRule domain_must_not_import_spring =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
                    .because("Domain layer must not depend on Spring Framework");

    @ArchTest
    public static final ArchRule domain_must_not_import_jpa =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.hibernate..")
                    .because("Domain layer must not depend on JPA or Hibernate");

    @ArchTest
    public static final ArchRule domain_must_not_import_vaadin =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.vaadin..")
                    .because("Domain layer must not depend on Vaadin Flow");

    @ArchTest
    public static final ArchRule vaadin_restricted_to_web =
            noClasses().that().resideOutsideOfPackage("..web..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.vaadin..")
                    .because("Vaadin Flow components and annotations must reside exclusively in supplygraph-web");

    @ArchTest
    public static final ArchRule jpa_restricted_to_persistence =
            noClasses().that().resideOutsideOfPackage("..adapter.persistence..")
                    .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.hibernate..")
                    .because("JPA and Hibernate entities and annotations must reside exclusively in supplygraph-adapter-persistence");
}
