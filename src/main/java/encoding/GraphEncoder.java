package encoding;

import graph.BaseEdge;
import graph.BaseGraph;
import graph.BaseNode;

/**
 * Created by lukas on 16.04.18.
 */

public class GraphEncoder {
    public long encode(BaseGraph graph){
        long size = 1L * graph.getNodes().size() * 4;
        for (BaseEdge e: graph.getEdges()){
            size += 8; // source & targetID
            size += 4; //e.getLabel().length();
        }
        return size;
    }
}
