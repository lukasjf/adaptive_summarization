package summary.topdown;

import encoding.SummaryEncoder;
import evaluation.Benchmarkable;
import graph.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by lukas on 12.03.18.
 */
public class HeuristicSummary implements Benchmarkable {

    private int newNodeCounter = 2;

    BaseGraph graph;
    BaseGraph summary;
    int sizeLimit;

    public HeuristicSummary(BaseGraph graph, int sizeLimit){
        this.graph = graph;
        this.sizeLimit = sizeLimit;
        summary = new BaseGraph();
        Set<Integer> allIds = graph.getNodes().stream().map(BaseNode::getId).collect(Collectors.toSet());
        summary.addNode(1, "");
        summary.getIdMapping().get(1).getContainedNodes().addAll(allIds);
        Set<String> edgeLabels = graph.getEdges().stream().map(BaseEdge::getLabel).collect(Collectors.toSet());
        for (String edgeLabel: edgeLabels){
            addPotentialEdge(1, 1, edgeLabel);
        }
    }

    public int getNewNodeID(){
        return newNodeCounter++;
    }

    @Override
    public List<Map<String, String>> query(BaseGraph query){
        SubgraphIsomorphism iso = new SubgraphIsomorphism();
        List<Map<String,String>> results = iso.query(query, summary, false);
        updateEdgeLosses(iso.matchings);
        return results;
    }

    private void updateEdgeLosses(List<Map<BaseEdge, BaseEdge>> matchings) {
        for (Map<BaseEdge, BaseEdge> match: matchings){
            for (BaseEdge queryEdge: match.keySet()){
                if (queryEdge.getSource().isVariable() || queryEdge.getTarget().isVariable()){
                    BaseEdge summaryEdge = match.get(queryEdge);
                    double edgeLoss = (double) summaryEdge.bookkeeping.getOrDefault("loss", 0.0);
                    double edgeSupport = (double) summaryEdge.bookkeeping.getOrDefault("support", 1.0);
                    match.get(queryEdge).bookkeeping.put("loss", edgeLoss + (1/edgeSupport-1));
                }
            }
        }
    }

    public void split(String choiceMethod, String splitMethod){
        SplitChoiceStrategy choice = SplitChoiceStrategy.algorithms.get(choiceMethod);
        choice.initialize(this);
        while (choice.hasNext()){
            BaseEdge criticalEdge = SplitChoiceStrategy.algorithms.get(choiceMethod).next();
            BaseNode[] splittedNodes = SplitStrategy.algorithms.get(splitMethod).split(this, criticalEdge);
            if (splittedNodes.length == 0){
                // split did not work
                continue;
            } else{
                // 3 nodes: first the splitnode, then the two new nodes
                // create new nodes and make new edges
                BaseNode splitNode = splittedNodes[0];
                BaseNode newNode1 = splittedNodes[1];
                BaseNode newNode2 = splittedNodes[2];
                summary.addNode(newNode1.getId(), "");
                summary.nodeWithId(newNode1.getId()).getContainedNodes().addAll(newNode1.getContainedNodes());
                summary.addNode(newNode2.getId(), "");
                summary.nodeWithId(newNode2.getId()).getContainedNodes().addAll(newNode2.getContainedNodes());

                checkEdges(splitNode.getId(), newNode1.getId(), newNode2.getId());
                summary.removeNode(splitNode.getId());
                break;
            }
        }
    }

    private void checkEdges(int splitId, int id1, int id2) {
        List<BaseEdge> toDoEdges = summary.outEdgesFor(splitId);
        toDoEdges.addAll(summary.inEdgesFor(splitId));

        for (BaseEdge edge: toDoEdges){
            if (edge.getSource().getId() == splitId && edge.getTarget().getId() == splitId){
                addPotentialEdge(id1, id1, edge.getLabel());
                addPotentialEdge(id1, id2, edge.getLabel());
                addPotentialEdge(id2, id1, edge.getLabel());
                addPotentialEdge(id2, id2, edge.getLabel());
            } else if (edge.getSource().getId() == splitId){
                addPotentialEdge(id1, edge.getTarget().getId(), edge.getLabel());
                addPotentialEdge(id2, edge.getTarget().getId(), edge.getLabel());
            } else {
                addPotentialEdge(edge.getSource().getId(), id1, edge.getLabel());
                addPotentialEdge(edge.getSource().getId(), id2, edge.getLabel());
            }
        }
    }

    private void addPotentialEdge(int source, int target, String label){
        long support = 0;
        for (int nodeId: summary.nodeWithId(source).getContainedNodes()){
            support += graph.outEdgesFor(nodeId).stream().filter(e ->
                    label.equals(e.getLabel()) && summary.nodeWithId(target).match(e.getTarget())).count();
        }
        if (support > 0){
            BaseEdge e = summary.addEdge(source, target, label);
            if (e != null) {
                e.bookkeeping.put("support", support / 1.0 / summary.nodeWithId(source).size() / summary.nodeWithId(target).size());
            }
        }
    }

    @Override
    public void train(Map<BaseGraph, List<Map<String, String>>> queries) {
        int oldSize = 1;
        SummaryEncoder encoder = new SummaryEncoder();
        while(encoder.encode(summary) < sizeLimit){
            System.out.println("new Round " + summary.getNodes().size() + " " + encoder.encode(summary));
            queries.keySet().forEach(this::query);
            split("loss", "variance");
            summary.getEdges().forEach(e -> e.bookkeeping.put("loss", 0.0));
            if (summary.getNodes().size() == oldSize){
                break;
            } else{
                oldSize = summary.getNodes().size();
            }
        }
    }

    @Override
    public long size() {
        return new SummaryEncoder().encode(summary);
    }

    public BaseGraph getGraph() {
        return graph;
    }

    public BaseGraph getSummary() {
        return summary;
    }
}
