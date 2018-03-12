package graph;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by lukas on 12.03.18.
 */
public class Graph {

    private Set<Node> nodes = new HashSet<>(30000);
    private HashMap<Integer, Node> nodeMapping = new HashMap<>(30000);
    private Set<Edge> edges= new HashSet<>(50000);

    static public Graph parseGraph(String filename){
        Graph g = new Graph();
        try (BufferedReader br = new BufferedReader(new FileReader(new File(filename)))){
            String line;
            while ((line = br.readLine()) != null){
                if (line.trim().isEmpty()){
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

    public long candidateCount(Edge queryEdge){
        return candidateEdges(queryEdge).count();
    }

    public Stream<Edge> candidateEdges(Edge queryEdge){
        return edges.stream().filter(e ->
                e.getSource().match(queryEdge.getSource())
                        && e.getTarget().match(queryEdge.getTarget())
                        && e.getLabel().equals(queryEdge.getLabel()));
    }

    public List<Map<String, String>> query(Graph query){
        List<Map<String, String>> result = new ArrayList<>();
        List<Edge> queryEdges = new ArrayList<>(query.edges);
        queryEdges.sort(Comparator.comparingLong(this::candidateCount));
        Edge e = queryEdges.remove(0);
        candidateEdges(e).forEach(edge -> {
            HashMap<Node, Node> match = new HashMap<>();
            match.put(e.getSource(), edge.getSource());
            match.put(e.getTarget(), edge.getTarget());
            result.addAll(query(queryEdges, match));
        });

        List<Map<String, String>> deduplicated = new ArrayList<>();
        for (int i = 0; i < result.size()-1; i++){
            boolean existsDuplicate = false;
            for (int j = 0; j < deduplicated.size(); j++){
                if (result.get(i).equals(deduplicated.get(j))) {
                    existsDuplicate = true;
                    break;
                }
            }
            if (!existsDuplicate){
                deduplicated.add(result.get(i));
            }
        }
        return deduplicated;
    }

    public List<Map<String, String>> query(List<Edge> _queryEdges, Map<Node, Node> match){
        if (_queryEdges.isEmpty()){
            List<Map<String, String>> result = new ArrayList<>();
            Map<String, String> entry = new HashMap<>();
            match.forEach((k,v) -> {if (k.isVariable()) {entry.put(k.getLabel(), v.getLabel());}});
            result.add(entry);
            return result;
        } else{
            List <Edge> queryEdges = new ArrayList<>(_queryEdges);
            Edge queryEdge = queryEdges.remove(0);
            if (match.containsKey(queryEdge.getSource()) && match.containsKey(queryEdge.getTarget())){
                return querySourceTarget(queryEdges, queryEdge, match);
            } else if (match.containsKey(queryEdge.getSource())) {
                return querySource(queryEdges, queryEdge, match);
            } else{
                return queryTarget(queryEdges, queryEdge, match);
            }
        }
    }

    private List<Map<String,String>> querySourceTarget(List<Edge> queryEdges, Edge queryEdge, Map<Node, Node> match) {
        boolean existsEdge = edges.stream().anyMatch(e ->
                e.getLabel().equals(queryEdge.getLabel())
                && e.getSource() == match.get(queryEdge.getSource())
                && e.getTarget() == match.get(queryEdge.getTarget()));
        if (existsEdge){
            return query(queryEdges, match);
        }
        return new ArrayList<>();
    }

    private List<Map<String,String>> querySource(List<Edge> queryEdges, Edge queryEdge, Map<Node, Node> match) {
        List<Map<String, String>> results = new ArrayList<>();
        Stream<Edge> candidates = edges.stream().filter(e ->
                e.getLabel().equals(queryEdge.getLabel())
                && e.getSource() == match.get(queryEdge.getSource()));
        candidates.forEach(e -> {
            HashMap<Node, Node> newMatch = new HashMap<>(match);
            newMatch.put(queryEdge.getTarget(), e.getTarget());
            results.addAll(query(queryEdges, newMatch));
        });
        return results;
    }

    private List<Map<String,String>> queryTarget(List<Edge> queryEdges, Edge queryEdge, Map<Node, Node> match) {
        List<Map<String, String>> results = new ArrayList<>();
        Stream<Edge> candidates = edges.stream().filter(e ->
                e.getLabel().equals(queryEdge.getLabel())
                && e.getTarget() == match.get(queryEdge.getTarget()));
        candidates.forEach(e -> {
            HashMap<Node, Node> newMatch = new HashMap<>(match);
            newMatch.put(queryEdge.getSource(), e.getSource());
            results.addAll(query(queryEdges, newMatch));
        });
        return results;
    }



    public void addNode(int id, String label){
        Node n = new Node(id, label);
        nodeMapping.put(id, n);
        nodes.add(n);
    }

    public void addEdge(int source, int target, String label){
        Edge e = new Edge(nodeMapping.get(source), nodeMapping.get(target), label);
        edges.add(e);
    }

}
