package com.supplygraph.application.service;

import com.supplygraph.domain.error.ValidationException;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Builds a complete, internally consistent supply chain (facilities, lanes, stock, bill of materials) for users who have no data of their own.
 * Locations are real cities with real coordinates; distances, transit times and freight costs are computed from those coordinates with fixed,
 * published-style rates. Company names, stock levels and capacities are invented. Every output is labelled as generated, never as measured.
 * The same seed always yields the same network. The CSV it returns goes through {@link SupplyChainImportService}, so it is validated like any import.
 */
@Component
public class SyntheticNetworkGenerator {
    public enum Industry { AUTOMOTIVE, ELECTRONICS, PHARMACEUTICAL, AEROSPACE, FOOD_BEVERAGE, APPAREL, MEDICAL_DEVICES, SOLAR_ENERGY, CHEMICALS, HEAVY_MACHINERY }
    public enum Region { EUROPE, NORTH_AMERICA, ASIA, GLOBAL }
    public enum Size { SMALL(3, 1, 2, 1), MEDIUM(6, 2, 3, 2), LARGE(10, 3, 4, 3);
        final int suppliers, plants, hubs, warehouses;
        Size(int s, int p, int h, int w) { suppliers = s; plants = p; hubs = h; warehouses = w; }
    }

    public record Dataset(String facilities, String lanes, String stock, String bom, String summary) {}
    private record City(String name, String country, Region region, double lat, double lon) {}

    private static final List<City> CITIES = List.of(
            new City("Hamburg", "DE", Region.EUROPE, 53.55, 9.99), new City("Munich", "DE", Region.EUROPE, 48.14, 11.58), new City("Stuttgart", "DE", Region.EUROPE, 48.78, 9.18),
            new City("Rotterdam", "NL", Region.EUROPE, 51.92, 4.48), new City("Antwerp", "BE", Region.EUROPE, 51.22, 4.40), new City("Lyon", "FR", Region.EUROPE, 45.76, 4.84),
            new City("Barcelona", "ES", Region.EUROPE, 41.39, 2.17), new City("Milan", "IT", Region.EUROPE, 45.46, 9.19), new City("Gothenburg", "SE", Region.EUROPE, 57.71, 11.97),
            new City("Oslo", "NO", Region.EUROPE, 59.91, 10.75), new City("Gdansk", "PL", Region.EUROPE, 54.35, 18.65), new City("Prague", "CZ", Region.EUROPE, 50.08, 14.44),
            new City("Vienna", "AT", Region.EUROPE, 48.21, 16.37), new City("Budapest", "HU", Region.EUROPE, 47.50, 19.04), new City("Turin", "IT", Region.EUROPE, 45.07, 7.69),
            new City("Detroit", "US", Region.NORTH_AMERICA, 42.33, -83.05), new City("Chicago", "US", Region.NORTH_AMERICA, 41.88, -87.63), new City("Houston", "US", Region.NORTH_AMERICA, 29.76, -95.37),
            new City("Los Angeles", "US", Region.NORTH_AMERICA, 34.05, -118.24), new City("Atlanta", "US", Region.NORTH_AMERICA, 33.75, -84.39), new City("Toronto", "CA", Region.NORTH_AMERICA, 43.65, -79.38),
            new City("Monterrey", "MX", Region.NORTH_AMERICA, 25.69, -100.32), new City("Dallas", "US", Region.NORTH_AMERICA, 32.78, -96.80), new City("Seattle", "US", Region.NORTH_AMERICA, 47.61, -122.33),
            new City("Newark", "US", Region.NORTH_AMERICA, 40.74, -74.17), new City("Savannah", "US", Region.NORTH_AMERICA, 32.08, -81.09),
            new City("Shenzhen", "CN", Region.ASIA, 22.54, 114.06), new City("Shanghai", "CN", Region.ASIA, 31.23, 121.47), new City("Ningbo", "CN", Region.ASIA, 29.87, 121.55),
            new City("Tokyo", "JP", Region.ASIA, 35.68, 139.69), new City("Nagoya", "JP", Region.ASIA, 35.18, 136.91), new City("Busan", "KR", Region.ASIA, 35.18, 129.08),
            new City("Singapore", "SG", Region.ASIA, 1.35, 103.82), new City("Chennai", "IN", Region.ASIA, 13.08, 80.27), new City("Pune", "IN", Region.ASIA, 18.52, 73.86),
            new City("Bangkok", "TH", Region.ASIA, 13.76, 100.50), new City("Ho Chi Minh City", "VN", Region.ASIA, 10.82, 106.63), new City("Taipei", "TW", Region.ASIA, 25.03, 121.57),
            new City("Mumbai", "IN", Region.ASIA, 19.08, 72.88), new City("Guangzhou", "CN", Region.ASIA, 23.13, 113.26),
            new City("Osaka", "JP", Region.ASIA, 34.69, 135.50), new City("Kuala Lumpur", "MY", Region.ASIA, 3.14, 101.69), new City("Jakarta", "ID", Region.ASIA, -6.21, 106.85),
            new City("Chongqing", "CN", Region.ASIA, 29.56, 106.55), new City("Delhi", "IN", Region.ASIA, 28.61, 77.21), new City("Seoul", "KR", Region.ASIA, 37.57, 126.98),
            new City("Warsaw", "PL", Region.EUROPE, 52.23, 21.01), new City("Bremen", "DE", Region.EUROPE, 53.08, 8.80), new City("Valencia", "ES", Region.EUROPE, 39.47, -0.38),
            new City("Genoa", "IT", Region.EUROPE, 44.41, 8.93), new City("Le Havre", "FR", Region.EUROPE, 49.49, 0.11), new City("Bratislava", "SK", Region.EUROPE, 48.15, 17.11),
            new City("Cologne", "DE", Region.EUROPE, 50.94, 6.96),
            new City("Louisville", "US", Region.NORTH_AMERICA, 38.25, -85.76), new City("Memphis", "US", Region.NORTH_AMERICA, 35.15, -90.05), new City("Phoenix", "US", Region.NORTH_AMERICA, 33.45, -112.07),
            new City("Vancouver", "CA", Region.NORTH_AMERICA, 49.28, -123.12), new City("Guadalajara", "MX", Region.NORTH_AMERICA, 20.66, -103.35), new City("Charlotte", "US", Region.NORTH_AMERICA, 35.23, -80.84),
            new City("Kansas City", "US", Region.NORTH_AMERICA, 39.10, -94.58), new City("Montreal", "CA", Region.NORTH_AMERICA, 45.50, -73.57), new City("Boston", "US", Region.NORTH_AMERICA, 42.36, -71.06));

    private record Spec(String product, List<String> parts, List<String> supplierSuffix, String productLabel) {}
    private static Spec spec(Industry i) {
        return switch (i) {
            case AUTOMOTIVE -> new Spec("EV-DRIVETRAIN", List.of("STEEL-COIL", "BATTERY-CELL", "WIRING-HARNESS", "ECU-CHIP"), List.of("Metals", "Cells", "Wiring", "Semiconductors"), "electric drivetrains");
            case ELECTRONICS -> new Spec("SMARTPHONE", List.of("DISPLAY-PANEL", "SOC-CHIP", "BATTERY-CELL", "CASING"), List.of("Displays", "Microelectronics", "Cells", "Plastics"), "smartphones");
            case PHARMACEUTICAL -> new Spec("TABLET-PACK", List.of("API-POWDER", "EXCIPIENT", "BLISTER-FOIL", "CARTON"), List.of("Chemicals", "Excipients", "Packaging", "Print"), "tablet packs");
            case AEROSPACE -> new Spec("AIRCRAFT-WING", List.of("TITANIUM-FORGING", "CARBON-FIBRE", "AVIONICS-UNIT", "FASTENERS"), List.of("Forgings", "Composites", "Avionics", "Fasteners"), "aircraft wing assemblies");
            case FOOD_BEVERAGE -> new Spec("PACKAGED-MEAL", List.of("GRAIN", "DAIRY", "PACKAGING-FILM", "FLAVOURING"), List.of("Agriculture", "Dairy", "Packaging", "Ingredients"), "packaged meals");
            case APPAREL -> new Spec("JACKET", List.of("COTTON-FABRIC", "ZIPPER", "DYE", "LABEL-TRIM"), List.of("Textiles", "Hardware", "Dyeing", "Trims"), "jackets");
            case MEDICAL_DEVICES -> new Spec("INFUSION-PUMP", List.of("PRECISION-MOTOR", "MEDICAL-PLASTIC", "CIRCUIT-BOARD", "STERILE-PACK"), List.of("Motors", "Plastics", "Electronics", "Sterile Packaging"), "infusion pumps");
            case SOLAR_ENERGY -> new Spec("SOLAR-MODULE", List.of("POLYSILICON", "GLASS-SHEET", "ALUMINIUM-FRAME", "JUNCTION-BOX"), List.of("Silicon", "Glass", "Aluminium", "Electricals"), "solar modules");
            case CHEMICALS -> new Spec("SPECIALTY-POLYMER", List.of("ETHYLENE", "CATALYST", "ADDITIVE-PACK", "DRUM"), List.of("Feedstock", "Catalysts", "Additives", "Containers"), "specialty polymers");
            case HEAVY_MACHINERY -> new Spec("EXCAVATOR", List.of("STEEL-PLATE", "HYDRAULIC-PUMP", "ENGINE-BLOCK", "TRACK-LINK"), List.of("Steel", "Hydraulics", "Engines", "Undercarriage"), "excavators");
        };
    }

    public Dataset generate(Industry industry, Region region, Size size, long seed) { return generate(industry, region, size, seed, ""); }

    /** idPrefix (for example "AERO-") is put in front of every facility and material id so several industries can share one workspace without clashing. */
    public Dataset generate(Industry industry, Region region, Size size, long seed, String idPrefix) {
        final String px = idPrefix == null ? "" : idPrefix;
        if (industry == null || region == null || size == null) throw new ValidationException("Choose an industry, a region and a size.");
        int needed = size.suppliers + size.plants + size.hubs + size.warehouses;
        List<City> pool = new ArrayList<>(CITIES.stream().filter(c -> region == Region.GLOBAL || c.region() == region).toList());
        if (pool.size() < needed) throw new ValidationException("Region " + region + " has only " + pool.size() + " cities; this size needs " + needed + ". Pick GLOBAL or a smaller size.");
        Random rnd = new Random(seed);
        Collections.shuffle(pool, rnd);
        Spec spec0 = spec(industry);
        Spec spec = new Spec(px + spec0.product(), spec0.parts().stream().map(x -> px + x).toList(), spec0.supplierSuffix(), spec0.productLabel());
        int at = 0;
        List<Fac> sup = new ArrayList<>(), plants = new ArrayList<>(), hubs = new ArrayList<>(), whs = new ArrayList<>();
        for (int k = 0; k < size.suppliers; k++) { City c = pool.get(at++); String part = spec.parts().get(k % spec.parts().size());
            sup.add(new Fac(px + "SUP-" + code(c) + "-" + (k + 1), c.name() + " " + spec.supplierSuffix().get(k % spec.supplierSuffix().size()), "SUPPLIER", c, 1 + k % 2, "", part)); }
        for (int k = 0; k < size.hubs; k++) { City c = pool.get(at++); hubs.add(new Fac(px + "HUB-" + code(c), c.name() + " Logistics Hub", "LOGISTICS_HUB", c, 0, String.valueOf(300 + rnd.nextInt(600)), null)); }
        for (int k = 0; k < size.plants; k++) { City c = pool.get(at++); plants.add(new Fac(px + "PLANT-" + code(c), c.name() + " Assembly Plant", "MANUFACTURING_SITE", c, 0, String.valueOf(30 + rnd.nextInt(90)), null)); }
        for (int k = 0; k < size.warehouses; k++) { City c = pool.get(at++); whs.add(new Fac(px + "WH-" + code(c), c.name() + " Distribution Centre", "WAREHOUSE", c, 0, String.valueOf(5000 + rnd.nextInt(25000)), null)); }

        StringBuilder fac = new StringBuilder("id,name,type,location,tier,capacity\n");
        for (List<Fac> l : List.of(sup, hubs, plants, whs)) for (Fac f : l)
            fac.append(f.id).append(',').append(f.name).append(',').append(f.type).append(',').append(f.city.name()).append(' ').append(f.city.country()).append(',')
               .append(f.tier == 0 ? "" : String.valueOf(f.tier)).append(',').append(f.capacity).append('\n');

        Set<String> seen = new LinkedHashSet<>();
        StringBuilder lanes = new StringBuilder("from,to,mode,transit_hours,cost_per_kg\n");
        for (Fac s : sup) for (Fac h : nearest(s, hubs, 2)) lane(lanes, seen, s, h);          // two inbound routes per supplier: redundancy
        for (Fac p : plants) for (Fac h : nearest(p, hubs, 2)) lane(lanes, seen, h, p);        // two supply routes per plant
        for (Fac s : sup) { Fac p = nearest(s, plants, 1).get(0); if (km(s, p) < 1200) lane(lanes, seen, s, p); }   // short direct supply where it is realistic
        for (Fac w : whs) for (Fac p : nearest(w, plants, 1)) lane(lanes, seen, p, w);
        for (Fac w : whs) for (Fac h : nearest(w, hubs, 1)) lane(lanes, seen, h, w);

        StringBuilder stock = new StringBuilder("facility,material,quantity\n");
        for (Fac p : plants) for (String part : spec.parts()) stock.append(p.id).append(',').append(part).append(',').append(200 + rnd.nextInt(1800)).append('\n');
        for (Fac h : hubs) for (String part : spec.parts()) stock.append(h.id).append(',').append(part).append(',').append(2000 + rnd.nextInt(9000)).append('\n');
        for (Fac w : whs) stock.append(w.id).append(',').append(spec.product()).append(',').append(500 + rnd.nextInt(4000)).append('\n');
        StringBuilder bom = new StringBuilder("parent,child,quantity_per_parent\n");
        for (String part : spec.parts()) bom.append(spec.product()).append(',').append(part).append(',').append(1 + rnd.nextInt(4)).append('\n');

        String summary = "Generated " + size + " " + industry + " network for " + region + " (seed " + seed + "): " + sup.size() + " suppliers, " + hubs.size() + " hubs, " + plants.size()
                + " plants, " + whs.size() + " warehouses producing " + spec.productLabel() + ". City locations are real; company names, capacities and stock are invented, and transit times and "
                + "costs are estimated from distance. Replace them with your own data when you have it.";
        return new Dataset(fac.toString(), lanes.toString(), stock.toString(), bom.toString(), summary);
    }

    private record Fac(String id, String name, String type, City city, int tier, String capacity, String part) {}

    private static String code(City c) { return c.name().replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT).substring(0, Math.min(5, c.name().replaceAll("[^A-Za-z]", "").length())); }

    private static double km(Fac a, Fac b) {
        double r = 6371, dLat = Math.toRadians(b.city.lat() - a.city.lat()), dLon = Math.toRadians(b.city.lon() - a.city.lon());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(a.city.lat())) * Math.cos(Math.toRadians(b.city.lat())) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.sqrt(h));
    }

    private static List<Fac> nearest(Fac from, List<Fac> to, int n) {
        return to.stream().sorted(Comparator.comparingDouble(t -> km(from, t))).limit(n).toList();
    }

    /** Mode by distance; hours = fixed handling + distance / average speed; cost per kg = rate x distance. Same inputs, same numbers. */
    private static void lane(StringBuilder out, Set<String> seen, Fac a, Fac b) {
        if (a.id.equals(b.id) || !seen.add(a.id + ">" + b.id)) return;
        double d = km(a, b) * 1.25;   // road/sea routes are longer than the great-circle line
        String mode; double speed, handling, rate;
        if (d < 1000) { mode = "ROAD"; speed = 55; handling = 3; rate = 0.00015; }
        else if (d < 3000) { mode = "RAIL"; speed = 40; handling = 12; rate = 0.00008; }
        else { mode = "OCEAN"; speed = 28; handling = 48; rate = 0.00003; }
        out.append(a.id).append(',').append(b.id).append(',').append(mode).append(',').append(String.format(Locale.ROOT, "%.1f", handling + d / speed)).append(',')
           .append(String.format(Locale.ROOT, "%.3f", Math.max(0.05, rate * d))).append('\n');
    }
}
