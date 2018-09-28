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
        for (BaseEdge e: graph.getEdges()){
            summary.addEdge(e.getSource().getId(), e.getTarget().getId(), e.getLabel());
        }
        for (String edgelabel : graph.getEdges().stream().map(BaseEdge::getLabel).collect(Collectors.toSet())) {

            Set<Integer> high_degree = new HashSet<>();

            //A node is high degree with at least tau incoming edges of a label
            for (BaseNode n : original.getNodes()) {
                if (original.inEdgesFor(n.getId()).stream().filter(e -> e.getLabel().equals(edgelabel)).count() >= tau) {
                    high_degree.add(n.getId());
                }
            }

            doInHigh(edgelabel, high_degree);

            high_degree.clear();
            for (BaseNode n : original.getNodes()) {
                if (original.outEdgesFor(n.getId()).stream().filter(e -> e.getLabel().equals(edgelabel)).count() >= tau) {
                    high_degree.add(n.getId());
                }
            }
            doOutHigh(edgelabel, high_degree);
        }
        System.out.println("" + tau + "," + graphSize + "," + new GraphEncoder().encode(summary));
    }

    private void doInHigh(String edgelabel, Set<Integer> high_degree) {

        // group all nodes depending on which high-degree nodes they are connected too
        Map<Set<Integer>, Set<Integer>> w = new HashMap<>();
        for (BaseNode n : original.getNodes()) {
            Set<Integer> high_connected = original.outEdgesFor(n.getId()).stream()
                    .filter(e -> e.getLabel().equals(edgelabel) && high_degree.contains(e.getTarget().getId()))
                    .map(e -> e.getTarget().getId()).collect(Collectors.toSet());
            if (high_connected.isEmpty()){
                continue;
            }
            if (!w.containsKey(high_connected)) {
                w.put(high_connected, new HashSet<>());
            }
            w.get(high_connected).add(n.getId());
        }

        for (Set<Integer> key : w.keySet()) {
            if (worthIt(key.size(), w.get(key).size())) {
                BaseNode dedens = summary.addNode(counter--, "");
                for (int high : key) {
                    summary.addEdge(dedens.getId(), high, edgelabel);
                }
                for (int connected : w.get(key)) {
                    summary.addEdge(connected, dedens.getId(), edgelabel);
                    for (BaseEdge e : new ArrayList<>(summary.outEdgesFor(connected))) {
                        if (key.contains(e.getTarget().getId())) {
                            summary.removeEdge(e);
                        }
                    }
                }
            }
        }
    }

    private void doOutHigh(String edgelabel, Set<Integer> high_degree) {
        Map<Set<Integer>, Set<Integer>> w = new HashMap<>();
        for (BaseNode n : original.getNodes()) {
            Set<Integer> high_connected = original.inEdgesFor(n.getId()).stream()
                    .filter(e -> e.getLabel().equals(edgelabel))
                    .map(e -> e.getSource().getId()).filter(high_degree::contains).collect(Collectors.toSet());
            if (!w.containsKey(high_connected)) {
                w.put(high_connected, new HashSet<>());
            }
            w.get(high_connected).add(n.getId());
        }

        for (Set<Integer> key : w.keySet()){
            if (worthIt(key.size(), w.get(key).size())){
                BaseNode dedens = summary.addNode(counter--, "");
                for (int high : key){
                    summary.addEdge(high, dedens.getId(), edgelabel);
                }
                for (int connected: w.get(key)){
                    summary.addEdge(dedens.getId(), connected, edgelabel);
                    for (BaseEdge e: new ArrayList<>(summary.inEdgesFor(connected))){
                        if (key.contains(e.getSource().getId())){
                            summary.removeEdge(e);
                        }
                    }
                }
            }
        }
    }

    private boolean worthIt(int size1, int size2) {
        int edgesSaved = size1 * size2 - size1 - size2;
        int additionalCost = 28; //one more node
        return additionalCost - 16 * edgesSaved < 0;
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
