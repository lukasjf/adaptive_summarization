package summary.dedensification;

import encoding.GraphEncoder;
import evaluation.Benchmarkable;
import graph.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by lukas on 25.09.18.
 */
public class DedensifiedSummary implements Benchmarkable{

    private BaseGraph original;
    private BaseGraph summary;

    public DedensifiedSummary(BaseGraph graph, int tau) {
        int counter = Integer.MAX_VALUE;
        original = graph;
        long graphSize = new GraphEncoder().encode(graph);
        summary = new BaseGraph();
        // treat every edge label differently
        for (BaseNode n : graph.getNodes()) {
            summary.addNode(n.getId(), Dataset.I.labelFrom(n.getId()));
        }
        for (String edgelabel : graph.getEdges().stream().map(BaseEdge::getLabel).collect(Collectors.toSet())) {

            //System.out.println(edgelabel);

            Set<Integer> high_degree = new HashSet<>();

            for (BaseNode n : graph.getNodes()) {
                if (graph.outEdgesFor(n.getId()).stream().filter(e -> e.getLabel().equals(edgelabel)).count() >= tau) {
                    high_degree.add(n.getId());
                }
            }

            Map<Set<Integer>, Set<Integer>> w = new HashMap<>();
            for (BaseNode n : graph.getNodes()) {
                Set<Integer> high_connected = graph.outEdgesFor(n.getId()).stream()
                        .filter(e -> e.getLabel().equals(edgelabel) && high_degree.contains(e.getTarget().getId()))
                        .map(e -> e.getTarget().getId()).collect(Collectors.toSet());
                if (!w.containsKey(high_connected)) {
                    w.put(high_connected, new HashSet<>());
                }
                w.get(high_connected).add(n.getId());
            }

            for (Set<Integer> key : w.keySet()) {
                if (key.isEmpty() || notWorthIt(key.size(), w.get(key).size())) {
                    for (int n : w.get(key)) {
                        for (BaseEdge e : graph.outEdgesFor(n)) {
                            if (e.getLabel().equals(edgelabel)) {
                                summary.addEdge(n, e.getTarget().getId(), edgelabel);
                            }
                        }
                    }
                } else {
                    BaseNode dedens = summary.addNode(counter--, "");
                    for (int n : key) {
                        summary.addEdge(dedens.getId(), n, edgelabel);
                    }
                    for (int n : w.get(key)) {
                        summary.addEdge(n, dedens.getId(), edgelabel);
                        for (BaseEdge e : graph.outEdgesFor(n)) {
                            if (e.getLabel().equals(edgelabel) && !key.contains(e.getTarget().getId())) {
                                summary.addEdge(n, e.getTarget().getId(), edgelabel);
                            } else {
                                int i = 0;
                            }
                        }
                    }
                }
            }
        }
        System.out.println("" + tau + "," + graphSize + "," + new GraphEncoder().encode(summary));
    }

    private boolean notWorthIt(int size1, int size2) {
        int edgesSaved = size1 * size2 - size1 - size2;
        int additionalCost = 28; //one more node
        return additionalCost - 16 * edgesSaved > 0;
    }


    @Override
    public List<Map<String, String>> query(BaseGraph query) {
        return null;
    }

    @Override
    public List<Map<String, String>> query(BaseGraph query, int timeout) {
        return null;
    }

    @Override
    public void train(Map<BaseGraph, List<Map<String, String>>> queries) {

    }

    @Override
    public long size() {
        return new GraphEncoder().encode(summary);
    }
}
