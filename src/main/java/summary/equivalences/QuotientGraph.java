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

    public BaseGraph graph;
    public BaseGraph summary;
    public EquivalenceRelation eq;

    public Map<Integer, Integer> supernodeMapping = new HashMap<>();


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
            System.out.println("doing node: " + i);
            if (done.contains(id)){
                continue;
            }
            done.add(id);
            BaseNode newNode = summary.addNode(nodeCounter++,"");
            newNode.getContainedNodes().add(id);
            supernodeMapping.put(id, newNode.getId());


            for (int j = i + 1; j < nodesIds.size(); j++){
                int otherId = nodesIds.get(j);
                if (done.contains(otherId)){
                    continue;
                }
                if (eq.areEquivalent(id, otherId)){
                    newNode.getContainedNodes().add(otherId);
                    done.add(otherId);
                    supernodeMapping.put(otherId, newNode.getId());
                }
            }
        }

        int k = 0;
        for (BaseNode n: graph.getNodes()){
            for (BaseEdge e: graph.outEdgesFor(n.getId())){
                summary.addEdge(supernodeMapping.get(e.getSource().getId()), supernodeMapping.get(e.getTarget().getId()), e.getLabel());
            }
            System.out.println("done edges for node: " + k++);
        }
        /*for (BaseNode node: summary.getNodes()){
            int containedId = node.getContainedNodes().stream().findFirst().get();
            for (BaseEdge e: graph.outEdgesFor(containedId)){
                int targetSuperNodeID = supernodeMapping.get(e.getTarget().getId());
                summary.addEdge(node.getId(), targetSuperNodeID, e.getLabel());
            }
            System.out.println("done summary node: " + k++);
        }*/
    }
}
