package summary.equivalences;

import graph.BaseEdge;
import graph.BaseGraph;
import graph.BaseNode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by lukas on 21.05.18.
 */
public class TotalEquivalence implements EquivalenceRelation{

    private List<Map<String, String>> queryResults;
    private BaseGraph graph;

    @Override
    public boolean areEquivalent(int id1, int id2) {

        List<BaseEdge> outEdges1 = graph.outEdgesFor(id1);
        List<BaseEdge> outEdges2 = graph.outEdgesFor(id2);
        if (outEdges1.size() != outEdges2.size()){
            return false;
        }
        for (BaseEdge edge: outEdges1){
            if (outEdges2.stream().noneMatch(e -> matchingEdges(id1, id2, edge, e, true))){
                return false;
            }
        }

        List<BaseEdge> inEdges1 = graph.inEdgesFor(id1);
        List<BaseEdge> inEdges2 = graph.inEdgesFor(id2);
        if (inEdges1.size() != inEdges2.size()){
            return false;
        }
        for (BaseEdge edge: inEdges1){
            if (inEdges2.stream().noneMatch(e -> matchingEdges(id1, id2, edge, e, false))){
                return false;
            }
        }

        return true;
    }

    private boolean matchingEdges(int id1, int id2, BaseEdge edge1, BaseEdge edge2, boolean isSource) {
        if (!edge1.getLabel().equals(edge2.getLabel())){
            return false;
        }
        if (edge2.getTarget().getId() == id1 && edge1.getTarget().getId() == id2){
            return true;
        }
        if (isSource){
            return edge1.getTarget().getId() == edge2.getTarget().getId();
        } else{
            return edge1.getSource().getId() == edge2.getSource().getId();
        }
    }

    @Override
    public BaseGraph createQuotientGraph(BaseGraph graph, List<Map<String, String>> trainingResults) {
        BaseGraph summary =  new BaseGraph(false);
        int nodeCounter = 0;
        this.queryResults = trainingResults;
        this.graph = graph;

        Set<Integer> done = new HashSet<>();
        List<Integer> nodesIds = new ArrayList<>(graph.getIdMapping().keySet());

        for (int i = 0; i < nodesIds.size(); i++){
            int id = nodesIds.get(i);
            if (done.contains(id)){
                continue;
            }
            BaseNode newNode = summary.addNode(nodeCounter++,"");
            newNode.getContainedNodes().add(id);

            for (int j = 0; j < nodesIds.size(); j++){
                int otherId = nodesIds.get(j);
                if (areEquivalent(id, otherId)){
                    newNode.getContainedNodes().add(otherId);
                    done.add(otherId);
                }
            }
        }

        int k = 0;
        for (BaseNode node: summary.getNodes()){
            int containedId = node.getContainedNodes().stream().findFirst().get();
            for (BaseEdge e: graph.outEdgesFor(containedId)){
                int targetSuperNodeID = summary.getNodes().stream()
                        .filter(n -> n.getContainedNodes().contains(e.getTarget().getId())).findFirst().get().getId();
                summary.addEdge(node.getId(), targetSuperNodeID, e.getLabel());
            }
            System.out.println("done: " + k++);
        }

        return summary;
    }

}
