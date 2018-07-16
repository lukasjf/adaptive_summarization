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

    public HeatWeights(int k, double t){
        this.k = k;
        this.t = t;
    }

    @Override
    public void initializeWeights(MergedSummary merged, Map<BaseGraph, List<Map<String, String>>> queries) {
        this.merged = merged;
        for (BaseEdge e : merged.summary.getEdges()) {
            merged.actual.put(e, 1);
        }

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
        System.out.println(kNeighborHood.size());

        //SimpleMatrix uStart = new SimpleMatrix(kNeighborHood.size(), 1);
        //SimpleMatrix L = new SimpleMatrix(kNeighborHood.size(), kNeighborHood.size());
        //DoubleMatrix uStart = new DoubleMatrix(kNeighborHood.size(), 1);
        //DoubleMatrix L = new DoubleMatrix(kNeighborHood.size(), kNeighborHood.size());

        double[][] uStart = new double[kNeighborHood.size()][1];
        double[][] L = new double[kNeighborHood.size()][kNeighborHood.size()];

        for (BaseNode n: kNeighborHood){
            forward.put(n.getId(), counter);
            backward.put(counter, n.getId());
            uStart[counter][0] = heats.getOrDefault(n, 0.0);
            //uStart.put(counter, 0, heats.getOrDefault(n, 0.0));
            int degree = merged.summary.outEdgesFor(n.getId()).size() + merged.summary.inEdgesFor(n.getId()).size();
            L[counter][counter] = 1.0/degree;
            //L.put(counter, counter, 1.0 / degree);
            counter++;
        }

        for (BaseNode n: kNeighborHood){
            int id = forward.get(n.getId());
            for (BaseEdge e: Dataset.I.getGraph().outEdgesFor(n.getId())){
                if (!kNeighborHood.contains(e.getTarget())){
                    continue;
                }
                int otherid = forward.get(e.getTarget().getId());
                if (L[otherid][otherid] > 0.01){
                    L[id][otherid] = -1.0 * L[otherid][otherid];
                }
                if (L[id][id] > 0.01){
                    L[otherid][id] = -1.0 * L[id][id];
                }
                //L.put(id, otherid, 1.0 / L.get(otherid, otherid));
                //L.put(otherid, id, 1.0 / L.get(id, id));
            }
        }

        for (int i = 0; i < kNeighborHood.size(); i++){
            L[i][i] = 1.0;
        }

        System.out.println("Matrices initialized");


        //SimpleMatrix exp = SimpleMatrix.identity(kNeighborHood.size());
        //SimpleMatrix running = SimpleMatrix.identity(kNeighborHood.size());
        //DoubleMatrix running = new DoubleMatrix(kNeighborHood.size(), kNeighborHood.size());
        //DoubleMatrix exp = DoubleMatrix.eye(kNeighborHood.size());
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
            int nodeId = backward.get(i);
            int degree = merged.summary.outEdgesFor(nodeId).size() + merged.summary.inEdgesFor(nodeId).size();
            if (degree < 100){
                heats.put(backward.get(i), heat[i][0]);
            }
        }

        for (BaseEdge e: merged.summary.getEdges()){
            double sourceHeat = heats.getOrDefault(e.getSource().getId(), 0.0);
            double targetHeat = heats.getOrDefault(e.getTarget().getId(), 0.0);
                merged.weights.put(e, (sourceHeat + targetHeat) / 2.0);
        }
        System.out.println("Weights Computed");
    }

    private Set<BaseNode> findNeighborhood(Map<BaseGraph, List<Map<String, String>>> queries) {
        Set<BaseNode> nb = new HashSet<>();

        for (List<Map<String, String>> results: queries.values()){
            for (Map<String, String> result: results){
                for (String res: result.values()){
                    BaseNode node = Dataset.I.getGraph().getLabelMapping().get(res);
                    nb.add(node);
                }
            }
        }
        Set<BaseNode> expandNodes = new HashSet<>(nb);
        for (int i = 0; i < k; i++){
            Set<BaseNode> newNodes = new HashSet<>();
            for (BaseNode n: expandNodes){
                for (BaseEdge e: Dataset.I.getGraph().outEdgesFor(n.getId())){
                    newNodes.add(e.getTarget());
                }
                for (BaseEdge e: Dataset.I.getGraph().inEdgesFor(n.getId())){
                    newNodes.add(e.getSource());
                }
            }
            expandNodes = new HashSet<>(newNodes);
            nb.addAll(newNodes);
        }
        return nb;
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
