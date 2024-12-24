import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// --- FICHIER: NetworkApp.java ---
// Classe principale avec le main(...)

public class NetworkApp {

    // Petit utilitaire pour label d'entrepôts
    private static String warehouseLabel(int id) {
        switch(id) {
            case 101: return "Warehouse X";
            case 102: return "Warehouse Y";
            case 103: return "Warehouse Z";
        }
        return "Warehouse " + id;
    }

    // On stocke les noms des villes (ex: "City A", etc.)
    private static List<String> cityNames = new ArrayList<>();

    public static void main(String[] args) {
        // Nom du fichier par défaut
        String inputFile = "TestCase0.txt";
        // Fichier de sortie
        String outputFile = "Output_TestCase0.json";

        // Si on passe un argument (ex. TestCase1.txt)
        if (args.length > 0) {
            inputFile = args[0];
            // On peut générer un JSON en fonction du nom de l'input s'il faut
            outputFile = "Output_" + args[0].replace(".txt", ".json");
        }

        // Lecture du fichier
        List<EmergencySupplyNetwork.City> cities = new ArrayList<>();
        List<EmergencySupplyNetwork.Warehouse> warehouses = new ArrayList<>();
        readInputFile(inputFile, cities, warehouses);

        System.out.println("Number of cities loaded: " + cities.size());
        System.out.println("Number of warehouses loaded: " + warehouses.size());

        // Tâche 1 & 2
        EmergencySupplyNetwork network = new EmergencySupplyNetwork(cities, warehouses);
        network.buildCostMatrix();
        network.printCostMatrix();
        network.allocateResources();

        // Tâche 3
        ResourceRedistribution rr = new ResourceRedistribution();
        rr.initializeHeaps(warehouses);
        rr.redistributeResources();

        System.out.println("Final Resource Levels (after Tâche 3):");
        for (EmergencySupplyNetwork.Warehouse w : warehouses) {
            System.out.println("Warehouse " + w.getId() + ": " + w.getCapacity() + " units");
        }

        // Tâche 4
        System.out.println("\nTâche 4 : Dynamic Resource Sharing (Union-Find)");
        DynamicResourceSharing ds = new DynamicResourceSharing(cities.size());

        // Affichage initial
        System.out.println("Initial Clusters:");
        for (int i = 0; i < cities.size(); i++) {
            int clusterIndex = ds.find(i) + 1;
            // ex: cityNames.get(i) = "City A"
            System.out.println(cityNames.get(i) + " belongs to cluster: " + clusterIndex);
        }

        // Fusion (ex: City A & City B si on a au moins 2 villes)
        if (cities.size() > 1) {
            System.out.println("\nMerging clusters of " + cityNames.get(0) + " and " + cityNames.get(1) + "...");
            ds.union(0, 1, cityNames.get(0), cityNames.get(1));
        }

        System.out.println("\nClusters after merging:");
        for (int i = 0; i < cities.size(); i++) {
            int clusterIndex = ds.find(i) + 1;
            System.out.println(cityNames.get(i) + " belongs to cluster: " + clusterIndex);
        }

        // Queries si on a 3 villes
        if (cities.size() == 3) {
            boolean sameAC = (ds.find(0) == ds.find(2));
            System.out.println("Query: Are " + cityNames.get(0) + " and " + cityNames.get(2) + " in the same cluster?");
            System.out.println(sameAC ? "Yes" : "No");
            ds.addQuery("Are " + cityNames.get(0) + " and " + cityNames.get(2) + " in the same cluster?", sameAC);

            boolean sameAB = (ds.find(0) == ds.find(1));
            System.out.println("Query: Are " + cityNames.get(0) + " and " + cityNames.get(1) + " in the same cluster?");
            System.out.println(sameAB ? "Yes" : "No");
            ds.addQuery("Are " + cityNames.get(0) + " and " + cityNames.get(1) + " in the same cluster?", sameAB);

            boolean sameBC = (ds.find(1) == ds.find(2));
            System.out.println("Query: Are " + cityNames.get(1) + " and " + cityNames.get(2) + " in the same cluster?");
            System.out.println(sameBC ? "Yes" : "No");
            ds.addQuery("Are " + cityNames.get(1) + " and " + cityNames.get(2) + " in the same cluster?", sameBC);
        }

        // Écriture JSON
        writeOutputJSON(outputFile, network, rr, ds);
        System.out.println("\nFichier JSON généré : " + outputFile);
    }

    // Lecture du fichier
    private static void readInputFile(String filename,
                                      List<EmergencySupplyNetwork.City> cities,
                                      List<EmergencySupplyNetwork.Warehouse> warehouses) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            boolean inCities = false, inWarehouses = false;
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("Cities:")) {
                    inCities = true;
                    inWarehouses = false;
                    continue;
                } else if (line.startsWith("Warehouses:")) {
                    inCities = false;
                    inWarehouses = true;
                    continue;
                }

                if (inCities && line.startsWith("City")) {
                    // ex: "City A: ID = 1, Coordinates = (2,3), Demand = 50 units, Priority = High"
                    String regex = "(City\\s+\\w+): ID = (\\d+), Coordinates = \\((\\d+),\\s*(\\d+)\\), Demand = (\\d+) units, Priority = (\\w+).*";
                    Matcher m = Pattern.compile(regex).matcher(line);
                    if (m.matches()) {
                        String cityLabel = m.group(1);  // ex: "City A"
                        int id = Integer.parseInt(m.group(2));
                        double x = Double.parseDouble(m.group(3));
                        double y = Double.parseDouble(m.group(4));
                        int demand = Integer.parseInt(m.group(5));
                        String priority = m.group(6);

                        cityNames.add(cityLabel);

                        EmergencySupplyNetwork.City c =
                            new EmergencySupplyNetwork.City(id, x, y, demand, priority);
                        cities.add(c);
                    }
                } else if (inWarehouses && line.startsWith("Warehouse")) {
                    // ex: Warehouse X: ID = 101, Coordinates = (10,20), Capacity = 100 units
                    String regex = ".*ID = (\\d+), Coordinates = \\((\\d+),\\s*(\\d+)\\), Capacity = (\\d+) units.*";
                    Matcher m = Pattern.compile(regex).matcher(line);
                    if (m.matches()) {
                        int id = Integer.parseInt(m.group(1));
                        double x = Double.parseDouble(m.group(2));
                        double y = Double.parseDouble(m.group(3));
                        int cap = Integer.parseInt(m.group(4));
                        EmergencySupplyNetwork.Warehouse w =
                            new EmergencySupplyNetwork.Warehouse(id, x, y, cap);
                        warehouses.add(w);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Génération du JSON
    private static void writeOutputJSON(String filename,
                                        EmergencySupplyNetwork network,
                                        ResourceRedistribution rr,
                                        DynamicResourceSharing ds) {
        try (PrintWriter out = new PrintWriter(filename)) {
            out.println("{");
            out.println("  \"Task 1 and 2\": {");

            // Graph Representation
            out.println("    \"Graph Representation\": {");
            out.println("      \"Cost Matrix\": [");
            List<Map<String,Object>> costMat = network.getCostMatrixAsList();
            for (int i = 0; i < costMat.size(); i++) {
                Map<String,Object> row = costMat.get(i);
                out.print("        {");
                int cpt = 0;
                for (Map.Entry<String,Object> e : row.entrySet()) {
                    out.print(" \"" + e.getKey() + "\": " + e.getValue());
                    cpt++;
                    if (cpt < row.size()) out.print(",");
                }
                out.print(" }");
                if (i < costMat.size()-1) out.println(",");
                else out.println();
            }
            out.println("      ]");
            out.println("    },");

            // Resource Allocation
            out.println("    \"Resource Allocation\": [");
            List<EmergencySupplyNetwork.AllocationRecord> allocs = network.getAllocationRecords();
            for (int i = 0; i < allocs.size(); i++) {
                EmergencySupplyNetwork.AllocationRecord ar = allocs.get(i);
                out.println("      {");
                out.println("        \"City\": \"" + ar.city + "\",");
                out.println("        \"Priority\": \"" + ar.priority + "\",");
                if (ar.parts.size() == 1) {
                    EmergencySupplyNetwork.AllocationPart ap = ar.parts.get(0);
                    out.println("        \"Allocated\": " + ap.units + ",");
                    out.println("        \"Warehouse\": \"" + ap.warehouse + "\"");
                } else {
                    out.println("        \"Allocated\": [");
                    for (int j = 0; j < ar.parts.size(); j++) {
                        EmergencySupplyNetwork.AllocationPart ap = ar.parts.get(j);
                        out.println("          {");
                        out.println("            \"Units\": " + ap.units + ",");
                        out.println("            \"Warehouse\": \"" + ap.warehouse + "\"");
                        out.print("          }");
                        if (j < ar.parts.size()-1) out.println(",");
                        else out.println();
                    }
                    out.println("        ]");
                }
                out.print("      }");
                if (i < allocs.size()-1) out.println(",");
                else out.println();
            }
            out.println("    ],");

            // Remaining Capacities
            out.println("    \"Remaining Capacities\": {");
            List<EmergencySupplyNetwork.Warehouse> wList = network.getWarehouses();
            Map<Integer,Integer> postCaps = network.postAllocationCapacities; 
            for (int i = 0; i < wList.size(); i++) {
                EmergencySupplyNetwork.Warehouse w = wList.get(i);
                int cap = postCaps.get(w.getId()); 
                out.print("      \"Warehouse " + w.getId() + "\": " + cap);
                if (i < wList.size()-1) out.println(",");
                else out.println();
            }
            out.println("    }");
            out.println("  },"); // fin Task 1 and 2

            // Tâche 3
            out.println("  \"Task 3\": {");
            out.println("    \"Resource Redistribution\": {");

            // Transfers
            out.println("      \"Transfers\": [");
            List<ResourceRedistribution.TransferRecord> trs = rr.getTransfers();
            for (int i = 0; i < trs.size(); i++) {
                ResourceRedistribution.TransferRecord t = trs.get(i);
                out.println("        {");
                out.println("          \"From\": \"" + warehouseLabel(t.getFromId()) + "\",");
                out.println("          \"To\": \"" + warehouseLabel(t.getToId()) + "\",");
                out.println("          \"Units\": " + t.getUnits());
                out.print("        }");
                if (i < trs.size()-1) out.println(",");
                else out.println();
            }
            out.println("      ],");

            // Final Resource Levels
            out.println("      \"Final Resource Levels\": {");
            for (int i = 0; i < wList.size(); i++) {
                EmergencySupplyNetwork.Warehouse w = wList.get(i);
                int finalCap = w.getCapacity();
                out.print("        \"Warehouse " + w.getId() + "\": " + finalCap);
                if (i < wList.size()-1) out.println(",");
                else out.println();
            }
            out.println("      }");

            out.println("    }");
            out.println("  },"); // fin Task 3

            // Tâche 4
            out.println("  \"Task 4\": {");
            out.println("    \"Dynamic Resource Sharing\": {");

            // Merging Steps
            out.println("      \"Merging Steps\": [");
            List<DynamicResourceSharing.MergeStep> merges = ds.getMergeSteps();
            for (int i = 0; i < merges.size(); i++) {
                DynamicResourceSharing.MergeStep ms = merges.get(i);
                out.println("        {");
                out.println("          \"Action\": \"" + ms.getAction() + "\",");
                out.print("          \"Cities\": [");
                List<String> cits = ms.getCities();
                for (int j = 0; j < cits.size(); j++) {
                    out.print("\"" + cits.get(j) + "\"");
                    if (j < cits.size() - 1) out.print(", ");
                }
                out.println("],");
                out.println("          \"Cluster After Merge\": \"" + ms.getClusterAfterMerge() + "\"");
                out.print("        }");
                if (i < merges.size() - 1) out.println(",");
                else out.println();
            }
            out.println("      ],");

            // Cluster Membership
            out.println("      \"Cluster Membership After Merging\": {");
            for (int i = 0; i < network.getCities().size(); i++) {
                int root = ds.find(i);
                String clusterLabel = "Cluster " + (root + 1);
                out.print("        \"" + cityNames.get(i) + "\": \"" + clusterLabel + "\"");
                if (i < network.getCities().size() - 1) out.println(",");
                else out.println();
            }
            out.println("      },");

            // Queries
            out.println("      \"Queries\": [");
            List<DynamicResourceSharing.QueryRecord> queries = ds.getQueryRecords();
            for (int i = 0; i < queries.size(); i++) {
                DynamicResourceSharing.QueryRecord qr = queries.get(i);
                out.println("        {");
                out.println("          \"Query\": \"" + qr.getQuery() + "\",");
                out.println("          \"Result\": \"" + qr.getResult() + "\"");
                out.print("        }");
                if (i < queries.size() - 1) out.println(",");
                else out.println();
            }
            out.println("      ]");

            out.println("    }");
            out.println("  }"); // fin Task 4

            out.println("}"); // fin JSON
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
