package summary.fan;

import graph.BaseEdge;
import graph.BaseGraph;
import graph.BaseNode;
import javafx.util.Pair;

import java.util.*;

/**
 * Created by lukas on 28.09.18.
 */
public class Bisimulation {

    private Set<Pair<BaseNode, BaseNode>> eqs = new HashSet<>();
    private Set<Pair<BaseNode, BaseNode>> nonEqs = new HashSet<>();

    Map<Integer, Set<Pair<BaseNode, BaseNode>>> levels = new HashMap<>();

    private BaseGraph graph;


    public Map<BaseNode, List<BaseNode>> getBisim2(BaseGraph graph){

        ArrayList<BaseNode> nodes = new ArrayList<>(graph.getNodes());

        this.graph = graph;
        levels.put(0, new HashSet<>());


        for (int i = 0; i < nodes.size() - 1; i++){
            System.out.print(".");
            for (int j = i+1; j < nodes.size(); j++){
                BaseNode n = nodes.get(i);
                BaseNode n2 = nodes.get(j);
                if (edgeTest(n, n2, 0)){
                    levels.get(0).add(new Pair<>(n, n2));
                }
            }
        }

        System.out.println("\n");
        for (int i = 1; i < graph.getNodes().size(); i++){
            System.out.println(i);
            levels.put(i, new HashSet<>());
            for (Pair<BaseNode, BaseNode> p : levels.get(i - 1)){
                BaseNode n = p.getKey();
                BaseNode n2 = p.getValue();

                if (edgeTest(n, n2, i)){
                    levels.get(i).add(new Pair<>(n, n2));
                }
            }
            levels.remove(i - 1);
        }
        return null;
    }

    public boolean edgeTest(BaseNode n, BaseNode n2, int i){
        for (BaseEdge e : graph.inEdgesFor(n.getId())){
            if (graph.inEdgesFor(n2.getId()).stream().noneMatch(e2 -> testSingleEdge(e, e2, false, i))){
                return false;
            }
        }
        for (BaseEdge e : graph.inEdgesFor(n2.getId())){
            if (graph.inEdgesFor(n.getId()).stream().noneMatch(e2 -> testSingleEdge(e, e2, false, i))){
                return false;
            }
        }

        for (BaseEdge e : graph.outEdgesFor(n.getId())){
            if (graph.outEdgesFor(n2.getId()).stream().noneMatch(e2 -> testSingleEdge(e, e2, true, i))){
                return false;
            }
        }

        for (BaseEdge e : graph.outEdgesFor(n2.getId())){
            if (graph.outEdgesFor(n.getId()).stream().noneMatch(e2 -> testSingleEdge(e, e2, true, i))){
                return false;
            }
        }
        return true;
    }

    public boolean testSingleEdge(BaseEdge e, BaseEdge e2, boolean fromSource, int i){
        boolean labelTest = e.getLabel().equals(e2.getLabel());
        boolean relTest;
        if (i == 0){
            relTest = true;
        } else {
            if (fromSource){
                relTest = levels.get(i - 1).contains(new Pair<>(e.getTarget(), e2.getTarget()));
            } else {
                relTest = levels.get(i - 1).contains(new Pair<>(e.getSource(), e2.getSource()));
            }
        }
        return labelTest && relTest;
    }

    public Map<BaseNode, List<BaseNode>> getBisimulationPartitions(BaseGraph graph){
        this.graph = graph;
        Map<BaseNode, List<BaseNode>> partitions = new HashMap<>();
        for (BaseNode n: graph.getNodes()){
            List<BaseNode> lst = new ArrayList<>();
            lst.add(n);
            partitions.put(n, lst);
        }

        for (BaseNode n: graph.getNodes()){
            if (!partitions.containsKey(n)){
                continue;
            }
            for (BaseNode n2: graph.getNodes()){
                if (!partitions.containsKey(n) || n == n2){
                    continue;
                }
                if (areEquivalent(n, n2)){
                    partitions.get(n).add(n2);
                    partitions.remove(n2);
                }
            }
        }

        return partitions;
    }

    private boolean areEquivalent(BaseNode n, BaseNode n2) {
        if (eqs.contains(new Pair<>(n, n2)) || eqs.contains(new Pair<>(n2, n))){
            return true;
        }
        if (nonEqs.contains(new Pair<>(n, n2)) || nonEqs.contains(new Pair<>(n2, n))){
            return false;
        }
        eqs.add(new Pair<>(n, n2));

        boolean e = checkEdgesFor(n, n2);
        boolean e2 = checkEdgesFor(n, n2);

        if (e && e2){
            return true;
        } else {
            eqs.remove(new Pair<>(n, n2));
            nonEqs.add(new Pair<>(n, n2));
            return false;
        }
    }

    private boolean checkEdgesFor(BaseNode n, BaseNode n2) {
        List<BaseEdge> inEdges = graph.inEdgesFor(n.getId());
        List<BaseEdge> inEdges2 = graph.inEdgesFor(n.getId());

        for (BaseEdge e : inEdges){
            boolean canSimulate = inEdges2.stream().anyMatch(e2 -> e.getLabel().equals(e2.getLabel())
                    && areEquivalent(e.getSource(), e2.getSource()));
            if (!canSimulate){
                return false;
            }
        }

        List<BaseEdge> outEdges = graph.outEdgesFor(n.getId());
        List<BaseEdge> outEdges2 = graph.outEdgesFor(n.getId());

        for (BaseEdge e : outEdges){
            boolean canSimulate = outEdges2.stream().anyMatch(e2 -> e.getLabel().equals(e2.getLabel())
                    && areEquivalent(e.getTarget(), e2.getTarget()));
            if (!canSimulate){
                return false;
            }
        }
        return true;
    }
}
