package summary.caching;

import encoding.GraphEncoder;
import evaluation.Benchmarkable;
import graph.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lukas on 03.05.18.
 */
public class SummaryCache implements Benchmarkable{

    private BaseGraph cache = new BaseGraph();
    private long sizeLimit;

    private Map<Integer, Double> usage = new HashMap<>();

    public SummaryCache(long sizeLimit){
        this.sizeLimit = sizeLimit;
    }

    @Override
    public List<Map<String, String>> query(BaseGraph query) {
        List<Map<String, String>> results = cache.query(query);
        age();
        return results;
    }

    @Override
    public void train(Map<BaseGraph, List<Map<String, String>>> queries) {
        for (BaseGraph query: queries.keySet()){
            for (Map<String, String> result: queries.get(query)){
                for (BaseEdge e: query.getEdges()){
                    String sourceL = result.get(Dataset.I.labelFrom(e.getSource().getId()));
                    int source = Dataset.I.IDFrom(sourceL);
                    String targetL = result.get(Dataset.I.labelFrom(e.getTarget().getId()));
                    int target = Dataset.I.IDFrom(targetL);
                    BaseNode s = cache.addNode(source, sourceL);
                    s.getContainedNodes().add(source);
                    BaseNode t = cache.addNode(target, targetL);
                    t.getContainedNodes().add(target);
                    cache.addEdge(source, target, e.getLabel());
                    touch(source);
                    touch(target);
                }
            }
            age();
        }

        while (size() > sizeLimit){
            evictData();
        }
    }


    private void touch(int nodeID){
        if (usage.containsKey(nodeID)){
            usage.put(nodeID, usage.get(nodeID) + 1);
        } else{
            usage.put(nodeID, 1.0);
        }
    }

    private void age() {
        for (int id: usage.keySet()){
            usage.put(id, 0.9 * usage.get(id));
        }
    }

    private void evictData() {
        int minID = Integer.MAX_VALUE;
        for (int id: usage.keySet()){
            if (usage.get(id) < minID){
                minID = id;
            }
        }
        cache.removeNode(minID);
        usage.remove(minID);
    }

    public long size(){
        return new GraphEncoder().encode(cache) + usage.size() * 8;
    }
}
