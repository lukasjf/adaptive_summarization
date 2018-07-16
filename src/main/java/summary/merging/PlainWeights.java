package summary.merging;

import graph.BaseEdge;
import graph.BaseGraph;
import graph.Dataset;

import java.util.List;
import java.util.Map;

/**
 * Created by lukas on 11.07.18.
 */
public class PlainWeights implements WeightCreation {

    MergedSummary merged;

    @Override
    public void initializeWeights(MergedSummary merged, Map<BaseGraph,List<Map<String, String>>> queries) {
        this.merged = merged;
        for (BaseGraph query : queries.keySet()) {
            for (Map<String, String> result : queries.get(query)) {
                for (BaseEdge queryEdge : query.getEdges()) {
                    BaseEdge resultEdge = findResultEdge(queryEdge, result);
                    merged.weights.put(resultEdge, merged.weights.getOrDefault(resultEdge, 0.0) + 1);
                }
            }
        }
    }

    private BaseEdge findResultEdge(BaseEdge queryEdge, Map<String, String> result) {
        String sourceLabel = result.get(Dataset.I.labelFrom(queryEdge.getSource().getId()));
        String targetLabel = result.get(Dataset.I.labelFrom(queryEdge.getTarget().getId()));
        for (BaseEdge e: merged.summary.outEdgesFor(Dataset.I.IDFrom(sourceLabel))){
            if (e.getTarget().getId() == Dataset.I.IDFrom(targetLabel) && e.getLabel().equals(queryEdge.getLabel())){
                return e;
            }
        }
        return null;
    }
}
