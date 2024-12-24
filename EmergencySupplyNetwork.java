import java.util.*;

// --- FICHIER: EmergencySupplyNetwork.java ---
// Il contient :
//   1) La classe EmergencySupplyNetwork
//   2) Les classes internes City, Warehouse, AllocationPart, AllocationRecord

public class EmergencySupplyNetwork {

    // ----- Attributs -----
    private List<City> cities;
    private List<Warehouse> warehouses;
    private double[][] costMatrix;

    // Capacités APRES Tâche 2
    public Map<Integer, Integer> postAllocationCapacities;
    // Stockage des allocations (pour JSON)
    private List<AllocationRecord> allocationRecords;

    // ----- Constructeur -----
    public EmergencySupplyNetwork(List<City> cities, List<Warehouse> warehouses) {
        this.cities = cities;
        this.warehouses = warehouses;
        this.postAllocationCapacities = new HashMap<>();
        this.allocationRecords = new ArrayList<>();
    }

    public List<City> getCities() { return cities; }
    public List<Warehouse> getWarehouses() { return warehouses; }
    public List<AllocationRecord> getAllocationRecords() { return allocationRecords; }

    // Tâche 1 : construire la matrice de coûts
    public void buildCostMatrix() {
        int nC = cities.size();
        int nW = warehouses.size();
        costMatrix = new double[nC][nW];

        for (int i = 0; i < nC; i++) {
            City c = cities.get(i);
            for (int j = 0; j < nW; j++) {
                Warehouse w = warehouses.get(j);
                double dist = distance(c.getX(), c.getY(), w.getX(), w.getY());
                int coeff = getTransportCoefficient(dist);
                costMatrix[i][j] = dist * coeff;
            }
        }
    }

    // Pour un affichage "tableau" de la matrice (Tâches 1 et 2)
    public void printCostMatrix() {
        System.out.println("Graph Representation (Cost Matrix):");
        System.out.println("----------------------------------------------------------------");
        System.out.print("cities     |");
        for (Warehouse w : warehouses) {
            System.out.printf(" Warehouse %3d |", w.getId());
        }
        System.out.println();
        System.out.println("----------------------------------------------------------------");

        for (int i = 0; i < costMatrix.length; i++) {
            System.out.printf("City %-6d |", (i + 1));
            for (int j = 0; j < costMatrix[i].length; j++) {
                System.out.printf(" %10.2f  |", costMatrix[i][j]);
            }
            System.out.println();
        }
        System.out.println("----------------------------------------------------------------");
    }

    // Pour générer la matrice de coûts en structure exploitable (JSON)
    public List<Map<String,Object>> getCostMatrixAsList() {
        List<Map<String,Object>> result = new ArrayList<>();
        for (int i = 0; i < cities.size(); i++) {
            City c = cities.get(i);
            Map<String,Object> row = new LinkedHashMap<>();
            row.put("City", "City " + c.getId());
            for (int j = 0; j < warehouses.size(); j++) {
                Warehouse w = warehouses.get(j);
                double cost = costMatrix[i][j];
                row.put("Warehouse " + w.getId(), String.format("%.2f", cost));
            }
            result.add(row);
        }
        return result;
    }

    // Tâche 2 : Allocation multi-entrepôts (priorités + file de priorité)
    public void allocateResources() {
        PriorityQueue<City> pq = new PriorityQueue<>((c1, c2) ->
            Integer.compare(priorityValue(c2.getPriority()), priorityValue(c1.getPriority()))
        );
        pq.addAll(cities);

        while (!pq.isEmpty()) {
            City city = pq.poll();
            int cityIndex = cities.indexOf(city);
            int demandRem = city.getDemand();

            System.out.println("Allocating resources for City " + city.getId()
                               + " (Priority: " + city.getPriority() + ")");
            AllocationRecord rec = new AllocationRecord("City " + city.getId(), city.getPriority());

            while (demandRem > 0) {
                int bestWhIndex = findBestWarehouse(cityIndex);
                if (bestWhIndex == -1) {
                    System.out.println("No available warehouse for City " + city.getId()
                                       + " (remaining demand: " + demandRem + ")");
                    break;
                }
                Warehouse wh = warehouses.get(bestWhIndex);
                int canAlloc = Math.min(wh.getCapacity(), demandRem);
                wh.setCapacity(wh.getCapacity() - canAlloc);
                demandRem -= canAlloc;

                System.out.println("Allocated " + canAlloc + " units from Warehouse " + wh.getId());
                rec.parts.add(new AllocationPart(canAlloc, "Warehouse " + wh.getId()));
            }

            city.setDemand(demandRem);
            if (!rec.parts.isEmpty()) {
                allocationRecords.add(rec);
            }
        }

        System.out.println("Remaining Warehouse Capacities (just after Tâche 2):");
        for (Warehouse w : warehouses) {
            System.out.println("Warehouse " + w.getId() + ": " + w.getCapacity() + " units");
        }

        // Enregistrer ces capacités
        postAllocationCapacities.clear();
        for (Warehouse w : warehouses) {
            postAllocationCapacities.put(w.getId(), w.getCapacity());
        }
    }

    // Cherche l'entrepôt le moins coûteux pour cityIndex
    private int findBestWarehouse(int cityIndex) {
        double minCost = Double.MAX_VALUE;
        int bestIndex = -1;
        for (int j = 0; j < warehouses.size(); j++) {
            if (warehouses.get(j).getCapacity() <= 0) continue;
            double cst = costMatrix[cityIndex][j];
            if (cst < minCost) {
                minCost = cst;
                bestIndex = j;
            }
        }
        return bestIndex;
    }

    private int priorityValue(String p) {
        switch (p.toLowerCase()) {
            case "high":   return 3;
            case "medium": return 2;
            case "low":    return 1;
        }
        return 0;
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    private static int getTransportCoefficient(double d) {
        if (d <= 10) return 1;
        if (d <= 20) return 2;
        return 3;
    }

    // ==========================
    // CLASSES INTERNES
    // ==========================
    public static class City {
        private int id;
        private double x, y;
        private int demand;
        private String priority; // "High", "Medium", "Low"

        public City(int id, double x, double y, int demand, String priority) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.demand = demand;
            this.priority = priority;
        }
        public int getId() { return id; }
        public double getX() { return x; }
        public double getY() { return y; }
        public int getDemand() { return demand; }
        public void setDemand(int d) { this.demand = d; }
        public String getPriority() { return priority; }
    }

    public static class Warehouse {
        private int id;
        private double x, y;
        private int capacity;

        public Warehouse(int id, double x, double y, int capacity) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.capacity = capacity;
        }
        public int getId() { return id; }
        public double getX() { return x; }
        public double getY() { return y; }
        public int getCapacity() { return capacity; }
        public void setCapacity(int cap) { this.capacity = cap; }
    }

    public static class AllocationPart {
        public int units;
        public String warehouse;

        public AllocationPart(int units, String warehouse) {
            this.units = units;
            this.warehouse = warehouse;
        }
    }

    public static class AllocationRecord {
        public String city;
        public String priority;
        public List<AllocationPart> parts;

        public AllocationRecord(String city, String priority) {
            this.city = city;
            this.priority = priority;
            this.parts = new ArrayList<>();
        }
    }
}
