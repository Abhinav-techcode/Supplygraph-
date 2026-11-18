package com.supplygraph.domain.model.collaboration;

public enum EightDStep {
    D1_TEAM_FORMATION("D1: Establish Cross-Functional Problem Solving Team"),
    D2_PROBLEM_DESCRIPTION("D2: Define the Problem using 5W2H"),
    D3_CONTAINMENT_ACTIONS("D3: Implement Interim Containment Actions (ICA)"),
    D4_ROOT_CAUSE_ANALYSIS("D4: Identify and Verify Root Cause (Ishikawa & 5 Whys)"),
    D5_CORRECTIVE_ACTIONS("D5: Formulate Permanent Corrective Actions (PCA)"),
    D6_IMPLEMENTATION_VALIDATION("D6: Implement and Validate PCA"),
    D7_PREVENT_RECURRENCE("D7: Modify Process & Control Plan to Prevent Recurrence"),
    D8_CONGRATULATE_TEAM("D8: Recognize Team and Formally Close 8D Incident");

    private final String description;

    EightDStep(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

