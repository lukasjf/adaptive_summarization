package graph;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by lukas on 12.03.18.
 */
public class BaseGraph implements GraphQueryAble{

    private static int DEDUPLICATE_COUNTER = 0;

    Map<String, Integer> index = new HashMap<>();
    Map<Integer, String> invertedIndex = new HashMap<>();

    Set<BaseNode> nodes = new HashSet<>(30000);
    HashMap<Integer, BaseNode> idMapping = new HashMap<>(30000);
    HashMap<String, BaseNode> labelMapping = new HashMap<>(30000);

    Set<BaseEdge> edges= new HashSet<>(50000);
    HashMap<Integer, List<BaseEdge>> inIndex = new HashMap<>(30000);
    HashMap<Integer, List<BaseEdge>> outIndex = new HashMap<>(30000);


    public void addNode(int id, String label){
        if (idMapping.keySet().contains(id)){
            System.err.println("ID already in use: " + id);
            return;
        }
        String newlabel = label;
        while (labelMapping.keySet().contains(newlabel)){
            newlabel += DEDUPLICATE_COUNTER++;
        }
        BaseNode node = new BaseNode(id);
        nodes.add(node);

        idMapping.put(id, node);
        labelMapping.put(newlabel, node);

        index.put(newlabel, id);
        invertedIndex.put(id, newlabel);

        for (BaseEdge e: inIndex.get(id)){
            edges.remove(e);
            outIndex.get(e.getSource().getId()).remove(e);
        }
        for (BaseEdge e: outIndex.get(id)){
            edges.remove(e);
            inIndex.get(e.getTarget().getId()).remove(e);
        }
        inIndex.put(id, new ArrayList<>());
        outIndex.put(id, new ArrayList<>());
    }

    public void removeNode(int id){
        String label = invertedIndex.get(id);
        BaseNode node = idMapping.get(id);

        nodes.remove(node);

        idMapping.remove(id);
        labelMapping.remove(label);

        index.remove(label);
        invertedIndex.remove(id);

        inIndex.remove(id);
        outIndex.remove(id);
    }

    public void addEdge(int source, int target, String label){
        if (outIndex.get(source).stream().anyMatch(e -> e.getTarget().getId() == target & e.getLabel().equals(label))){
            System.err.println("Trying to insert duplicate edge: " + source + " " + target + " " + label);
        }
        BaseEdge e = new BaseEdge(idMapping.get(source), idMapping.get(target), label);
        edges.add(e);
        inIndex.get(target).add(e);
        outIndex.get(source).add(e);
    }

    public void removeEdge(BaseEdge edge){
        edges.remove(edge);
        outIndex.get(edge.getSource().getId()).remove(edge);
        inIndex.get(edge.getTarget().getId()).remove(edge);
    }


    @Override
    public List<Map<String, String>> query(BaseGraph query) {
        return new SubgraphIsomorphism().query(query, this);
    }


    /*public List<String> getVariables(){
        return getNodes().stream().filter(n -> n.getLabel().startsWith("?"))
                .sorted(Comparator.comparingInt(BaseNode::getId)).map(BaseNode::getLabel).collect(Collectors.toList());
    }*/

    /*public List<String[]> query(BaseGraph query){
        List<Map<BaseEdge, BaseEdge>> matchings = new SubgraphIsomorphism(this).query(query);
        List<String[]> results = new ArrayList<>();

        List<String> variables = query.getVariables();

        for (Map<BaseEdge, BaseEdge> match: matchings){
            String[] singleResult = new String[variables.size()];
            for (BaseEdge queryEdge: match.keySet()){
                if (queryEdge.getSource().isVariable()){
                    int variableIndex = variables.indexOf(queryEdge.getSource().getLabel());
                    singleResult[variableIndex] = match.get(queryEdge).getSource().getLabel();
                }
                if (queryEdge.getTarget().isVariable()){
                    int variableIndex = variables.indexOf(queryEdge.getTarget().getLabel());
                    singleResult[variableIndex] = match.get(queryEdge).getTarget().getLabel();
                }
            }
            results.add(singleResult);
        }
        return results;
    }*/


}
