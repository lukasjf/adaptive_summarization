package summary.topdown;

import graph.BaseEdge;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lukas on 18.04.18.
 */
public interface SplitChoiceStrategy {

    Map<String, SplitChoiceStrategy> algorithms = new HashMap<>();

    void initialize(HeuristicSummary summary);
    boolean hasNext();
    BaseEdge next();

}
