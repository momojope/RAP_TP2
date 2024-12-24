import java.util.*;

// --- FICHIER: ResourceRedistribution.java ---
// Il contient :
//   1) La classe ResourceRedistribution
//   2) La classe interne TransferRecord

public class ResourceRedistribution {

    private List<TransferRecord> transfers;
    private PriorityQueue<EmergencySupplyNetwork.Warehouse> maxHeap; 
    private PriorityQueue<EmergencySupplyNetwork.Warehouse> minHeap; 

    public ResourceRedistribution() {
        transfers = new ArrayList<>();
        // Entrepôts excédentaires = tas max
        maxHeap = new PriorityQueue<>((w1, w2) -> w2.getCapacity() - w1.getCapacity());
        // Entrepôts en manque = tas min
        minHeap = new PriorityQueue<>(Comparator.comparingInt(EmergencySupplyNetwork.Warehouse::getCapacity));
    }

    public void initializeHeaps(List<EmergencySupplyNetwork.Warehouse> wList) {
        for (EmergencySupplyNetwork.Warehouse w : wList) {
            if (w.getCapacity() > 50) {
                maxHeap.add(w);
            } else if (w.getCapacity() < 50) {
                minHeap.add(w);
            }
        }
    }

    public void redistributeResources() {
        System.out.println("Resource Redistribution:");
        while (!maxHeap.isEmpty() && !minHeap.isEmpty()) {
            EmergencySupplyNetwork.Warehouse from = maxHeap.poll();
            EmergencySupplyNetwork.Warehouse to = minHeap.poll();

            int fromExcess = from.getCapacity() - 50;
            int toNeed = 50 - to.getCapacity();
            int transfer = Math.min(fromExcess, toNeed);

            from.setCapacity(from.getCapacity() - transfer);
            to.setCapacity(to.getCapacity() + transfer);

            System.out.println("Transferred " + transfer + " units from Warehouse "
                               + from.getId() + " to Warehouse " + to.getId());

            // On consigne le transfert
            transfers.add(new TransferRecord(from.getId(), to.getId(), transfer));

            // Réinsérer si toujours excédent ou besoin
            if (from.getCapacity() > 50) {
                maxHeap.add(from);
            } else if (from.getCapacity() < 50) {
                minHeap.add(from);
            }
            if (to.getCapacity() > 50) {
                maxHeap.add(to);
            } else if (to.getCapacity() < 50) {
                minHeap.add(to);
            }
        }
    }

    public List<TransferRecord> getTransfers() {
        return transfers;
    }

    // ==========================
    // CLASSE INTERNE
    // ==========================
    public static class TransferRecord {
        private int from;
        private int to;
        private int units;

        public TransferRecord(int from, int to, int units) {
            this.from = from;
            this.to = to;
            this.units = units;
        }
        public int getFromId() { return from; }
        public int getToId() { return to; }
        public int getUnits() { return units; }
    }
}
