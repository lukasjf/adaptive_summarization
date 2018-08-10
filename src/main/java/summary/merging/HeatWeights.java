package summary.merging;

import encoding.SummaryEncoder;
import graph.BaseEdge;
import graph.BaseGraph;
import graph.BaseNode;
import graph.Dataset;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by lukas on 12.07.18.
 */
public class HeatWeights implements WeightCreation {

    MergedSummary merged;

    Map<Integer, Double> heats = new HashMap<>();

    int k;
    double t;
    double EPSILON = 1.0e-4;

    public HeatWeights(double t){
        this.k = (int) (2 * t * Math.log(1/EPSILON)) + 1;
        this.t = t;
    }

    @Override
    public void initializeWeights(MergedSummary merged, Map<BaseGraph, List<Map<String, String>>> queries) {
        for (List<Map<String, String>> results : queries.values()) {
            for (Map<String, String> result : results) {
                for (String r : result.values()) {
                    int nodeID = Dataset.I.IDFrom(r);
                    heats.put(nodeID, heats.getOrDefault(nodeID, 0.0) + 1);
                }
            }
        }

        double[] psis = computePsis();

        Map<BaseNode, Double> heat = new HashMap<>();
        Map<Entry, Double> residuals = new HashMap<>();

        List<Entry> queue = new ArrayList<>();
        double heatsum = heats.values().stream().mapToDouble(d->d).sum();
        for (int key : heats.keySet()) {
            Entry entry = new Entry(Dataset.I.getGraph().nodeWithId(key), 0);
            residuals.put(entry, heats.get(key)/heatsum);
            queue.add(entry);
        }

        while (!queue.isEmpty()) {
            Entry entry = queue.remove(0);
            Set<BaseNode> neighbors = getNeighbors(entry.node);
            double res = residuals.get(entry);
            heat.put(entry.node, res + heat.getOrDefault(entry.node, 0.0));
            double mass = (t * res) / (1.0 + entry.iter) / neighbors.size();
            residuals.put(entry, 0.0);
            for (BaseNode n : neighbors) {
                Entry next = new Entry(n, entry.iter + 1);
                if (next.iter == k) {
                    heat.put(next.node, heat.getOrDefault(next.node, 0.0) + res / neighbors.size());
                    continue;
                }
                if (!residuals.containsKey(next)) {
                    residuals.put(next, 0.0);
                }
                double threshold = Math.exp(t) * EPSILON * getNeighbors(n).size();
                threshold /= (k * psis[next.iter]);
                if (residuals.get(next) < threshold && residuals.get(next) + mass >= threshold) {
                    queue.add(entry);
                }
                residuals.put(next, residuals.get(next) + mass);
            }
        }
        for (Entry n: residuals.keySet()){
            heats.put(n.node.getId(), residuals.get(n) + heat.getOrDefault(n.node, 0.0));
        }
        for (BaseEdge e : merged.summary.getEdges()) {
            double sourceHeat = heats.getOrDefault(e.getSource().getId(), 0.0);
            double targetHeat = heats.getOrDefault(e.getTarget().getId(), 0.0);
            double edgeHeat = harmonicMean(sourceHeat, targetHeat);
            if (edgeHeat > 0) {
                merged.weights.put(e, edgeHeat);
            }
        }
    }

    private double[] computePsis() {
        double[] psis = new double[k+1];
        psis[k] = 1.0;
        for (int i = k-1; i > 0; i--){
            psis[i] = psis[i+1] * t / (1.0 + i) + 1;
        }
        return psis;
    }

    private Set<BaseNode> getNeighbors(BaseNode node) {
        Set<BaseNode> nodes = Dataset.I.getGraph().outEdgesFor(node.getId()).stream()
                .map(BaseEdge::getTarget).collect(Collectors.toSet());
        nodes.addAll(Dataset.I.getGraph().inEdgesFor(node.getId()).stream()
                .map(BaseEdge::getSource).collect(Collectors.toList()));
        return nodes;
    }
    
    private class Entry{
        int iter;
        BaseNode node;
        Entry(BaseNode node, int iter){
            this.iter = iter;
            this.node = node;
        }

        @Override
        public int hashCode(){
            return Integer.valueOf(iter).hashCode() + node.hashCode();
        }
    }

    public double harmonicMean(double h1, double h2){
        return 2 * h1 * h2 / (h1 + h2);
    }
}
