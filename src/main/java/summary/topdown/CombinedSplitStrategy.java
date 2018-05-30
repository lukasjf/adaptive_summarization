package summary.topdown;

import graph.BaseEdge;
import graph.BaseNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lukas on 20.04.18.
 */
public class CombinedSplitStrategy implements SplitStrategy{

    private ExistentialSplitStrategy existential = new ExistentialSplitStrategy();
    private VarianceSplitStrategy variance = new VarianceSplitStrategy();

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

        if (sourceConns.values().contains(0) || targetConns.values().contains(0)){
            return existential.split(summary, criticalEdge, sourceConns, targetConns);
        } else {
            return variance.split(summary, criticalEdge, sourceConns, targetConns);
        }
    }
}
