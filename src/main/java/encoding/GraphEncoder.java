package encoding;

import graph.BaseEdge;
import graph.BaseGraph;
import graph.BaseNode;

/**
 * Created by lukas on 16.04.18.
 */

public class GraphEncoder {
    public long encode(BaseGraph graph){
        long size = 1L * graph.getNodes().size() * 8;
        for (BaseEdge e: graph.getEdges()){
            size += 16; // source & targetID
            size += 8; //e.getLabel().length();
        }
        return size;
    }
}
