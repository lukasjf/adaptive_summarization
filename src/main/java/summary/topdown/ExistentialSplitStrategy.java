package summary.topdown;

import graph.BaseEdge;
import graph.BaseNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by lukas on 14.03.18.
 */
public class ExistentialSplitStrategy implements SplitStrategy {

    @Override
    public BaseNode[] split(HeuristicSummary summary, BaseEdge criticalEdge) {
        Map<Integer, Long> sourceConns = new HashMap<>();
        Map<Integer, Long> targetConns = new HashMap<>();

        for (int id : criticalEdge.getSource().getContainedNodes()) {
            long connectivity = summary.graph.outEdgesFor(id).stream().filter(e ->
                    criticalEdge.getLabel().equals(e.getLabel()) && criticalEdge.getTarget().match(e.getTarget())).count();
            sourceConns.put(id, connectivity);
        }

        for (int id : criticalEdge.getTarget().getContainedNodes()) {
            long connectivity = summary.graph.inEdgesFor(id).stream().filter(e ->
                    criticalEdge.getLabel().equals(e.getLabel()) && criticalEdge.getSource().match(e.getSource())).count();
            targetConns.put(id, connectivity);
        }

        return split(summary, criticalEdge, sourceConns, targetConns);
    }

    public BaseNode[] split(HeuristicSummary summary, BaseEdge criticalEdge, Map<Integer, Long> sourceConns, Map<Integer, Long> targetConns) {
        double sourceSupport = sourceConns.values().stream().filter(b -> b > 0).count() / 1.0 / sourceConns.size();
        double targetSupport = targetConns.values().stream().filter(b -> b > 0).count() / 1.0 / targetConns.size();

        BaseNode splitNode = sourceSupport < targetSupport ? criticalEdge.getSource() : criticalEdge.getTarget();
        Map<Integer, Long> conns = sourceSupport < targetSupport ? sourceConns : targetConns;

        Set<Integer> nodesWithEdge, nodesWithoutEdge;

        nodesWithEdge = conns.keySet().stream().filter(id -> conns.get(id) > 0).collect(Collectors.toSet());
        nodesWithoutEdge = conns.keySet().stream().filter(id -> conns.get(id) == 0).collect(Collectors.toSet());

        if (nodesWithEdge.isEmpty() || nodesWithoutEdge.isEmpty()) {
            return new BaseNode[]{};
        } else {
            BaseNode newNode1 = new BaseNode(summary.getNewNodeID());
            newNode1.getContainedNodes().addAll(nodesWithEdge);
            BaseNode newNode2 = new BaseNode(summary.getNewNodeID());
            newNode2.getContainedNodes().addAll(nodesWithoutEdge);
            return new BaseNode[]{splitNode, newNode1, newNode2};
        }
    }
}
