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

    BaseGraph originalGraph;
    BaseGraph summaryGraph;

    public QuotientGraph(BaseGraph graph, EquivalenceRelation eq){
        summaryGraph =  new BaseGraph(false);
        int nodeCounter = 0;
        this.originalGraph = graph;

        Set<Integer> done = new HashSet<>();
        List<Integer> nodesIds = new ArrayList<>(graph.getIdMapping().keySet());

        for (int i = 0; i < nodesIds.size(); i++){
            int id = nodesIds.get(i);
            if (done.contains(id)){
                continue;
            }
            BaseNode newNode = summaryGraph.addNode(nodeCounter++,"");
            newNode.getContainedNodes().add(id);

            for (int j = 0; j < nodesIds.size(); j++){
                int otherId = nodesIds.get(j);
                if (eq.areEquivalent(id, otherId)){
                    newNode.getContainedNodes().add(otherId);
                    done.add(otherId);
                }
            }
        }

        int k = 0;
        for (BaseNode node: summaryGraph.getNodes()){
            int containedId = node.getContainedNodes().stream().findFirst().get();
            for (BaseEdge e: graph.outEdgesFor(containedId)){
                int targetSuperNodeID = summaryGraph.getNodes().stream()
                        .filter(n -> n.getContainedNodes().contains(e.getTarget().getId())).findFirst().get().getId();
                summaryGraph.addEdge(node.getId(), targetSuperNodeID, e.getLabel());
            }
            System.out.println("done: " + k++);
        }
    }

    @Override
    public List<Map<String, String>> query(BaseGraph query) {
        return new SubgraphIsomorphism().query(query, summaryGraph, false);
    }

    @Override
    public void train(Map<BaseGraph, List<Map<String, String>>> queries) {

    }
}
