package encoding;

import graph.summary.Summary;
import graph.summary.SummaryEdge;
import graph.summary.SummaryNode;

/**
 * Created by lukas on 05.04.18.
 */
public class SummaryEncoder {

    public long encode(Summary summary){
        long size = 0L;
        for (SummaryNode n: summary.getSNodes()){
            size += 8; // nodeID
            size += 8; // node size
            size += n.getLabel().length();
        }

        for (SummaryEdge e: summary.getSEdges()){
            size += 16 ; // source & targetID
            size += e.getLabel().length();
            size += 8; // actual count
        }
        return size;
    }
}
