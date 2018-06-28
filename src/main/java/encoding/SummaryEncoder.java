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
            size += 4; // nodeID
            size += 4; // node size
            size += n.getContainedNodes().size() * 4;
        }

        for (BaseEdge e: summary.getEdges()){
            size += 8 ; // source & targetID
            size += 4; //e.getLabel().length();
        }
        return size;
    }
}
