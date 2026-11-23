package com.supplygraph.application.planning;

import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.domain.model.planning.TransportMode;
import com.supplygraph.domain.model.planning.TransportModeTradeoff;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class SupplyChainNetworkPlanningService {

    public List<TransportModeTradeoff> evaluateTransportTradeoffs(double distanceKm, double shipmentWeightTons) {
        if (distanceKm <= 0 || shipmentWeightTons <= 0) {
            throw new ValidationException("Distance and shipment weight must be positive");
        }

        List<TransportModeTradeoff> results = new ArrayList<>();

        for (TransportMode mode : TransportMode.values()) {
            double baseRatePerTonKm = switch (mode) {
                case ROAD -> 0.12;
                case RAIL -> 0.05;
                case OCEAN -> 0.02;
                case AIR -> 0.85;
            };

            double costValue = baseRatePerTonKm * distanceKm * shipmentWeightTons;
            BigDecimal freightCost = BigDecimal.valueOf(costValue).setScale(2, RoundingMode.HALF_UP);

            double speedKmPerDay = switch (mode) {
                case ROAD -> 650.0;
                case RAIL -> 400.0;
                case OCEAN -> 500.0;
                case AIR -> 8000.0;
            };
            int transitDays = Math.max(1, (int) Math.ceil(distanceKm / speedKmPerDay));
            if (mode == TransportMode.OCEAN) {
                transitDays += 4; // Port handling & customs buffer
            } else if (mode == TransportMode.AIR) {
                transitDays += 1; // Airport staging & customs
            }

            double tonKm = distanceKm * shipmentWeightTons;
            double co2Kg = (tonKm * mode.getGramsCo2PerTonKm()) / 1000.0;

            double reliability = switch (mode) {
                case ROAD -> 94.5;
                case RAIL -> 88.0;
                case OCEAN -> 82.5;
                case AIR -> 97.8;
            };

            results.add(new TransportModeTradeoff(mode, freightCost, transitDays, co2Kg, reliability));
        }

        return results;
    }
}

