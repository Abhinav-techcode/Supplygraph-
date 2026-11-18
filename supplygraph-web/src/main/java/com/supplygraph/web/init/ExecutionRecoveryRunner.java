package com.supplygraph.web.init;

import com.supplygraph.application.execution.ExecutionOrchestrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** On startup, tasks left IN_PROGRESS by a crash are marked RECONCILIATION_REQUIRED (unknown outcome). Nothing is re-sent. */
@Component
public class ExecutionRecoveryRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ExecutionRecoveryRunner.class);
    private final ExecutionOrchestrationService orchestration;

    public ExecutionRecoveryRunner(ExecutionOrchestrationService orchestration) { this.orchestration = orchestration; }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int n = orchestration.recoverInterrupted();
            if (n > 0) log.warn("Recovered {} interrupted execution task(s); they now require reconciliation", n);
        } catch (RuntimeException e) {
            log.error("Execution recovery could not run: {}", e.getClass().getSimpleName());
        }
    }
}
