package summary.merging;

import encoding.SummaryEncoder;
import evaluation.Benchmarkable;
import graph.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by lukas on 25.06.18.
 */
public class Stupid2 implements Benchmarkable {

    public BaseGraph original;
    public BaseGraph summary;
    private long sizeLimit;

    Map<Integer, Double> heats = new HashMap<>();

    SummaryEncoder se = new SummaryEncoder();
    double EPSILON = 1.0E-4;


    int k;
    double t;

    public Stupid2(BaseGraph originalGraph, long sizeLimit, double t){
        this.original = originalGraph;
        this.summary = new BaseGraph();
        this.sizeLimit = sizeLimit;
        this.t = t;
        this.k = (int) (2 * t * Math.log(1/EPSILON)) + 1;
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
        for (int k = 0;pq.size() > 0;k++){
            if (k % 10000 == 0){
                System.out.print(".");
            }
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

        double[] psis = computePsis();

        Map<BaseNode, Double> heat = new HashMap<>();
        Map<Entry, Double> residuals = new HashMap<>();

        List<Entry> queue = new ArrayList<>();
        double heatsum = heats.values().stream().mapToDouble(d->d).sum();
        for (int key: heats.keySet()){
            Entry entry = new Entry(Dataset.I.getGraph().nodeWithId(key), 0);
            residuals.put(entry, heats.get(key)/heatsum);
            queue.add(entry);
        }

        while (!queue.isEmpty()){
            Entry entry = queue.remove(0);
            Set<BaseNode> neighbors = getNeighbors(entry.node);
            double res = residuals.get(entry);
            heat.put(entry.node, res + heat.getOrDefault(entry.node, 0.0));
            double mass = (t * res) / (1.0 + entry.iter) / neighbors.size();
            residuals.put(entry, 0.0);
            for (BaseNode n: neighbors){
                Entry next = new Entry(n, entry.iter + 1);
                if (next.iter == k){
                    heat.put(next.node, heat.getOrDefault(next.node, 0.0) + res/neighbors.size());
                    continue;
                }
                if (!residuals.containsKey(next)){
                    residuals.put(next, 0.0);
                }
                double threshold = Math.exp(t)*EPSILON*getNeighbors(n).size();
                threshold /= (k * psis[next.iter]);
                if (residuals.get(next) < threshold && residuals.get(next) + mass >= threshold){
                    queue.add(entry);
                }
                residuals.put(next, residuals.get(next) + mass);
            }
        }
        for (Entry n: residuals.keySet()){
            heats.put(n.node.getId(), residuals.get(n) + heat.getOrDefault(n.node, 0.0));
        }
        int i = 0;
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

    @Override
    public long size() {
        return new SummaryEncoder().encode(summary);
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


}
