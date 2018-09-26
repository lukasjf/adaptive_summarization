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
    int counter = Integer.MAX_VALUE;

    public DedensifiedSummary(BaseGraph graph, int tau) {
        original = graph;
        long graphSize = new GraphEncoder().encode(graph);
        summary = new BaseGraph();
        // treat every edge label differently
        for (BaseNode n : graph.getNodes()) {
            summary.addNode(n.getId(), Dataset.I.labelFrom(n.getId()));
        }
        for (String edgelabel : graph.getEdges().stream().map(BaseEdge::getLabel).collect(Collectors.toSet())) {

            doInHigh(tau, edgelabel);
            //doOutHigh(tau, edgelabel);
        }
        System.out.println("" + tau + "," + graphSize + "," + new GraphEncoder().encode(summary));
    }

    private void doInHigh(int tau, String edgelabel) {
        Set<Integer> high_degree = new HashSet<>();

        //A node is high degree with at least tau incoming edges of a label
        for (BaseNode n : original.getNodes()) {
            if (original.inEdgesFor(n.getId()).stream().filter(e -> e.getLabel().equals(edgelabel)).count() >= tau) {
                high_degree.add(n.getId());
            }
        }

        // group all nodes depending on which high-degree nodes they are connected too
        Map<Set<Integer>, Set<Integer>> w = new HashMap<>();
        for (BaseNode n : original.getNodes()) {
            Set<Integer> high_connected = original.outEdgesFor(n.getId()).stream()
                    .filter(e -> e.getLabel().equals(edgelabel) && high_degree.contains(e.getTarget().getId()))
                    .map(e -> e.getTarget().getId()).collect(Collectors.toSet());
            if (!w.containsKey(high_connected)) {
                w.put(high_connected, new HashSet<>());
            }
            w.get(high_connected).add(n.getId());
        }

        // add a compressor node, if we can save storage --- otherwise add edges directly
        for (Set<Integer> key : w.keySet()) {
            if (key.isEmpty() || notWorthIt(key.size(), w.get(key).size())) {
                for (int n : w.get(key)) {
                    for (BaseEdge e : original.outEdgesFor(n)) {
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
                    for (BaseEdge e : original.outEdgesFor(n)) {
                        if (e.getLabel().equals(edgelabel) && !key.contains(e.getTarget().getId())) {
                            summary.addEdge(n, e.getTarget().getId(), edgelabel);
                        }
                    }
                }
            }
        }
    }

    private void doOutHigh(int tau, String edgelabel) {
        Set<Integer> high_degree = new HashSet<>();

        for (BaseNode n : original.getNodes()) {
            if (original.outEdgesFor(n.getId()).stream().filter(e -> e.getLabel().equals(edgelabel)).count() >= tau) {
                high_degree.add(n.getId());
            }
        }

        Map<Set<Integer>, Set<Integer>> w = new HashMap<>();
        for (BaseNode n : original.getNodes()) {
            Set<Integer> high_connected = original.inEdgesFor(n.getId()).stream()
                    .filter(e -> e.getLabel().equals(edgelabel) && high_degree.contains(e.getSource().getId()))
                    .map(e -> e.getSource().getId()).collect(Collectors.toSet());
            if (!w.containsKey(high_connected)) {
                w.put(high_connected, new HashSet<>());
            }
            w.get(high_connected).add(n.getId());
        }

        for (Set<Integer> key : w.keySet()) {
            if (key.isEmpty() || notWorthIt(key.size(), w.get(key).size())) {
                for (int n : w.get(key)) {
                    for (BaseEdge e : original.inEdgesFor(n)) {
                        if (e.getLabel().equals(edgelabel)) {
                            summary.addEdge(e.getSource().getId(), n, edgelabel);
                        }
                    }
                }
            } else {
                BaseNode dedens = summary.addNode(counter--, "");
                for (int n : key) {
                    summary.addEdge(n, dedens.getId(), edgelabel);
                }
                for (int n : w.get(key)) {
                    summary.addEdge(dedens.getId(), n, edgelabel);
                    for (BaseEdge e : original.inEdgesFor(n)) {
                        if (e.getLabel().equals(edgelabel) && !key.contains(e.getSource().getId())) {
                            summary.addEdge(e.getSource().getId(), n, edgelabel);
                        }
                    }
                }
            }
        }
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
