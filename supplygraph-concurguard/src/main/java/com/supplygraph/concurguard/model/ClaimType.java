package com.supplygraph.concurguard.model;

/**
 * Nature of the claim exerted over a physical supply chain resource.
 */
public enum ClaimType {
    EXCLUSIVE,    // Exclusive usage of machine, line, or lot during the window
    SHARED,       // Rate-shared usage of capacity (e.g. 20 wafers out of 100/hr)
    CONSUMPTIVE   // Decrements available physical inventory permanently
}
