package summary.merging;

import graph.BaseEdge;
import graph.BaseNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by lukas on 16.07.18.
 */
public class DiffObjective {

    MergedSummary merged;
    double delta;

    Set<BaseEdge> diffEdges;

    double getObjectiveDelta(MergedSummary mergedSummary, BaseNode n1, BaseNode n2){
        merged = mergedSummary;

        double delta = 0.0;

        diffEdges = new HashSet<>();
        for (BaseEdge e: merged.summary.outEdgesFor(n1.getId())){
            if (merged.weights.containsKey(e)){
                removediff(e, false);
            }
        }
        for (BaseEdge e: merged.summary.inEdgesFor(n1.getId())){
            if (merged.weights.containsKey(e) && e.getSource() != e.getTarget()){
                removediff(e, false);
            }
        }
        for (BaseEdge e: new ArrayList<>(merged.summary.outEdgesFor(n2.getId()))){
            if (merged.weights.containsKey(e)){
                removediff(e, true);
                merged.mergeEdge(e, n1.getId(), true);
            }
        }
        for (BaseEdge e: new ArrayList<>(merged.summary.inEdgesFor(n2.getId()))){
            if (merged.weights.containsKey(e) && e.getSource() != e.getTarget()){
                removediff(e, true);
                merged.mergeEdge(e, n1.getId(), false);
            }
        }

        n1.getContainedNodes().addAll(n2.getContainedNodes());

        for (BaseEdge e: merged.summary.outEdgesFor(n1.getId())){
            if (merged.weights.containsKey(e)){
                adddiff(e);
            }
        }

        for (BaseEdge e: merged.summary.inEdgesFor(n1.getId())){
            if (merged.weights.containsKey(e) && e.getSource() != e.getTarget()){
                adddiff(e);
            }
        }

        n1.getContainedNodes().removeAll(n2.getContainedNodes());

        for (BaseEdge e: merged.summary.outEdgesFor(n2.getId())){
            if (merged.weights.containsKey(e)){
                merged.unmergeEdge(e, n1.getId(), true);
            }
        }

        for (BaseEdge e: merged.summary.inEdgesFor(n2.getId())){
            if (merged.weights.containsKey(e) && e.getSource() != e.getTarget()){
                merged.unmergeEdge(e, n1.getId(), false);
            }
        }

        return delta / merged.totalWeight;
    }

    private void removediff(BaseEdge e, boolean nodeToMerge) {
        if (nodeToMerge && diffEdges.contains(e)){
            return;
        }
        if (merged.weights.containsKey(e)){
            diffEdges.add(e);
            delta -= merged.weights.get(e) * merged.actual.get(e) / e.size();
        }
    }

    private void adddiff(BaseEdge e){
        delta += merged.weights.get(e) * merged.actual.get(e) / e.size();
    }
}
