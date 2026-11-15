package com.supplygraph.concurguard.conflict;

import com.supplygraph.concurguard.api.ConflictType;
import com.supplygraph.concurguard.model.ActionIntent;
import com.supplygraph.concurguard.model.ClaimType;
import com.supplygraph.concurguard.model.ResourceClaim;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.inventory.BalanceCategory;
import com.supplygraph.domain.model.inventory.InventoryAccountId;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;

import java.util.*;

/**
 * 1. Quantity Conflict Detector:
 * Detects when aggregate consumptive claims across intents exceed available inventory in snapshot.
 */
public class QuantityConflictDetector {

    public List<Conflict> detect(Collection<ActionIntent> intents, DigitalTwinSnapshot snapshot) {
        List<Conflict> conflicts = new ArrayList<>();
        Map<InventoryAccountId, List<ActionIntent>> claimsByAccount = new LinkedHashMap<>();
        Map<InventoryAccountId, Quantity> totalClaimedByAccount = new LinkedHashMap<>();

        for (ActionIntent intent : intents) {
            for (ResourceClaim claim : intent.resourceClaims()) {
                if (claim.claimType() == ClaimType.CONSUMPTIVE) {
                    InventoryAccountId accountId = InventoryAccountId.of(
                            NodeId.of(claim.resourceIdentity().facilityId()),
                            MaterialId.of(claim.resourceIdentity().canonicalId()),
                            BalanceCategory.ON_HAND_AVAILABLE
                    );

                    claimsByAccount.computeIfAbsent(accountId, k -> new ArrayList<>()).add(intent);
                    totalClaimedByAccount.merge(accountId, claim.claimedQuantity(), Quantity::add);
                }
            }
        }

        for (Map.Entry<InventoryAccountId, Quantity> entry : totalClaimedByAccount.entrySet()) {
            InventoryAccountId account = entry.getKey();
            Quantity totalClaimed = entry.getValue();

            Quantity available = snapshot.inventoryBalances().getOrDefault(account, Quantity.zero(totalClaimed.unit()));

            if (totalClaimed.compareTo(available) > 0) {
                List<ActionIntent> competingIntents = claimsByAccount.get(account);
                for (int i = 0; i < competingIntents.size(); i++) {
                    ActionIntent primary = competingIntents.get(i);
                    ActionIntent secondary = (i + 1 < competingIntents.size()) ? competingIntents.get(i + 1) : competingIntents.get(0);

                    conflicts.add(Conflict.of(
                            ConflictType.QUANTITY_CONFLICT,
                            primary.id(),
                            primary.id().equals(secondary.id()) ? null : secondary.id(),
                            account.toString(),
                            "Aggregate inventory claim (" + totalClaimed + ") exceeds available balance (" + available + ") for " + account,
                            Severity.CRITICAL
                    ));
                }
            }
        }

        return conflicts;
    }
}
