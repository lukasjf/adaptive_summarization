package encoding;

import graph.BaseEdge;
import graph.BaseNode;
import summary.adaptive.heuristic.HeuristicSummary;

/**
 * Created by lukas on 05.04.18.
 */
public class HeuristicEncoder {

    public long encode(HeuristicSummary heuristicSummary){
        long size = 0L;
        for (BaseNode n: heuristicSummary.getSummary().getNodes()){
            size += 8; // nodeID
            size += 8; // node size
            size += n.getContainedNodes().size() * 8;
        }

        for (BaseEdge e: heuristicSummary.getSummary().getEdges()){
            size += 16 ; // source & targetID
            size += e.getLabel().length();
            size += 8; // support
        }
        return size;
    }
}
