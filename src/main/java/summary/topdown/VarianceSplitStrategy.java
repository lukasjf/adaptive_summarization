package summary.topdown;

import graph.BaseEdge;
import graph.BaseNode;

import java.util.*;

/**
 * Created by lukas on 20.04.18.
 */
public class VarianceSplitStrategy implements SplitStrategy{

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

    public BaseNode[] split(HeuristicSummary summary, BaseEdge criticalEdge, Map<Integer, Long> sourceConns, Map<Integer, Long> targetConns){
        double sourceMean = sourceConns.values().stream().mapToLong(Long::longValue).sum() / sourceConns.size();
        double sourceVariance = sourceConns.values().stream().map(l -> Math.pow(l - sourceMean, 2)).mapToDouble(Double::doubleValue).sum();
        double targetMean = targetConns.values().stream().mapToLong(Long::longValue).sum() / targetConns.size();
        double targetVariance = targetConns.values().stream().map(l -> Math.pow(l - targetMean, 2)).mapToDouble(Double::doubleValue).sum();

        BaseNode splitNode = sourceVariance > targetVariance ? criticalEdge.getSource() : criticalEdge.getTarget();
        Map<Integer, Long> conns = sourceVariance > targetVariance ? sourceConns : targetConns;

        List<Integer> newNode1Ids = new ArrayList<>();
        List<Integer> newNode2Ids = new ArrayList<>();

        long minConn = Collections.min(conns.values());
        long maxConn = Collections.max(conns.values());

        for (int id: conns.keySet()){
            if (maxConn - conns.get(id) <= (conns.get(id) - minConn)){
                newNode1Ids.add(id);
            } else {
                newNode2Ids.add(id);
            }
        }

        if (newNode1Ids.isEmpty() || newNode2Ids.isEmpty()) {
            return new BaseNode[]{};
        } else {
            BaseNode newNode1 = new BaseNode(summary.getNewNodeID());
            newNode1.getContainedNodes().addAll(newNode1Ids);
            BaseNode newNode2 = new BaseNode(summary.getNewNodeID());
            newNode2.getContainedNodes().addAll(newNode2Ids);
            return new BaseNode[]{splitNode, newNode1, newNode2};
        }
    }
}
