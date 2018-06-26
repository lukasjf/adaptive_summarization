package summary.merging;

import graph.BaseGraph;

import java.util.List;
import java.util.Map;

/**
 * Created by lukas on 26.06.18.
 */
public interface MergeStrategy {

    int[] search(MergedSummary mSummary, Map<BaseGraph, List<Map<String, String>>> queries);
}
