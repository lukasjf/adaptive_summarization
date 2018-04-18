package summary.adaptive.heuristic;

import graph.BaseEdge;
import graph.BaseNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by lukas on 14.03.18.
 */
public class ExistentialSplitStrategy implements SplitStrategy{

    @Override
    public BaseNode[] split(HeuristicSummary summary, BaseEdge criticalEdge) {
        Map<Integer, Boolean> sourceConns = new HashMap<>();
        Map<Integer, Boolean> targetConns = new HashMap<>();

        for (int id : criticalEdge.getSource().getContainedNodes()) {
            boolean connectivity = summary.graph.outEdgesFor(id).stream().anyMatch(e ->
                    criticalEdge.getLabel().equals(e.getLabel()) && criticalEdge.getTarget().match(e.getTarget()));
            sourceConns.put(id, connectivity);
        }

        for (int id : criticalEdge.getTarget().getContainedNodes()) {
            boolean connectivity = summary.graph.inEdgesFor(id).stream().anyMatch(e ->
                    criticalEdge.getLabel().equals(e.getLabel()) && criticalEdge.getSource().match(e.getSource()));
            targetConns.put(id, connectivity);
        }

        double sourceSupport = sourceConns.values().stream().filter(b -> b).count() / 1.0 / sourceConns.size();
        double targetSupport = targetConns.values().stream().filter(b -> b).count() / 1.0 / targetConns.size();

        BaseNode splitNode = sourceSupport < targetSupport ? criticalEdge.getSource() : criticalEdge.getTarget();
        boolean isSourceSplit = sourceSupport < targetSupport;

        Set<Integer> nodesWithEdge, nodesWithoutEdge;

        if (isSourceSplit) {
            nodesWithEdge = sourceConns.keySet().stream().filter(id -> sourceConns.get(id)).collect(Collectors.toSet());
            nodesWithoutEdge = sourceConns.keySet().stream().filter(id -> !sourceConns.get(id)).collect(Collectors.toSet());
        } else {
            nodesWithEdge = targetConns.keySet().stream().filter(id -> targetConns.get(id)).collect(Collectors.toSet());
            nodesWithoutEdge = targetConns.keySet().stream().filter(id -> !targetConns.get(id)).collect(Collectors.toSet());
        }

        if (nodesWithEdge.isEmpty() || nodesWithoutEdge.isEmpty()){
            return new BaseNode[]{};
        }else{
            BaseNode newNode1 = new BaseNode(summary.getNewNodeID());
            newNode1.getContainedNodes().addAll(nodesWithEdge);
            BaseNode newNode2 = new BaseNode(summary.getNewNodeID());
            newNode2.getContainedNodes().addAll(nodesWithoutEdge);
            return new BaseNode[]{splitNode, newNode1, newNode2};
        }
    }

}
