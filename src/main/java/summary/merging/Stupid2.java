package summary.merging;

import encoding.SummaryEncoder;
import evaluation.Benchmarkable;
import graph.*;

import java.util.*;

/**
 * Created by lukas on 25.06.18.
 */
public class Stupid2 implements Benchmarkable {

    public BaseGraph original;
    public BaseGraph summary;
    private long sizeLimit;

    Map<Integer, Double> heats = new HashMap<>();

    SummaryEncoder se = new SummaryEncoder();
    int k;
    double t;

    public Stupid2(BaseGraph originalGraph, long sizeLimit, int k, double t){
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
        /*for (BaseNode n: original.getNodes()){
            summary.addNode(n.getId(), Dataset.I.labelFrom(n.getId()));
            summary.nodeWithId(n.getId()).getContainedNodes().addAll(n.getContainedNodes());
        }
        for (BaseEdge e: original.getEdges()){
            summary.addEdge(e.getSource().getId(), e.getTarget().getId(), e.getLabel());
        }*/

        initializeWeights(queries);

        PriorityQueue<BaseNode> pq = new PriorityQueue<>((o1, o2) ->
                -Double.compare(heats.getOrDefault(o1.getId(), 0.0), heats.getOrDefault(o2.getId(), 0.0)));
        pq.addAll(original.getNodes());
        summary.addNode(Integer.MIN_VALUE, "");


        Set<Integer> hottest = new HashSet<>();
        Map<Integer, List<BaseEdge>> ingoingFor = new HashMap<>();
        Map<Integer, Map<String, Integer>> ingoingLabelIndex = new HashMap<>();
        Map<Integer, List<BaseEdge>> outgoingFor = new HashMap<>();
        Map<Integer, Map<String, Integer>> outgoingLabelIndex = new HashMap<>();

        List<BaseNode> last = new ArrayList<>();

        int i = 0;
        while (size() + 40 + pq.size()*4 +
                ingoingLabelIndex.values().stream().mapToInt(Map::size).sum() * 16 +
                outgoingLabelIndex.values().stream().mapToInt(Map::size).sum() * 16 < sizeLimit ){
            BaseNode node = pq.poll();
            last.add(node);
            if (i++ % 1000 == 0){
                System.out.println(se.encode(summary) + " " + (40 + 4 * pq.size()) + " " +
                        ingoingLabelIndex.values().stream().mapToInt(Map::size).sum() * 16 + " " +
                        outgoingLabelIndex.values().stream().mapToInt(Map::size).sum() * 16);
            }
            hottest.add(node.getId());
            BaseNode newNode = summary.addNode(node.getId(), Dataset.I.labelFrom(node.getId()));
            newNode.getContainedNodes().add(node.getId());
            for (BaseEdge e: ingoingFor.getOrDefault(node.getId(), new ArrayList<>())){
                summary.addEdge(e.getSource().getId(), node.getId(), e.getLabel());
                int usage = outgoingLabelIndex.get(e.getSource().getId()).get(e.getLabel()) -1;
                if (usage == 0){
                    outgoingLabelIndex.get(e.getSource().getId()).remove(e.getLabel());
                } else{
                    outgoingLabelIndex.get(e.getSource().getId()).put(e.getLabel(), usage);
                }
            }
            ingoingFor.remove(node.getId());

            for (BaseEdge e: outgoingFor.getOrDefault(node.getId(), new ArrayList<>())){
                summary.addEdge(node.getId(), e.getTarget().getId(), e.getLabel());
                int usage = ingoingLabelIndex.get(e.getTarget().getId()).get(e.getLabel()) - 1;
                if (usage == 0){
                    ingoingLabelIndex.get(e.getTarget().getId()).remove(e.getLabel());
                } else {
                    ingoingLabelIndex.get(e.getTarget().getId()).put(e.getLabel(), usage);
                }
            }
            outgoingFor.remove(node.getId());


            outgoingLabelIndex.put(node.getId(), new HashMap<>());
            ingoingLabelIndex.put(node.getId(), new HashMap<>());
            for (BaseEdge e: original.outEdgesFor(node.getId())){
                if (!hottest.contains(e.getTarget().getId())){
                    if (!ingoingFor.containsKey(e.getTarget().getId())){
                        ingoingFor.put(e.getTarget().getId(), new ArrayList<>());
                    }
                    ingoingFor.get(e.getTarget().getId()).add(e);
                    int usage = outgoingLabelIndex.get(node.getId()).getOrDefault(e.getLabel(), 0) + 1;
                    outgoingLabelIndex.get(node.getId()).put(e.getLabel(), usage);
                }
            }

            for (BaseEdge e: original.inEdgesFor(node.getId())){
                if (!hottest.contains(e.getSource().getId())){
                    if (!outgoingFor.containsKey(e.getSource().getId())){
                        outgoingFor.put(e.getSource().getId(), new ArrayList<>());
                    }
                    outgoingFor.get(e.getSource().getId()).add(e);
                    int usage = ingoingLabelIndex.get(node.getId()).getOrDefault(e.getLabel(), 0) + 1;
                    ingoingLabelIndex.get(node.getId()).put(e.getLabel(), usage);
                }
            }
        }

        BaseNode largeNode = summary.addNode(Integer.MIN_VALUE, "");

        for (int id: ingoingLabelIndex.keySet()){
            for (String openEdgeLabel: ingoingLabelIndex.get(id).keySet()){
                summary.addEdge(Integer.MIN_VALUE, id, openEdgeLabel);
            }
        }
        for (int id: outgoingLabelIndex.keySet()){
            for (String openEdgeLabel: outgoingLabelIndex.get(id).keySet()){
                summary.addEdge(id, Integer.MIN_VALUE, openEdgeLabel);
            }
        }

        Set<String> superLabels = new HashSet<>();
        for (;pq.size() > 0;){
            BaseNode n = pq.poll();
            largeNode.getContainedNodes().add(n.getId());
            for (BaseEdge e: original.outEdgesFor(n.getId())){
                if (!hottest.contains(e.getTarget().getId()) && !superLabels.contains(e.getLabel())){
                    superLabels.add(e.getLabel());
                }
            }
            for (BaseEdge e: original.inEdgesFor(n.getId())){
                if (!hottest.contains(e.getSource().getId()) && !superLabels.contains(e.getLabel())){
                    superLabels.add(e.getLabel());
                }
            }
        }

        for (String label: superLabels){
            summary.addEdge(Integer.MIN_VALUE, Integer.MIN_VALUE, label);
        }


        while (size() > sizeLimit && summary.getNodes().size() > 1){
            BaseNode n = last.remove(last.size()-1);
            System.out.println("Merge again node " + n.getId());
            mergeNodes(Integer.MIN_VALUE, n.getId());
        }
        System.out.println("Nodes: " + summary.getNodes().size());
        System.out.println("Start Queries ");
    }

    private void mergeNodes(int destinationID, int nodeToMergeID) {
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

    private void initializeWeights(Map<BaseGraph, List<Map<String, String>>> queries) {
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
        System.out.println(original.getNodes().size()+ " " + kNeighborHood.size());

        double[][] uStart = new double[kNeighborHood.size()][1];
        double[][] L = new double[kNeighborHood.size()][kNeighborHood.size()];

        for (BaseNode n: kNeighborHood){
            forward.put(n.getId(), counter);
            backward.put(counter, n.getId());
            uStart[counter][0] = heats.getOrDefault(n.getId(), 0.0);
            int degree = original.outEdgesFor(n.getId()).size() + original.inEdgesFor(n.getId()).size();
            L[counter][counter] = 1.0/degree;
            counter++;
        }

        for (BaseNode n: kNeighborHood){
            int id = forward.get(n.getId());
            for (BaseEdge e: original.outEdgesFor(n.getId())){
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
                    BaseNode node = original.nodeWithId(Dataset.I.IDFrom(res));
                    if (!Dataset.I.blacklist.contains(node.getId())){
                        nb.add(node);
                    }
                }
            }
        }
        Set<BaseNode> expandNodes = new HashSet<>(nb);
        for (int i = 0; i < k; i++){
            Set<BaseNode> newNodes = new HashSet<>();
            for (BaseNode n: expandNodes){
                for (BaseEdge e: original.outEdgesFor(n.getId())){
                    if (!Dataset.I.blacklist.contains(e.getTarget().getId())){
                        newNodes.add(e.getTarget());
                    }
                }
                for (BaseEdge e: original.inEdgesFor(n.getId())){
                    if (!Dataset.I.blacklist.contains(e.getSource().getId())) {
                        newNodes.add(e.getSource());
                    }
                }
            }
            expandNodes = new HashSet<>(newNodes);
            nb.addAll(newNodes);
        }
        return nb;
    }

    private double[][] mult(double[][] a, double[][]b){
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
