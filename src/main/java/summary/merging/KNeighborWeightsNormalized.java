package summary.merging;

import graph.BaseEdge;
import graph.BaseGraph;
import graph.BaseNode;
import graph.Dataset;

import java.util.List;
import java.util.Map;

/**
 * Created by lukas on 11.07.18.
 */
public class KNeighborWeightsNormalized implements WeightCreation {

    MergedSummary merged;
    int k;
    double decay;

    public KNeighborWeightsNormalized(int k, double decay){
        this.k = k;
        this.decay = decay;
    }

    @Override
    public void initializeWeights(MergedSummary merged, Map<BaseGraph, List<Map<String, String>>> queries) {
        this.merged = merged;
        for (BaseEdge e : merged.summary.getEdges()) {
            merged.actual.put(e, 1);
        }
        for (BaseGraph query : queries.keySet()) {
            for (Map<String, String> result : queries.get(query)) {
                for (BaseEdge queryEdge : query.getEdges()) {
                    BaseEdge resultEdge = findResultEdge(queryEdge, result);
                    double oldWeight = merged.weights.getOrDefault(resultEdge, 0.0);
                    merged.weights.put(resultEdge, oldWeight + 1);
                    addWeightToNeighbors(resultEdge, resultEdge.getSource());
                    addWeightToNeighbors(resultEdge, resultEdge.getTarget());
                }
            }
        }
    }

    private void addWeightToNeighbors(BaseEdge resultEdge, BaseNode node) {
        int neighborhoodSize = merged.summary.outEdgesFor(node.getId()).size() + merged.summary.inEdgesFor( node.getId()).size();
        for (BaseEdge e: merged.summary.outEdgesFor(node.getId())){
            if (e != resultEdge){
                merged.weights.put(e, decay / neighborhoodSize);
            }
        }
        for (BaseEdge e: merged.summary.inEdgesFor(node.getId())){
            if (e != resultEdge){
                merged.weights.put(e, decay / neighborhoodSize);
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
