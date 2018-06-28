package encoding;

import graph.BaseEdge;
import graph.BaseGraph;
import graph.BaseNode;
import summary.topdown.HeuristicSummary;

/**
 * Created by lukas on 05.04.18.
 */
public class SummaryEncoder {

    public long encode(BaseGraph summary){
        long size = 0L;
        for (BaseNode n: summary.getNodes()){
            size += 8; // nodeID
            size += 8; // node size
            size += n.getContainedNodes().size() * 8;
        }

        for (BaseEdge e: summary.getEdges()){
            size += 16 ; // source & targetID
            size += 8; //e.getLabel().length();
        }
        return size;
    }
}
