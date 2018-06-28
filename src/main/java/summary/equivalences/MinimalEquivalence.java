package summary.equivalences;

import graph.BaseGraph;
import graph.Dataset;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lukas on 29.05.18.
 */
public class MinimalEquivalence implements EquivalenceRelation {

    private BaseGraph graph;
    private Map<BaseGraph, List<Map<String, String>>> trainingResults;

    @Override
    public boolean areEquivalent(int id1, int id2) {
        if (id1 == id2){
            return true;
        }

        for (List<Map<String,String>> results: trainingResults.values()){
            for (Map<String, String> result: results){
                for (String queryLabel: result.keySet()){
                    /*if (! queryLabel.startsWith("-")){
                        continue;
                    }*/
                    String answerlabel = result.get(queryLabel);

                    if (id1 == Dataset.I.IDFrom(answerlabel)){
                        Map<String, String> testMap = new HashMap<>(result);
                        testMap.put(queryLabel, Dataset.I.labelFrom(id2));
                        if (!results.contains(testMap)){
                            return false;
                        }
                    }
                    if (id2 == Dataset.I.IDFrom(answerlabel)){
                        Map<String, String> testMap = new HashMap<>(result);
                        testMap.put(queryLabel, Dataset.I.labelFrom(id1));
                        if (!results.contains(testMap)){
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void initialize(BaseGraph graph, Map<BaseGraph, List<Map<String, String>>> trainingResults) {
        this.graph = graph;
        this.trainingResults = trainingResults;
    }
}
