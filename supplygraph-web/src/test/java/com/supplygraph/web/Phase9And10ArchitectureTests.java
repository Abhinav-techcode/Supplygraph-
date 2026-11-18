package com.supplygraph.web;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.implement;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Architecture rules added for the Phase 9 (connectors, outbox) and Phase 10 (governed execution) boundaries. */
@AnalyzeClasses(packages = "com.supplygraph", importOptions = ImportOption.DoNotIncludeTests.class)
public class Phase9And10ArchitectureTests {

    @ArchTest
    public static final ArchRule application_layer_is_independent_of_adapters_and_web =
            noClasses().that().resideInAnyPackage("..application.connector..", "..application.outbox..", "..application.execution..")
                    .should().dependOnClassesThat().resideInAnyPackage("..adapter..", "..web..")
                    .because("application services depend on ports only; adapters plug in from the outside");

    @ArchTest
    public static final ArchRule connector_domain_has_no_transport_or_vendor_types =
            noClasses().that().resideInAnyPackage("..domain.model.connector..", "..domain.model.outbox..", "..domain.model.sync..", "..domain.model.execution..")
                    .should().dependOnClassesThat().resideInAnyPackage("java.net.http..", "javax.net..", "com.fasterxml..", "org.springframework..", "com.sun.net..")
                    .because("vendor/transport specifics must not leak into the domain");

    @ArchTest
    public static final ArchRule integration_adapters_do_not_touch_persistence =
            noClasses().that().resideInAPackage("..adapter.integration..")
                    .should().dependOnClassesThat().resideInAnyPackage("..adapter.persistence..", "jakarta.persistence..", "org.hibernate..", "org.springframework.jdbc..")
                    .because("connector and execution adapters reach state only through application services and ports");

    @ArchTest
    public static final ArchRule execution_adapters_cannot_bypass_application_services =
            noClasses().that(implement(com.supplygraph.application.execution.ExecutionAdapter.class))
                    .should().dependOnClassesThat(simpleNameEndingWith("RepositoryPort"))
                    .orShould().dependOnClassesThat().haveSimpleNameEndingWith("Service")
                    .because("an execution adapter only executes a request; workflow state transitions belong to the orchestration service");

    @ArchTest
    public static final ArchRule agents_cannot_reach_execution_aggregates_or_services =
            noClasses().that().resideInAPackage("..adapter.ai..")
                    .should().dependOnClassesThat().resideInAnyPackage("..application.execution..", "..domain.model.execution..", "..application.outbox..", "..application.connector..")
                    .because("bounded agents may only propose; they must never mutate execution state");

    @ArchTest
    public static final ArchRule advisory_service_is_read_only =
            noClasses().that().haveSimpleName("ExecutionAdvisoryService")
                    .should().dependOnClassesThat(describe("mutating execution services or write-capable repositories", (JavaClass c) ->
                            c.getSimpleName().matches("Execution(Orchestration|Approval|Reconciliation|Compensation|Acknowledgement)Service|ClosedLoopService|RecoveryPlanConversionService|ExecutionWorkflowRepositoryPort|ExecutionEvidenceRepositoryPort|OutcomeRepositoryPort|ExecutionPolicyRepositoryPort")))
                    .because("advisories are explanations; actionable proposals must pass through deterministic governed services");

    @ArchTest
    public static final ArchRule repository_ports_are_implemented_only_by_persistence_adapter =
            classes().that(describe("implement a repository port", (JavaClass c) -> !c.isInterface() && c.getAllRawInterfaces().stream().anyMatch(i -> i.getSimpleName().endsWith("RepositoryPort"))))
                    .should().resideInAnyPackage("..adapter.persistence..")
                    .because("persistence implementation stays behind ports");

    @ArchTest
    public static final ArchRule industry_modules_do_not_depend_on_web =
            noClasses().that().resideInAPackage("..industry..")
                    .should().dependOnClassesThat().resideInAPackage("..web..")
                    .because("industry packs are independent of the delivery layer");

    @ArchTest
    public static final ArchRule outbox_enqueue_is_only_called_from_application_or_web_layers =
            noClasses().that().resideInAnyPackage("..domain..", "..adapter.persistence..")
                    .should().dependOnClassesThat().haveFullyQualifiedName("com.supplygraph.application.outbox.OutboxService")
                    .because("only application services enqueue events, inside their own transaction");
}
