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
            size += n.getContainedNodes().size() * 4;
        }
        return size + summary.getNodes().size() * 40 + summary.getEdges().size() * 16;
    }
}
