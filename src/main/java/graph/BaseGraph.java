package graph;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by lukas on 12.03.18.
 */
public class BaseGraph implements Serializable{

    private static int DEDUPLICATE_COUNTER = 0;

    private Set<BaseNode> nodes = new HashSet<>(30000);
    private HashMap<Integer, BaseNode> nodeMapping = new HashMap<>(30000);
    private HashMap<String, BaseNode> labelMapping = new HashMap<>(30000);
    private Set<BaseEdge> edges= new HashSet<>(50000);

    private HashMap<BaseNode, List<BaseEdge>> inIndex = new HashMap<>(30000);
    private HashMap<BaseNode, List<BaseEdge>> outIndex = new HashMap<>(30000);


    static public BaseGraph parseGraph(String filename){
        BaseGraph g = new BaseGraph();
        try (BufferedReader br = new BufferedReader(new FileReader(new File(filename)))){
            String line;
            while ((line = br.readLine()) != null){
                if (line.trim().isEmpty()){
                    continue;
                }
                if (line.startsWith("#")){
                    continue;
                }
                String[] token = line.split(" ");
                if (token[0].equals("v")){
                    int id = Integer.parseInt(token[1]);
                    String label = token[2];
                    g.addNode(id, label);
                }
                if (token[0].equals("e")){
                    int source = Integer.parseInt(token[1]);
                    int target = Integer.parseInt(token[2]);
                    String label = token[3];
                    g.addEdge(source, target, label);
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return g;
    }

    public List<String> getVariables(){
        return getNodes().stream().filter(n -> n.getLabel().startsWith("?"))
                .sorted(Comparator.comparingInt(BaseNode::getId)).map(BaseNode::getLabel).collect(Collectors.toList());
    }

    public List<String[]> query(BaseGraph query){
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
    }


    public void addNode(int id, String label){
        BaseNode node = new BaseNode(id, label);
        addNode(node);
    }

    public void addNode(BaseNode node){
        if (nodeMapping.containsKey(node.getId())){
            nodes.remove(nodeMapping.get(node.getId()));
        }
        while (labelMapping.containsKey(node.getLabel())){
            node.setLabel(node.getLabel() + DEDUPLICATE_COUNTER++);
        }
        nodeMapping.put(node.getId(), node);
        labelMapping.put(node.getLabel(), node);
        nodes.add(node);
        inIndex.put(node, new ArrayList<>());
        outIndex.put(node, new ArrayList<>());
    }

    public void addEdge(int source, int target, String label){

        if (getOutIndex().get(nodeMapping.get(source)).stream().anyMatch(e -> e.getTarget().getId() == target
                && e.getLabel().equals(label))){
            return;
        }
        BaseEdge e = new BaseEdge(nodeMapping.get(source), nodeMapping.get(target), label);
        edges.add(e);
        inIndex.get(nodeMapping.get(target)).add(e);
        outIndex.get(nodeMapping.get(source)).add(e);
    }

    public void removeNode(BaseNode node){
        getNodes().remove(node);
        getNodeMapping().remove(node.getId());
        getLabelMapping().remove(node.getLabel());

        for (BaseEdge e : getInIndex().remove(node)){
            getEdges().remove(e);
            getOutIndex().get(e.getSource()).remove(e);
        }
        for (BaseEdge e : getOutIndex().remove(node)){
            getEdges().remove(e);
            getInIndex().get(e.getTarget()).remove(e);
        }
    }

    public void removeEdge(BaseEdge edge){
        getEdges().remove(edge);
        getOutIndex().get(edge.getSource()).remove(edge);
        getInIndex().get(edge.getTarget()).remove(edge);
    }


    public Set<BaseNode> getNodes() {
        return nodes;
    }

    public HashMap<Integer, BaseNode> getNodeMapping() {
        return nodeMapping;
    }

    public HashMap<String, BaseNode> getLabelMapping() {
        return labelMapping;
    }

    public Set<BaseEdge> getEdges() {
        return edges;
    }

    public HashMap<BaseNode, List<BaseEdge>> getInIndex() {
        return inIndex;
    }

    public HashMap<BaseNode, List<BaseEdge>> getOutIndex() {
        return outIndex;
    }
}
