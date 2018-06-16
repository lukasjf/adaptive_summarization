package summary.equivalences;

import graph.BaseGraph;
import graph.Dataset;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by lukas on 30.05.18.
 */
public class QueryNodeEquivalence implements EquivalenceRelation {

    private BaseGraph graph;
    public Set<Integer> nodesFromQueries = new HashSet<>();
    private TotalEquivalence total;

    @Override
    public boolean areEquivalent(int id1, int id2) {
        if (id1 == id2){
            return true;
        }

        Set<Integer> node1Equivalents = nodesFromQueries.stream().filter(n -> total.areEquivalent(n, id1))
                .collect(Collectors.toSet());
        Set<Integer> node2Equivalents = nodesFromQueries.stream().filter(n -> total.areEquivalent(n, id2))
                .collect(Collectors.toSet());

        if (node1Equivalents.isEmpty() && node2Equivalents.isEmpty()){
            return true;
        }

        node1Equivalents.retainAll(node2Equivalents);
        return node1Equivalents.size() > 0;
    }

    @Override
    public void initialize(BaseGraph graph, Map<BaseGraph, List<Map<String, String>>> trainingResults) {
        this.graph = graph;
        this.total = new TotalEquivalence();
        total.initialize(graph, trainingResults);

        for (List<Map<String, String>> results: trainingResults.values()){
            for (Map<String, String> result: results){
                for (String answerLabel: result.values()){
                    nodesFromQueries.add(Dataset.I.IDFrom(answerLabel));
                }
            }
        }
    }
}
