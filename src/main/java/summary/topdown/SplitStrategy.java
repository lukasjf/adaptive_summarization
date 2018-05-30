package summary.topdown;

import graph.BaseEdge;
import graph.BaseNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lukas on 18.04.18.
 */
public interface SplitStrategy {

    Map<String, SplitStrategy> algorithms = new HashMap<>();

    BaseNode[] split(HeuristicSummary summary, BaseEdge criticalEdge);
}
