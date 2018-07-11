package summary.merging;

import graph.BaseGraph;

import java.util.List;
import java.util.Map;

/**
 * Created by lukas on 11.07.18.
 */
public interface WeightCreation {

    public void initializeWeights(MergedSummary merged, Map<BaseGraph, List<Map<String, String>>> queries);
}
