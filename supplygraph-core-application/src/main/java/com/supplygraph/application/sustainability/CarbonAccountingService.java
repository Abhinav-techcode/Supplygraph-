package com.supplygraph.application.sustainability;

import com.supplygraph.application.sustainability.port.out.CarbonEmissionRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.planning.TransportMode;
import com.supplygraph.domain.model.sustainability.CarbonEmissionRecord;
import com.supplygraph.domain.model.sustainability.CarbonEmissionRecordId;
import com.supplygraph.domain.model.sustainability.EmissionScope;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class CarbonAccountingService {

    private final CarbonEmissionRepositoryPort carbonRepository;

    public CarbonAccountingService(CarbonEmissionRepositoryPort carbonRepository) {
        this.carbonRepository = carbonRepository;
    }

    public CarbonEmissionRecord recordScope1Combustion(
            WorkspaceId workspaceId,
            NodeId facilityId,
            String fuelType,
            double fuelQuantity,
            String uom,
            double emissionFactorKgPerUnit
    ) {
        double totalKg = fuelQuantity * emissionFactorKgPerUnit;

        CarbonEmissionRecord record = new CarbonEmissionRecord(
                CarbonEmissionRecordId.generate(),
                workspaceId,
                facilityId,
                EmissionScope.SCOPE_1_DIRECT,
                "Combustion: " + fuelType,
                fuelQuantity,
                uom,
                emissionFactorKgPerUnit,
                totalKg,
                Instant.now()
        );

        carbonRepository.save(record);
        return record;
    }

    public CarbonEmissionRecord recordScope2Electricity(
            WorkspaceId workspaceId,
            NodeId facilityId,
            double kwhConsumed,
            double gridEmissionFactorKgPerKwh
    ) {
        double totalKg = kwhConsumed * gridEmissionFactorKgPerKwh;

        CarbonEmissionRecord record = new CarbonEmissionRecord(
                CarbonEmissionRecordId.generate(),
                workspaceId,
                facilityId,
                EmissionScope.SCOPE_2_INDIRECT_ELECTRICITY,
                "Purchased Grid Electricity",
                kwhConsumed,
                "kWh",
                gridEmissionFactorKgPerKwh,
                totalKg,
                Instant.now()
        );

        carbonRepository.save(record);
        return record;
    }

    public CarbonEmissionRecord recordScope3Freight(
            WorkspaceId workspaceId,
            NodeId laneId,
            TransportMode mode,
            double distanceKm,
            double weightTons
    ) {
        double tonKm = distanceKm * weightTons;
        double factorKgPerTonKm = mode.getGramsCo2PerTonKm() / 1000.0;
        double totalKg = tonKm * factorKgPerTonKm;

        CarbonEmissionRecord record = new CarbonEmissionRecord(
                CarbonEmissionRecordId.generate(),
                workspaceId,
                laneId,
                EmissionScope.SCOPE_3_UPSTREAM_FREIGHT,
                "Freight: " + mode.name(),
                tonKm,
                "ton-km",
                factorKgPerTonKm,
                totalKg,
                Instant.now()
        );

        carbonRepository.save(record);
        return record;
    }

    public Map<EmissionScope, Double> getEmissionsByScope(WorkspaceId workspaceId) {
        List<CarbonEmissionRecord> records = carbonRepository.findByWorkspaceId(workspaceId);
        Map<EmissionScope, Double> scopeMap = new EnumMap<>(EmissionScope.class);

        for (EmissionScope scope : EmissionScope.values()) {
            scopeMap.put(scope, 0.0);
        }

        for (CarbonEmissionRecord r : records) {
            scopeMap.merge(r.scope(), r.getTotalEmissionsTons(), Double::sum);
        }

        return Collections.unmodifiableMap(scopeMap);
    }

    public List<CarbonEmissionRecord> listRecords(WorkspaceId workspaceId) {
        return carbonRepository.findByWorkspaceId(workspaceId);
    }
}

