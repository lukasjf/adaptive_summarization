package summary.adaptive.heuristic;

import graph.BaseEdge;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by lukas on 18.04.18.
 */
public class LossChoice implements SplitChoiceStrategy{

    private List<BaseEdge> candidates;

    @Override
    public void initialize(HeuristicSummary summary) {
        candidates = summary.summary.getEdges().stream()
                .filter(e -> (double) e.bookkeeping.getOrDefault("queryLoss", 0.0) > 0)
                .sorted(Comparator.comparingDouble(e ->
                        -1 * (double) e.bookkeeping.getOrDefault("queryLoss", 0.0)))
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasNext() {
        return !candidates.isEmpty();
    }

    @Override
    public BaseEdge next() {
        return candidates.remove(0);
    }
}
