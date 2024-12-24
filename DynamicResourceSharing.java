import java.util.*;

// --- FICHIER: DynamicResourceSharing.java ---
// Contient :
//   1) DynamicResourceSharing
//   2) MergeStep
//   3) QueryRecord

public class DynamicResourceSharing {

    private int[] parent;
    private int[] rank;
    private List<MergeStep> mergeSteps;
    private List<QueryRecord> queryRecords;

    public DynamicResourceSharing(int n) {
        parent = new int[n];
        rank = new int[n];
        mergeSteps = new ArrayList<>();
        queryRecords = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            parent[i] = i;
            rank[i] = 0;
        }
    }

    public int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]);
        }
        return parent[x];
    }

    public void union(int x, int y, String labelX, String labelY) {
        int rootX = find(x);
        int rootY = find(y);

        if (rootX != rootY) {
            if (rank[rootX] > rank[rootY]) {
                parent[rootY] = rootX;
            } else if (rank[rootX] < rank[rootY]) {
                parent[rootX] = rootY;
            } else {
                parent[rootY] = rootX;
                rank[rootX]++;
            }

            // On enregistre la fusion
            mergeSteps.add(new MergeStep(
                "Merge",
                List.of(labelX, labelY),
                "Cluster " + (find(x) + 1)
            ));
        }
    }

    public void addQuery(String query, boolean sameCluster) {
        queryRecords.add(new QueryRecord(query, sameCluster ? "Yes" : "No"));
    }

    public List<MergeStep> getMergeSteps() {
        return mergeSteps;
    }

    public List<QueryRecord> getQueryRecords() {
        return queryRecords;
    }

    // ==========================
    // CLASSES INTERNES
    // ==========================
    public static class MergeStep {
        private String action;
        private List<String> cities;
        private String clusterAfterMerge;

        public MergeStep(String action, List<String> cities, String clusterAfterMerge) {
            this.action = action;
            this.cities = cities;
            this.clusterAfterMerge = clusterAfterMerge;
        }
        public String getAction() { return action; }
        public List<String> getCities() { return cities; }
        public String getClusterAfterMerge() { return clusterAfterMerge; }
    }

    public static class QueryRecord {
        private String query;
        private String result;

        public QueryRecord(String query, String result) {
            this.query = query;
            this.result = result;
        }
        public String getQuery() { return query; }
        public String getResult() { return result; }
    }
}
