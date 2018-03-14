package graph.summary;

import graph.Graph;
import graph.Query;
import splitstrategies.SplitStrategy;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by lukas on 12.03.18.
 */
public class Summary extends Graph {

    private Graph graph;

    public static Summary createFromGraph(Graph graph){
        Summary s = new Summary(graph);
        List<String> nodeLabels = graph.getNodes().stream().map(n -> n.getLabel()).collect(Collectors.toList());
        Set<String> edgeLabels = graph.getEdges().stream().map(e -> e.getLabel()).collect(Collectors.toSet());
        SummaryNode node = new SummaryNode(0, new HashSet<>(nodeLabels));
        s.getNodes().add(node);
        s.getNodeMapping().put(0, node);
        for (String edgeLabel: edgeLabels){
            s.addSEdge(node, node, edgeLabel);
        }
        return s;
    }

    public Summary(Graph graph) {
        super();
        this.graph = graph;
    }

    @Override
    public List<List<String>> query(Query query){
        List<List<String>> raw = super.query(query);
        List<List<String>> result = new ArrayList<>();
        List<String> variables = query.getVariables();
        for (List<String> entry: raw){
            List<List<String>> unfolded = entry.stream()
                    .map(s -> Arrays.asList(s.split("#"))).collect(Collectors.toList());
            int cartesianCount = unfolded.stream().map(List::size).reduce(1, (a, b) -> a * b);
            String[][] unfoldedResults = new String[cartesianCount][variables.size()];
            int numberIterations = 1;
            for (int i = 0, c = 0; i < variables.size(); i++, c = 0){
                int blockSize = cartesianCount / unfolded.get(i).size() / numberIterations;
                for (int j = 0; j < numberIterations; j++){
                    for (String answer: unfolded.get(i)){
                        for (int k = 0; k < blockSize; k++){
                            unfoldedResults[c++][i] = answer;
                        }
                    }
                }
                numberIterations = numberIterations * unfolded.get(i).size();
            }
            for (String[] array : unfoldedResults) {
                result.add(Arrays.asList(array));
            }
        }
        return result;
    }

    public void split(SplitStrategy strategy){
        strategy.split(this);
    }

    public void addSEdge(SummaryNode source, SummaryNode target, String label) {
        SummaryEdge edge = new SummaryEdge(source, target, label);
        edge.setActual(supportOf(edge));
        getEdges().add(edge);
    }

    public long supportOf(SummaryEdge edge){
        return this.graph.getEdges().stream().filter(e -> e.getLabel().equals(edge.getLabel())
                && edge.getSSource().getLabels().contains(e.getSource().getLabel())
                && edge.getSTarget().getLabels().contains(e.getTarget().getLabel())).count();
    }

    public Graph getGraph(){
        return graph;
    }

}
