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

    private long candidateCount(BaseEdge queryEdge){
        return candidateEdges(queryEdge).count();
    }

    private Stream<BaseEdge> candidateEdges(BaseEdge queryEdge){
        return edges.stream().filter(e ->
                e.getSource().match(queryEdge.getSource())
                && e.getTarget().match(queryEdge.getTarget())
                && e.getLabel().equals(queryEdge.getLabel()));
    }

    public List<List<String>> query(BaseGraph query){
        List<List<String>> result = new ArrayList<>();
        List<Map<String, String>> res = new ArrayList<>();
        List<BaseEdge> queryEdges = new ArrayList<>(query.getEdges());
        queryEdges.sort(Comparator.comparingLong(this::candidateCount));
        BaseEdge e = queryEdges.remove(0);
        candidateEdges(e).forEach(edge -> {
            HashMap<BaseNode, BaseNode> match = new HashMap<>();
            match.put(e.getSource(), edge.getSource());
            match.put(e.getTarget(), edge.getTarget());
            res.addAll(query(queryEdges, match));
        });

        List<Map<String, String>> deduplicated = new ArrayList<>();
        for (int i = 0; i < res.size(); i++){
            boolean existsDuplicate = false;
            for (int j = 0; j < deduplicated.size(); j++){
                if (res.get(i).equals(deduplicated.get(j))) {
                    existsDuplicate = true;
                    break;
                }
            }
            if (!existsDuplicate){
                deduplicated.add(res.get(i));
            }
        }
        List<String> variables = query.getVariables();
        for (Map<String, String> m : deduplicated){
            String[] r =  new String[variables.size()];
            m.forEach((k,v) -> r[variables.indexOf(k)] = v);
            result.add(Arrays.asList(r));
        }
        return result;
    }

    private List<Map<String, String>> query(List<BaseEdge> _queryEdges, Map<BaseNode, BaseNode> match){
        if (_queryEdges.isEmpty()){
            List<Map<String, String>> result = new ArrayList<>();
            Map<String, String> entry = new HashMap<>();
            match.forEach((k,v) -> {if (k.isVariable()) {entry.put(k.getLabel(), v.getLabel());}});
            result.add(entry);
            return result;
        } else{
            List <BaseEdge> queryEdges = new ArrayList<>(_queryEdges);
            BaseEdge queryEdge = queryEdges.remove(0);
            if (match.containsKey(queryEdge.getSource()) && match.containsKey(queryEdge.getTarget())){
                return querySourceTarget(queryEdges, queryEdge, match);
            } else if (match.containsKey(queryEdge.getSource())) {
                return querySource(queryEdges, queryEdge, match);
            } else{
                return queryTarget(queryEdges, queryEdge, match);
            }
        }
    }

    private List<Map<String,String>> querySourceTarget(List<BaseEdge> queryEdges, BaseEdge queryEdge, Map<BaseNode, BaseNode> match) {
        boolean existsEdge = edges.stream().anyMatch(e ->
                e.getLabel().equals(queryEdge.getLabel())
                && e.getSource() == match.get(queryEdge.getSource())
                && e.getTarget() == match.get(queryEdge.getTarget()));
        if (existsEdge){
            return query(queryEdges, match);
        }
        return new ArrayList<>();
    }

    private List<Map<String,String>> querySource(List<BaseEdge> queryEdges, BaseEdge queryEdge, Map<BaseNode, BaseNode> match) {
        List<Map<String, String>> results = new ArrayList<>();
        Stream<BaseEdge> candidates = outIndex.get(match.get(queryEdge.getSource())).stream().filter(e ->
                e.getLabel().equals(queryEdge.getLabel()));
        candidates.forEach(e -> {
            HashMap<BaseNode, BaseNode> newMatch = new HashMap<>(match);
            newMatch.put(queryEdge.getTarget(), e.getTarget());
            results.addAll(query(queryEdges, newMatch));
        });
        return results;
    }

    private List<Map<String,String>> queryTarget(List<BaseEdge> queryEdges, BaseEdge queryEdge, Map<BaseNode, BaseNode> match) {
        List<Map<String, String>> results = new ArrayList<>();
        Stream<BaseEdge> candidates = inIndex.get(match.get(queryEdge.getTarget())).stream().filter(e ->
                e.getLabel().equals(queryEdge.getLabel()));
        candidates.forEach(e -> {
            HashMap<BaseNode, BaseNode> newMatch = new HashMap<>(match);
            newMatch.put(queryEdge.getSource(), e.getSource());
            results.addAll(query(queryEdges, newMatch));
        });
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
        BaseEdge e = new BaseEdge(nodeMapping.get(source), nodeMapping.get(target), label);
        edges.add(e);
        inIndex.get(nodeMapping.get(target)).add(e);
        outIndex.get(nodeMapping.get(source)).add(e);
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
