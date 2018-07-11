package summary.merging;

import encoding.SummaryEncoder;
import evaluation.Benchmarkable;
import graph.*;

import java.util.*;

/**
 * Created by lukas on 05.07.18.
 */
public class RegularizedMergedSummary implements Benchmarkable {

    public BaseGraph original;
    public BaseGraph summary;
    private long sizeLimit;
    private String method;

    private List<BaseNode> nodes;
    private Random random = new Random(0);

    private Map<BaseEdge, Double> weights = new HashMap<>();
    private Map<BaseEdge, Integer> actual = new HashMap<>();

    public double lastObjective;
    SummaryEncoder se = new SummaryEncoder();

    public RegularizedMergedSummary(BaseGraph originalGraph, String method, long sizeLimit){
        this.original = originalGraph;
        this.summary = new BaseGraph();
        this.sizeLimit = sizeLimit;
        this.method = method;
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
        initializeEdgeMetaData(queries);

        condenseUnusedNodes(queries);

        SummaryEncoder se = new SummaryEncoder();
        System.out.println(se.encode(summary));
        while (summary.getNodes().size() > 1 && size() > sizeLimit){
            merge();
            System.out.println(se.encode(summary) + " " + lastObjective + " " + summary.getNodes().size() + " " + summary.getEdges().size());
        }
    }

    private void initializeEdgeMetaData(Map<BaseGraph, List<Map<String, String>>> queries) {
        for (BaseEdge e : summary.getEdges()) {
            actual.put(e, 1);
        }
        for (BaseGraph query : queries.keySet()) {
            for (Map<String, String> result : queries.get(query)) {
                for (BaseEdge queryEdge : query.getEdges()) {
                    BaseEdge resultEdge = findResultEdge(queryEdge, result);
                    double oldWeight = weights.getOrDefault(resultEdge, 0.0);
                    weights.put(resultEdge, oldWeight + 1);
                    addWeightToNeighbors(resultEdge, resultEdge.getSource());
                    addWeightToNeighbors(resultEdge, resultEdge.getTarget());
                }
            }
        }

    }

    private void addWeightToNeighbors(BaseEdge resultEdge, BaseNode node) {
        for (BaseEdge e: summary.outEdgesFor(node.getId())){
            if (e != resultEdge){
                weights.put(e, 0.25);
            }
        }
        for (BaseEdge e: summary.inEdgesFor(node.getId())){
            if (e != resultEdge){
                weights.put(e, 0.25);
            }
        }
    }

    private void condenseUnusedNodes(Map<BaseGraph, List<Map<String, String>>> queries) {
        Set<Integer> usedNodes = new HashSet<>();
        for (List<Map<String,String>> queryResults: queries.values()){
            for (Map<String, String> result: queryResults){
                for (String nodeLabel: result.values()){
                    usedNodes.add(Dataset.I.IDFrom(nodeLabel));
                }
            }
        }

        BaseNode condenseNode = summary.addNode(Integer.MIN_VALUE, "");
        for (BaseNode n: new ArrayList<>(summary.getNodes())){
            HashSet<Integer> neighborhood = new HashSet<>(n.getContainedNodes());
            if (se.encode(summary) < sizeLimit){
                break;
            }
            neighborhood.retainAll(usedNodes);
            if (n.getId() != condenseNode.getId() && neighborhood.isEmpty()){
                mergeNodes(condenseNode.getId(), n.getId());
            }
        }
    }

    private BaseEdge findResultEdge(BaseEdge queryEdge, Map<String, String> result) {
        String sourceLabel = result.get(Dataset.I.labelFrom(queryEdge.getSource().getId()));
        String targetLabel = result.get(Dataset.I.labelFrom(queryEdge.getTarget().getId()));
        for (BaseEdge e: summary.outEdgesFor(Dataset.I.IDFrom(sourceLabel))){
            if (e.getTarget().getId() == Dataset.I.IDFrom(targetLabel) && e.getLabel().equals(queryEdge.getLabel())){
                return e;
            }
        }
        return null;
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
        System.out.println("merge: " + destinationID  + "   " + nodeToMergeID);
        for (BaseEdge e: summary.outEdgesFor(nodeToMergeID)){
            BaseEdge equivalent = findEquivalentEdge(e, destinationID, true);
            if (equivalent == null){
                addNewMergeEdge(e, destinationID, true);
            } else{
                combineEdges(equivalent, e);
            }
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
            weights.remove(e);
            actual.remove(e);
        }
        summary.nodeWithId(destinationID).getContainedNodes()
                .addAll(summary.nodeWithId(nodeToMergeID).getContainedNodes());
        summary.removeNode(nodeToMergeID);
    }

    private int[] mergeRandomized() {
        double bestObjective = -1.0;
        int[] pair = new int[]{-1, -1};
        if (nodes == null) {
            nodes = new ArrayList<>(summary.getNodes());
        }
        for (int i = 0; i < 5; i++){
            BaseNode n1 = nodes.get(random.nextInt(nodes.size()));
            Set<BaseNode> candidates = new HashSet<>();
            for (BaseEdge e: summary.outEdgesFor(n1.getId())){
                for (BaseEdge e2: summary.outEdgesFor(e.getTarget().getId())){
                    candidates.add(e2.getTarget());
                }
                for (BaseEdge e2: summary.inEdgesFor(e.getTarget().getId())){
                    candidates.add(e2.getSource());
                }
            }
            for (BaseEdge e: summary.inEdgesFor(n1.getId())){
                for (BaseEdge e2: summary.outEdgesFor(e.getTarget().getId())){
                    candidates.add(e2.getTarget());
                }
                for (BaseEdge e2: summary.inEdgesFor(e.getTarget().getId())){
                    candidates.add(e2.getSource());
                }
            }
            for (BaseNode n2: candidates){
                double objective = testMerge(n1, n2);
                if (objective == 1.0){
                    lastObjective = 1.0;
                    return new int[]{n1.getId(), n2.getId()};
                }
                if (objective > bestObjective){
                    bestObjective = objective;
                    pair = new int[]{n1.getId(), n2.getId()};
                }
            }
        }
        return pair;
    }

    private int[] mergeGlobal() {
        double bestObjective = -1.0;
        int[] pair = new int[] {-1, -1};
        for (BaseNode n1: summary.getNodes()){
            for (BaseNode n2: summary.getNodes()){
                if (n1.getId() <= n2.getId() || n1.getId() == Integer.MIN_VALUE || n2.getId() == Integer.MIN_VALUE){
                    continue;
                }
                double testObjective = testMerge(n1, n2);
                if (testObjective == 1.0){
                    lastObjective = testObjective;
                    return new int[]{n1.getId(), n2.getId()};
                }
                if (testObjective > bestObjective){
                    bestObjective = testObjective;
                    this.lastObjective = testObjective;
                    pair = new int[]{n1.getId(), n2.getId()};
                }
            }
        }
        return pair;
    }

    private double testMerge(BaseNode n1, BaseNode n2) {
        if (n1.getId() == n2.getId()){
            return -1;
        }
        Set<Integer> backupContained2 = new HashSet<>(n2.getContainedNodes());

        n2.getContainedNodes().clear();
        n1.getContainedNodes().addAll(backupContained2);

        for (BaseEdge e: summary.outEdgesFor(n2.getId())){
            BaseEdge equivalent = findEquivalentEdge(e, n1.getId(), true);
            if (equivalent == null){
                addNewMergeEdge(e, n1.getId(), true);
            }
            else{
                combineEdges(equivalent, e);
            }
        }

        for (BaseEdge e: summary.inEdgesFor(n2.getId())){
            if (e.getSource() == e.getTarget()) continue;
            BaseEdge equivalent = findEquivalentEdge(e, n1.getId(), false);
            if (equivalent == null){
                addNewMergeEdge(e, n1.getId(), false);
            }
            else{
                combineEdges(equivalent, e);
            }
        }

        double objective = computeObjective();

        n1.getContainedNodes().removeAll(backupContained2);
        n2.getContainedNodes().addAll(backupContained2);

        for (BaseEdge e: summary.outEdgesFor(n2.getId())){
            BaseEdge equivalent = findEquivalentEdge(e, n1.getId(), true);
            if (actual.get(e) < actual.get(equivalent)){
                actual.put(equivalent, actual.get(equivalent) - actual.get(e));
            } else {
                summary.removeEdge(equivalent);
                weights.remove(equivalent);
                actual.remove(equivalent);
            }
        }
        for (BaseEdge e: summary.inEdgesFor(n2.getId())){
            if (e.getSource() == e.getTarget()) continue;
            BaseEdge equivalent = findEquivalentEdge(e, n1.getId(), false);
            if (actual.get(e) < actual.get(equivalent)){
                actual.put(equivalent, actual.get(equivalent) - actual.get(e));
            } else {
                summary.removeEdge(equivalent);
                weights.remove(equivalent);
                actual.remove(equivalent);
            }
        }
        return objective;
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
        if(!actual.containsKey(equivalent) || !actual.containsKey(e)){
            System.out.println(equivalent + "||||" + e);
        }
        if(!actual.containsKey(equivalent)){
            System.out.println(equivalent);
        }
        if(!actual.containsKey(e)){
            System.out.println(e);
        }
        actual.put(equivalent, actual.get(equivalent) + actual.get(e));
    }

    private double computeObjective() {
        double objective = 0.0;
        double totalWeight = 0.0;
        for (BaseEdge e: weights.keySet()){
            if (e.getSource().getContainedNodes().isEmpty() || e.getTarget().getContainedNodes().isEmpty()){
                continue;
            }
            objective += weights.get(e) * actual.get(e) / e.size();
            totalWeight += weights.get(e);
        }
        return  objective / totalWeight;
    }

    @Override
    public long size() {
        return new SummaryEncoder().encode(summary);
    }

}
