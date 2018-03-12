package graph.summary;

import graph.Graph;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by lukas on 12.03.18.
 */
public class Summary extends Graph {

    private Graph graph;

    public static Summary createFromGraph(Graph graph){
        Summary s = new Summary(graph);
        Set<String> nodeLabels = graph.getNodes().stream().map(n -> n.getLabel()).collect(Collectors.toSet());
        Set<String> edgeLabels = graph.getEdges().stream().map(e -> e.getLabel()).collect(Collectors.toSet());
        SummaryNode node = new SummaryNode(0, nodeLabels);
        s.getNodes().add(node);
        for (String edgeLabel: edgeLabels){
            s.getEdges().add(new SummaryEdge(node, node , edgeLabel, graph.getEdges().size()));
        }
        return s;
    }

    public Summary(Graph graph){
        this.graph = graph;
    }


}
