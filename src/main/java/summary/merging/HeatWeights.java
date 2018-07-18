package summary.merging;

import graph.BaseEdge;
import graph.BaseGraph;
import graph.BaseNode;
import graph.Dataset;

import java.util.*;

/**
 * Created by lukas on 12.07.18.
 */
public class HeatWeights implements WeightCreation {

    MergedSummary merged;

    Map<Integer, Double> heats = new HashMap<>();

    int k;
    double t;
    String aggregate;

    public HeatWeights(String aggregate, int k, double t){
        this.k = k;
        this.t = t;
        this.aggregate = aggregate;
    }

    @Override
    public void initializeWeights(MergedSummary merged, Map<BaseGraph, List<Map<String, String>>> queries) {
        this.merged = merged;

        for (List<Map<String,String>> results: queries.values()){
            for (Map<String, String> result: results){
                for (String r: result.values()){
                    int nodeID = Dataset.I.IDFrom(r);
                    heats.put(nodeID, heats.getOrDefault(nodeID, 0.0) + 1);
                }
            }
        }


        int counter = 0;
        Map<Integer, Integer> forward = new HashMap<>();
        Map<Integer, Integer> backward = new HashMap<>();

        Set<BaseNode> kNeighborHood = findNeighborhood(queries);
        System.out.println(merged.summary.getNodes().size()+" "+merged.blackList.size()+" "+kNeighborHood.size());

        double[][] uStart = new double[kNeighborHood.size()][1];
        double[][] L = new double[kNeighborHood.size()][kNeighborHood.size()];

        for (BaseNode n: kNeighborHood){
            forward.put(n.getId(), counter);
            backward.put(counter, n.getId());
            uStart[counter][0] = heats.getOrDefault(n.getId(), 0.0);
            int degree = merged.summary.outEdgesFor(n.getId()).size() + merged.summary.inEdgesFor(n.getId()).size();
            L[counter][counter] = 1.0/degree;
            counter++;
        }

        for (BaseNode n: kNeighborHood){
            int id = forward.get(n.getId());
            for (BaseEdge e: merged.summary.outEdgesFor(n.getId())){
                if (!kNeighborHood.contains(e.getTarget())){
                    continue;
                }
                int otherid = forward.get(e.getTarget().getId());
                L[id][otherid] = -1.0 * L[otherid][otherid];
                L[otherid][id] = -1.0 * L[id][id];
            }
        }

        for (int i = 0; i < kNeighborHood.size(); i++){
            L[i][i] = 1.0;
        }
        System.out.println("Matrices initialized");

        double[][] running = new double[kNeighborHood.size()][kNeighborHood.size()];
        double[][] exp = new double[kNeighborHood.size()][kNeighborHood.size()];
        for (int i = 0; i < exp.length; i++){
            exp[i][i] = 1.0;
            running[i][i] = 1.0;
        }
        double factorial = 1;
        double power = 1;
        System.out.println("Start exponential");
        for (int i = 1; i <= k; i++){
            running = mult(running, L);
            //running = running.mmul(L);
            factorial *= i;
            power *= -1 * t;
            add(exp, running, power / factorial);
            //exp = exp.add(running.mul(power/ factorial));
            System.out.println("Iteration complete");
        }

        System.out.println("Exponential Matrix computed");

        //DoubleMatrix heat = exp.mmul(uStart);
        double[][] heat = mult(exp, uStart);
        for (int i = 0; i < heat.length; i++){
            heats.put(backward.get(i), heat[i][0]);
        }

        for (BaseEdge e: merged.summary.getEdges()){
            if (kNeighborHood.contains(e.getSource()) || kNeighborHood.contains(e.getTarget())){
                double sourceHeat = heats.getOrDefault(e.getSource().getId(), 0.0);
                double targetHeat = heats.getOrDefault(e.getTarget().getId(), 0.0);
                double edgeHeat = aggregate(sourceHeat, targetHeat);
                if (edgeHeat > 0){
                    merged.weights.put(e, edgeHeat);
                }
            }
        }
        System.out.println("Weights Computed");
    }

    private Set<BaseNode> findNeighborhood(Map<BaseGraph, List<Map<String, String>>> queries) {
        Set<BaseNode> nb = new HashSet<>();

        for (List<Map<String, String>> results: queries.values()){
            for (Map<String, String> result: results){
                for (String res: result.values()){
                    BaseNode node = merged.summary.nodeWithId(Dataset.I.IDFrom(res));
                    if (!merged.blackList.contains(node)){
                        nb.add(node);
                    }
                }
            }
        }
        Set<BaseNode> expandNodes = new HashSet<>(nb);
        for (int i = 0; i < k; i++){
            Set<BaseNode> newNodes = new HashSet<>();
            for (BaseNode n: expandNodes){
                for (BaseEdge e: merged.summary.outEdgesFor(n.getId())){
                    if (!merged.blackList.contains(e.getTarget())){
                        newNodes.add(e.getTarget());
                    }
                }
                for (BaseEdge e: merged.summary.inEdgesFor(n.getId())){
                    if (!merged.blackList.contains(e.getSource())) {
                        newNodes.add(e.getSource());
                    }
                }
            }
            expandNodes = new HashSet<>(newNodes);
            nb.addAll(newNodes);
        }
        return nb;
    }

    public double aggregate(double h1, double h2){
        switch (aggregate){
            case "harmonic":
                return harmonicMean(h1, h2);
            default:
                return geometricMean(h1, h2);
        }
    }

    public double harmonicMean(double h1, double h2){
        return 2 * h1 + h2 / (h1 + h2);
    }

    public double geometricMean(double h1, double h2){
        return Math.sqrt(h1 * h2);
    }

    public double[][] mult(double[][] a, double[][]b){
        double[][] result = new double[a.length][b[0].length];
        for (int i = 0; i < result.length; i++){
            for (int k = 0; k < a.length; k++){
                if (a[i][k] != 0){
                    for (int j = 0; j < b[0].length; j++){
                        result[i][j] += a[i][k] * b[k][j];
                    }
                }
            }
        }
        return result;
    }

    public void add(double[][] target, double[][] add, double scale){
        for (int i = 0; i < target.length; i++){
            for (int j = 0; j < target[0].length; j++){
                target[i][j] += add[i][j] * scale;
            }
        }
    }
}
