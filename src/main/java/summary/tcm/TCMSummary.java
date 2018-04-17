package graph.tcm;

import graph.BaseEdge;
import graph.BaseGraph;
import graph.BaseNode;
import graph.GraphQueryAble;
import graph.summary.Summary;
import graph.summary.SummaryNode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by lukas on 16.04.18.
 */
public class TCMSummary implements GraphQueryAble {

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
        int numberEdgeTypes = graph.getEdges().stream().map(e -> e.getLabel()).collect(Collectors.toSet()).size();
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
        super(graph, null);
        graphs = new ArrayList<>();
        for (HashFunction hash: hashes){

            Summary g = new Summary(graph, null);
            TCMNode[] nodes = new TCMNode[hash.range()];

            for (BaseNode n: graph.nodes){
                int nodeHash = hash.getHash(n.getId());
                if (nodes[nodeHash] == null){
                    nodes[nodeHash] = new TCMNode(nodeHash);
                }
                nodes[nodeHash].labels.add(n.getLabel());
            }

            for (TCMNode n: nodes){
                if (n != null){
                    SummaryNode sn = new SummaryNode(n.getId(), n.labels);
                    g.addNode(sn);
                }
            }
            for (BaseEdge e: graph.getEdges()){
                int sourceHash = hash.getHash(e.getSource().getId());
                int targetHash = hash.getHash(e.getTarget().getId());
                g.addSEdge((SummaryNode)g.getNodeMapping().get(sourceHash), (SummaryNode)g.getNodeMapping().get(targetHash), e.getLabel());
            }
            graphs.add(g);
        }
    }

    public TCMNode nodeFor(int i, String label){
        for (BaseNode n: graphs.get(i).getNodes()){
            if (((TCMNode)n ).labels.contains(label)){
                return (TCMNode)n;
            }
        }
        return null;
    }

    @Override
    public List<String[]> query(BaseGraph query) {
        System.out.println("query");
        List<String[]> results = new ArrayList<>();

        Map<Integer, List<String[]>> intermediate = new HashMap<>();

        for (int i = 0; i < graphs.size(); i++) {
            intermediate.put(i, graphs.get(i).query(query));
        }

        for (String[] result : intermediate.getOrDefault(0, new ArrayList<>())) {
            boolean missing = false;
            for (int i = 1; i < graphs.size(); i++){
                if (intermediate.get(i).stream().noneMatch(otherResult -> Arrays.equals(otherResult, result))){
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
}
