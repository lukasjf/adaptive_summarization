package summary.equivalences;

import evaluation.Benchmarkable;
import graph.BaseEdge;
import graph.BaseGraph;
import graph.BaseNode;
import graph.SubgraphIsomorphism;

import java.util.*;

/**
 * Created by lukas on 22.05.18.
 */
public class QuotientGraph implements Benchmarkable{

    private BaseGraph graph;
    private BaseGraph summary;
    private EquivalenceRelation eq;

    private Map<Integer, Integer> supernodeMapping = new HashMap<>();


    public QuotientGraph(BaseGraph graph, EquivalenceRelation eq){
        summary =  new BaseGraph();
        this.graph = graph;
        this.eq = eq;
    }

    @Override
    public List<Map<String, String>> query(BaseGraph query) {
        return new SubgraphIsomorphism().query(query, summary, false);
    }

    @Override
    public void train(Map<BaseGraph, List<Map<String, String>>> queries) {
        this.eq.initialize(graph, queries);
        int nodeCounter = 0;

        Set<Integer> done = new HashSet<>();
        List<Integer> nodesIds = new ArrayList<>(graph.getIdMapping().keySet());

        for (int i = 0; i < nodesIds.size(); i++){
            int id = nodesIds.get(i);
            if (done.contains(id)){
                continue;
            }
            BaseNode newNode = summary.addNode(nodeCounter++,"");
            newNode.getContainedNodes().add(id);
            supernodeMapping.put(id, nodeCounter-1);


            for (int j = i; j < nodesIds.size(); j++){
                int otherId = nodesIds.get(j);
                if (eq.areEquivalent(id, otherId)){
                    newNode.getContainedNodes().add(otherId);
                    done.add(otherId);
                    supernodeMapping.put(otherId, nodeCounter-1);
                }
            }
            System.out.println("done node: " + i);
        }

        int k = 0;
        for (BaseNode node: summary.getNodes()){
            int containedId = node.getContainedNodes().stream().findFirst().get();
            for (BaseEdge e: graph.outEdgesFor(containedId)){
                int targetSuperNodeID = supernodeMapping.get(e.getTarget().getId());
                summary.addEdge(node.getId(), targetSuperNodeID, e.getLabel());
            }
            System.out.println("done summary node: " + k++);
        }
    }
}
