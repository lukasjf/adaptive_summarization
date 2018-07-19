package summary.merging;

import encoding.SummaryEncoder;
import evaluation.Benchmarkable;
import graph.*;

import java.util.*;

/**
 * Created by lukas on 25.06.18.
 */
public class StupidMerge implements Benchmarkable {

    public BaseGraph original;
    public BaseGraph summary;
    private long sizeLimit;


    Set<BaseNode> blackList = new HashSet<>();

    Map<Integer, Double> heats = new HashMap<>();

    SummaryEncoder se = new SummaryEncoder();
    int k;
    double t;

    public StupidMerge(BaseGraph originalGraph, String method, long sizeLimit, int k, double t){
        this.original = originalGraph;
        this.summary = new BaseGraph();
        this.sizeLimit = sizeLimit;
        this.t = t;
        this.k = k;
    }

    @Override
    public List<Map<String, String>> query(BaseGraph query) {
        return new SubgraphIsomorphism().query(query, summary, false);
    }

    @Override
    public List<Map<String, String>> query(BaseGraph query, int timeout) {
        return new SubgraphIsomorphism(timeout).query(query, summary, false);
    }

    @Override
    public void train(Map<BaseGraph, List<Map<String, String>>> queries) {
        for (BaseNode n: original.getNodes()){
            summary.addNode(n.getId(), Dataset.I.labelFrom(n.getId()));
            summary.nodeWithId(n.getId()).getContainedNodes().addAll(n.getContainedNodes());
        }
        for (BaseEdge e: original.getEdges()){
            summary.addEdge(e.getSource().getId(), e.getTarget().getId(), e.getLabel());
        }

        blackListNodes();

        initializeWeights(queries);

        PriorityQueue<BaseNode> pq = new PriorityQueue<>(new Comparator<BaseNode>() {
            @Override
            public int compare(BaseNode o1, BaseNode o2) {
                return Double.compare(heats.getOrDefault(o1.getId(), 0.0), heats.getOrDefault(o2.getId(), 0.0));
            }
        });
        pq.addAll(summary.getNodes());
        summary.addNode(Integer.MIN_VALUE, "");

        int i = 0;
        while (summary.getNodes().size() > 1 && size() > sizeLimit){
            BaseNode node = pq.poll();
            if (i++ % 1000 == 0){
                System.out.println(se.encode(summary) + " " + heats.getOrDefault(node.getId(), 0.0)
                        + " " + summary.getNodes().size() + " " + summary.getEdges().size());
            }
            mergeNodes(Integer.MIN_VALUE, node.getId());
        }
    }

    private void blackListNodes() {
        PriorityQueue<BaseNode> pq = new PriorityQueue<>((n1, n2) -> {
            int degree1 = summary.outEdgesFor(n1.getId()).size() + summary.inEdgesFor(n1.getId()).size();
            int degree2 = summary.outEdgesFor(n2.getId()).size() + summary.inEdgesFor(n2.getId()).size();
            return Integer.compare(-degree1, -degree2);
        });
        pq.addAll(summary.getNodes());
        for (int i = 0; i < 0.01 * summary.getNodes().size(); i++){
            blackList.add(pq.poll());
        }
    }

    private void mergeNodes(int destinationID, int nodeToMergeID) {
        if (destinationID != Integer.MIN_VALUE){
            //System.out.println("merge: " + destinationID  + "   " + nodeToMergeID);
        }
        for (BaseEdge e: summary.outEdgesFor(nodeToMergeID)){
            BaseEdge equivalent = findEquivalentEdge(e, destinationID, true);
            if (equivalent == null){
                addNewMergeEdge(e, destinationID, true);
            }
        }
        for (BaseEdge e: summary.inEdgesFor(nodeToMergeID)){
            if (e.getSource() == e.getTarget()) continue;
            BaseEdge equivalent = findEquivalentEdge(e, destinationID, false);
            if (equivalent == null){
                addNewMergeEdge(e, destinationID, false);
            }
        }
        summary.nodeWithId(destinationID).getContainedNodes()
                .addAll(summary.nodeWithId(nodeToMergeID).getContainedNodes());
        summary.removeNode(nodeToMergeID);
    }


    void mergeEdge(BaseEdge e, int nodeID, boolean isOutEdge) {
        BaseEdge equivalent = findEquivalentEdge(e, nodeID, isOutEdge);
        if (equivalent == null) {
            addNewMergeEdge(e, nodeID, isOutEdge);
        }
    }

    private BaseEdge findEquivalentEdge(BaseEdge e, int otherNodeID, boolean isOutEdge) {
        if (e.getSource() == e.getTarget()){
            for (BaseEdge candidate: summary.outEdgesFor(otherNodeID)){
                if (candidate.getSource().getId() == otherNodeID && candidate.getTarget().getId() == otherNodeID
                        && e.getLabel().equals(candidate.getLabel())){
                    return candidate;
                }
            }
            return null;
        } else {
            List<BaseEdge> candidates = isOutEdge ?
                    summary.outEdgesFor(otherNodeID) : summary.inEdgesFor(otherNodeID);
            for (BaseEdge candidate : candidates) {
                BaseNode testNode = isOutEdge ? e.getTarget() : e.getSource();
                BaseNode otherTestNode = isOutEdge ? candidate.getTarget() : candidate.getSource();
                if (testNode.getId() == otherTestNode.getId() && e.getLabel().equals(candidate.getLabel())) {
                    return candidate;
                }
            }
            return null;
        }
    }

    private void addNewMergeEdge(BaseEdge e, int mergeNodeID,  boolean isOutEdge) {
        if (e.getSource() == e.getTarget()){
            summary.addEdge(mergeNodeID, mergeNodeID, e.getLabel());
        } else {
            if (isOutEdge) {
                summary.addEdge(mergeNodeID, e.getTarget().getId(), e.getLabel());
            } else {
                summary.addEdge(e.getSource().getId(), mergeNodeID, e.getLabel());
            }
        }
    }

    public void initializeWeights(Map<BaseGraph, List<Map<String, String>>> queries) {
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
        System.out.println(summary.getNodes().size()+" "+blackList.size()+" "+kNeighborHood.size());

        double[][] uStart = new double[kNeighborHood.size()][1];
        double[][] L = new double[kNeighborHood.size()][kNeighborHood.size()];

        for (BaseNode n: kNeighborHood){
            forward.put(n.getId(), counter);
            backward.put(counter, n.getId());
            uStart[counter][0] = heats.getOrDefault(n.getId(), 0.0);
            int degree = summary.outEdgesFor(n.getId()).size() + summary.inEdgesFor(n.getId()).size();
            L[counter][counter] = 1.0/degree;
            counter++;
        }

        for (BaseNode n: kNeighborHood){
            int id = forward.get(n.getId());
            for (BaseEdge e: summary.outEdgesFor(n.getId())){
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
        for (int i = 1; i <= 4; i++){
            running = mult(L, running);
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
        System.out.println("Weights Computed");
    }

    private Set<BaseNode> findNeighborhood(Map<BaseGraph, List<Map<String, String>>> queries) {
        Set<BaseNode> nb = new HashSet<>();

        for (List<Map<String, String>> results: queries.values()){
            for (Map<String, String> result: results){
                for (String res: result.values()){
                    BaseNode node = summary.nodeWithId(Dataset.I.IDFrom(res));
                    if (!blackList.contains(node)){
                        nb.add(node);
                    }
                }
            }
        }
        Set<BaseNode> expandNodes = new HashSet<>(nb);
        for (int i = 0; i < k; i++){
            Set<BaseNode> newNodes = new HashSet<>();
            for (BaseNode n: expandNodes){
                for (BaseEdge e: summary.outEdgesFor(n.getId())){
                    if (!blackList.contains(e.getTarget())){
                        newNodes.add(e.getTarget());
                    }
                }
                for (BaseEdge e: summary.inEdgesFor(n.getId())){
                    if (!blackList.contains(e.getSource())) {
                        newNodes.add(e.getSource());
                    }
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

    @Override
    public long size() {
        return new SummaryEncoder().encode(summary);
    }

}
