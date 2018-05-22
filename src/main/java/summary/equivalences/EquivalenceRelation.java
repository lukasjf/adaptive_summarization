package summary.equivalences;

import graph.BaseGraph;

import java.util.List;
import java.util.Map;

/**
 * Created by lukas on 21.05.18.
 */
public interface EquivalenceRelation {

    boolean areEquivalent (int id1, int id2);
    BaseGraph createQuotientGraph(BaseGraph graph, List<Map<String, String>> trainingResults);

}
