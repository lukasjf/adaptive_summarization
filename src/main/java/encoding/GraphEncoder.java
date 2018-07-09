package encoding;

import graph.BaseEdge;
import graph.BaseGraph;
import graph.BaseNode;

/**
 * Created by lukas on 16.04.18.
 */

public class GraphEncoder {
    public long encode(BaseGraph graph){
        return graph.getNodes().size() * 28 + graph.getEdges().size() * 16;
    }
}
