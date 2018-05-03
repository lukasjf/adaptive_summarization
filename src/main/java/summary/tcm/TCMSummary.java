package summary.tcm;

import evaluation.Benchmarkable;
import graph.*;

import java.util.*;

/**
 * Created by lukas on 16.04.18.
 */
public class TCMSummary implements Benchmarkable {

    private static int LARGE_PRIME = 15485863;

    List<BaseGraph> graphs;

    public static TCMSummary createFromGraph(BaseGraph graph, int numberHashes, long maxSize){
        Random random = new Random(1337);
        int k = chooseK(graph, numberHashes, maxSize);
        List<HashFunction> hashes = new ArrayList<>();
        for (int i = 0; i < numberHashes; i++){
            int a = random.nextInt() % LARGE_PRIME;
            while (a == 0){
                a = random.nextInt() % LARGE_PRIME;
            }
            int b = random.nextInt() % LARGE_PRIME;
            hashes.add(new HashFunction(a, b, LARGE_PRIME, k));
        }
        return new TCMSummary(graph, hashes);
    }

    private static int chooseK(BaseGraph graph, int numberHashes, long maxSize){
        int numberEdgeTypes = graph.getNumberEdgeTypes();
        int k = 1;
        while (maxSize >
                graph.getNodes().size() * 4 * numberHashes +
                k * k / 8 * numberHashes * numberEdgeTypes){
            k++;
        }
        k--;
        if (k == 0){
            System.err.println("Too little storage");
            System.exit(1);
        }
        return k;
    }

    public TCMSummary(BaseGraph graph, List<HashFunction> hashes) {
        graphs = new ArrayList<>();
        for (HashFunction hash: hashes){

            BaseGraph g = new BaseGraph();
            BaseNode[] nodes = new BaseNode[hash.range()];

            for (int i = 0; i < hash.range(); i++){
                g.addNode(i, "");
            }

            for (BaseNode n: graph.getNodes()){
                int nodeHash = hash.getHash(n.getId());
                g.getIdMapping().get(nodeHash).getContainedNodes().add(n.getId());
            }

            List<BaseNode> emptyNodes = new ArrayList<>();
            for (BaseNode n: g.getNodes()){
                if (n.getContainedNodes().isEmpty()){
                    emptyNodes.add(n);
                }
            }
            for (BaseNode n: emptyNodes){
                g.removeNode(n.getId());
            }

            for (BaseEdge e: graph.getEdges()){
                int sourceHash = hash.getHash(e.getSource().getId());
                int targetHash = hash.getHash(e.getTarget().getId());
                g.addEdge(sourceHash, targetHash, e.getLabel());
            }
            g.index = graph.index;
            g.invertedIndex = graph.invertedIndex;
            graphs.add(g);
        }
    }

    @Override
    public List<Map<String, String>> query(BaseGraph query) {
        List<Map<String, String>> results = new ArrayList<>();

        Map<Integer, List<Map<String, String>>> intermediate = new HashMap<>();

        for (int i = 0; i < graphs.size(); i++) {
            intermediate.put(i, new SubgraphIsomorphism().query(query, graphs.get(i), false));
        }

        for (Map<String,String> result: intermediate.getOrDefault(0, new ArrayList<>())) {
            boolean missing = false;
            for (int i = 1; i < graphs.size(); i++){
                if (intermediate.get(i).stream().noneMatch(result::equals)){
                    missing = true;
                    break;
                }
            }
            if (!missing){
                results.add(result);
            }
        }
        return results;
    }

    @Override
    public void train(Map<BaseGraph, List<Map<String, String>>> queries) {
        
    }
}
