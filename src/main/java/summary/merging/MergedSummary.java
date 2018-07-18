package summary.merging;

import encoding.SummaryEncoder;
import evaluation.Benchmarkable;
import graph.*;

import java.util.*;

/**
 * Created by lukas on 25.06.18.
 */
public class MergedSummary implements Benchmarkable {

    public BaseGraph original;
    public BaseGraph summary;
    private long sizeLimit;
    private String method;


    String mergeMethod = "diff";

    private int prunecounter = 0;
    private WeightCreation initializer;

    private Random random = new Random(0);

    Set<BaseNode> blackList = new HashSet<>();

    Map<Integer,Map<Integer,Double>> cache = new HashMap<>();

    Map<BaseEdge, Double> weights = new HashMap<>();
    Map<BaseEdge, Integer> actual = new HashMap<>();

    double totalWeight = 0.0;

    public double currentObjective = 1.0;
    public double mergeObjective = -1.0;

    SummaryEncoder se = new SummaryEncoder();
    DiffObjective diffO = new DiffObjective();

    public MergedSummary(BaseGraph originalGraph, String method, long sizeLimit, WeightCreation weights){
        this.original = originalGraph;
        this.summary = new BaseGraph();
        this.sizeLimit = sizeLimit;
        this.method = method;
        this.initializer = weights;
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
            cache.put(n.getId(), new HashMap<>());
        }
        for (BaseEdge e: original.getEdges()){
            summary.addEdge(e.getSource().getId(), e.getTarget().getId(), e.getLabel());
        }

        blackListNodes();

        initializeEdgeMetaData(queries);

        condenseUnusedNodes();

        SummaryEncoder se = new SummaryEncoder();
        System.out.println("Sumary size after pruning " + prunecounter + " elements: " + se.encode(summary));

        int i = 0;
        while (summary.getNodes().size() > 1 && size() > sizeLimit){
            currentObjective = computeObjective();
            if (i++ % 1000 == 0){
                System.out.println(se.encode(summary) + " " + currentObjective + " " + mergeObjective + " " + summary.getNodes().size() + " " + summary.getEdges().size());
            }
            merge();

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

    private void initializeEdgeMetaData(Map<BaseGraph, List<Map<String, String>>> queries) {
        for (BaseEdge e : summary.getEdges()) {
            actual.put(e, 1);
        }
        initializer.initializeWeights(this, queries);
        for (double w: weights.values()){
            totalWeight += w;
        }
    }

    private void condenseUnusedNodes() {
        BaseNode condenseNode = summary.addNode(Integer.MIN_VALUE, "");
        cache.put(Integer.MIN_VALUE, new HashMap<>());
        for (BaseNode n: new ArrayList<>(summary.getNodes())){
            if (n.getId() == condenseNode.getId()){
                continue;
            }
            if (se.encode(summary) < sizeLimit){
                break;
            }
            boolean prune = true;
            for (BaseEdge e: summary.outEdgesFor(n.getId())){
                if (weights.getOrDefault(e, 0.0) > 0.0){
                    prune = false;
                    break;
                }
            }
            if (!prune){
                continue;
            }
            for (BaseEdge e: summary.inEdgesFor(n.getId())){
                if (weights.getOrDefault(e, 0.0) > 0.0){
                    prune = false;
                    break;
                }
            }
            if (prune){
                //System.out.println("prune " + n.getId());
                prunecounter++;
                mergeNodes(condenseNode.getId(), n.getId());
            }
        }
    }

    private void merge() {
        int[] bestPair;
        if ("full".equals(method)){
            bestPair = mergeGlobal();
        }else{
            bestPair = mergeRandomized();
        }
        int destinationID = bestPair[0];
        int nodeToMergeID = bestPair[1];

        mergeNodes(destinationID, nodeToMergeID);
    }

    private void mergeNodes(int destinationID, int nodeToMergeID) {
        if (destinationID != Integer.MIN_VALUE){
            //System.out.println("merge: " + destinationID  + "   " + nodeToMergeID);
        }
        for (BaseEdge e: summary.outEdgesFor(nodeToMergeID)){
            BaseEdge equivalent = findEquivalentEdge(e, destinationID, true);
            if (equivalent == null){
                addNewMergeEdge(e, destinationID, true);
            } else{
                combineEdges(equivalent, e);
            }
            cache.get(e.getTarget().getId()).remove(nodeToMergeID);
            weights.remove(e);
            actual.remove(e);
        }
        for (BaseEdge e: summary.inEdgesFor(nodeToMergeID)){
            if (e.getSource() == e.getTarget()) continue;
            BaseEdge equivalent = findEquivalentEdge(e, destinationID, false);
            if (equivalent == null){
                addNewMergeEdge(e, destinationID, false);
            } else {
                combineEdges(equivalent, e);
            }
            cache.get(e.getSource().getId()).remove(nodeToMergeID);
            weights.remove(e);
            actual.remove(e);
        }
        summary.nodeWithId(destinationID).getContainedNodes()
                .addAll(summary.nodeWithId(nodeToMergeID).getContainedNodes());
        summary.removeNode(nodeToMergeID);

        for (BaseEdge e: summary.outEdgesFor(destinationID)){
            cache.get(e.getTarget().getId()).remove(destinationID);
        }

        for (BaseEdge e: summary.outEdgesFor(destinationID)){
            cache.get(e.getSource().getId()).remove(destinationID);
        }

        cache.get(destinationID).clear();
        cache.remove(nodeToMergeID);
    }

    private int[] mergeRandomized() {
        double bestObjective = -1.0;
        int[] pair = new int[]{-1, -1};
        List<BaseNode> nodes = new ArrayList<>(summary.getNodes());
        for (int i = 0; i < 5; i++){
            BaseNode n1 = nodes.get(random.nextInt(nodes.size()));
            while (blackList.contains(n1)){
                n1 = nodes.get(random.nextInt(nodes.size()));
            }
            Set<BaseNode> candidates = new HashSet<>();
            for (BaseEdge e: summary.outEdgesFor(n1.getId())){
                if (blackList.contains(e.getTarget())){
                    continue;
                }
                for (BaseEdge e2: summary.outEdgesFor(e.getTarget().getId())){
                    if (!blackList.contains(e2.getTarget())){
                        candidates.add(e2.getTarget());
                    }
                }
                for (BaseEdge e2: summary.inEdgesFor(e.getTarget().getId())){
                    if (!blackList.contains(e2.getSource())){
                        candidates.add(e2.getSource());
                    }
                }
            }
            for (BaseEdge e: summary.inEdgesFor(n1.getId())){
                if (blackList.contains(e.getSource())){
                    continue;
                }
                for (BaseEdge e2: summary.outEdgesFor(e.getSource().getId())){
                    if (!blackList.contains(e2.getTarget())){
                        candidates.add(e2.getTarget());
                    }
                }
                for (BaseEdge e2: summary.inEdgesFor(e.getSource().getId())){
                    if (!blackList.contains(e2.getSource())){
                        candidates.add(e2.getSource());
                    }
                }
            }
            //System.out.println("" + candidates.size() + " candidates");
            if (candidates.isEmpty()){
                return mergeRandomized();
            }
            for (BaseNode n2: candidates){
                double objective = testMerge(n1,n2);
                if (objective == 1.0){
                    System.out.println("perfect merge found");
                    mergeObjective = 1.0;
                    return new int[]{n1.getId(), n2.getId()};
                }
                if (objective > bestObjective){
                    bestObjective = objective;
                    pair = new int[]{n1.getId(), n2.getId()};
                }
            }
        }
        mergeObjective = bestObjective;
        return pair;
    }

    private int[] mergeGlobal() {
        double bestObjective = -1.0;
        int[] pair = new int[] {-1, -1};
        for (BaseNode n1: summary.getNodes()){
            for (BaseNode n2: summary.getNodes()){
                if (n1.getId() >= n2.getId() || n1.getId() == Integer.MIN_VALUE || n2.getId() == Integer.MIN_VALUE){
                    continue;
                }
                if (blackList.contains(n1) || blackList.contains(n2)){
                    continue;
                }
                double testObjective = testMerge(n1, n2);
                if (testObjective == 1.0){
                    mergeObjective = 1.0;
                    return new int[]{n1.getId(), n2.getId()};
                }
                if (testObjective > bestObjective){
                    bestObjective = testObjective;
                    pair = new int[]{n1.getId(), n2.getId()};
                }
            }
        }
        mergeObjective = bestObjective;
        return pair;
    }

    private double testMerge(BaseNode n1, BaseNode n2) {
        if (n1.getId() == n2.getId() || n1.getId() == Integer.MIN_VALUE || n2.getId() == Integer.MIN_VALUE){
            return -1;
        }
        if ("diff".equals(mergeMethod)){
            if (cache.get(n1.getId()).containsKey(n2.getId())){
                //System.out.print("*");
                return cache.get(n1.getId()).get(n2.getId());
            }

            double delta = diffO.getObjectiveDelta(this, n1, n2);
            cache.get(n1.getId()).put(n2.getId(), delta);
            cache.get(n2.getId()).put(n1.getId(), delta);
            //System.out.print(".");
            return currentObjective + delta;
        } else{
            Set<Integer> backupContained2 = new HashSet<>(n2.getContainedNodes());

            n2.getContainedNodes().clear();
            n1.getContainedNodes().addAll(backupContained2);

            for (BaseEdge e: summary.outEdgesFor(n2.getId())){
                mergeEdge(e, n1.getId(), true);
            }

            for (BaseEdge e: summary.inEdgesFor(n2.getId())){
                if (e.getSource() != e.getTarget()) {
                    mergeEdge(e, n1.getId(), false);
                }
            }

            double objective = computeObjective();

            n1.getContainedNodes().removeAll(backupContained2);
            n2.getContainedNodes().addAll(backupContained2);

            for (BaseEdge e: summary.outEdgesFor(n2.getId())){
                unmergeEdge(e, n1.getId(), true);
            }
            for (BaseEdge e: summary.inEdgesFor(n2.getId())){
                if (e.getSource() != e.getTarget()) {
                    unmergeEdge(e, n1.getId(), false);
                }
            }
            return objective;
        }
    }

    void mergeEdge(BaseEdge e, int nodeID, boolean isOutEdge){
        BaseEdge equivalent = findEquivalentEdge(e, nodeID, isOutEdge);
        if (equivalent == null){
            addNewMergeEdge(e, nodeID, isOutEdge);
        }
        else{
            combineEdges(equivalent, e);
        }
    }

    void unmergeEdge(BaseEdge e, int nodeID, boolean isOutEdge){
        BaseEdge equivalent = findEquivalentEdge(e, nodeID, isOutEdge);
        if (actual.get(e) < actual.get(equivalent)){
            actual.put(equivalent, actual.get(equivalent) - actual.get(e));
        } else {
            summary.removeEdge(equivalent);
            weights.remove(equivalent);
            actual.remove(equivalent);
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
        BaseEdge newEdge;
        if (e.getSource() == e.getTarget()){
            newEdge = summary.addEdge(mergeNodeID, mergeNodeID, e.getLabel());
        } else {
            if (isOutEdge) {
                newEdge = summary.addEdge(mergeNodeID, e.getTarget().getId(), e.getLabel());
            } else {
                newEdge = summary.addEdge(e.getSource().getId(), mergeNodeID, e.getLabel());
            }
        }
        if (weights.containsKey(e)){
            weights.put(newEdge, weights.get(e));
        }
        actual.put(newEdge, actual.get(e));
    }

    private void combineEdges(BaseEdge equivalent, BaseEdge e) {
        double newWeight = weights.getOrDefault(equivalent, 0.0) + weights.getOrDefault(e, 0.0);
        if (newWeight > 0){
            weights.put(equivalent, newWeight);
        }
        actual.put(equivalent, actual.get(equivalent) + actual.get(e));
    }

    private double computeObjective() {
        double objective = 0.0;
        double totalW = 0.0;
        for (BaseEdge e: weights.keySet()){
            if (e.getSource().getContainedNodes().isEmpty() || e.getTarget().getContainedNodes().isEmpty()){
                continue;
            }
            objective += weights.get(e) * actual.get(e) / e.size();
            totalW += weights.get(e);
        }
        totalWeight = totalW;
        return  objective / totalWeight;
    }

    @Override
    public long size() {
        return new SummaryEncoder().encode(summary);
    }

}
