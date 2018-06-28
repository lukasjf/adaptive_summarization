package graph;

import java.util.*;
import java.util.stream.Stream;

/**
 * Created by lukas on 26.03.18.
 */
public class SubgraphIsomorphism {

    private BaseGraph graph;
    private BaseGraph query;
    private boolean isInjective;
    public List<Map<BaseEdge, BaseEdge>> matchings;

    private int timeout = -1;
    private long startTime = 0;
    boolean timedout = false;

    public SubgraphIsomorphism(){

    }

    public SubgraphIsomorphism(int timeout){
        this.startTime = System.currentTimeMillis();
        this.timeout = timeout;
    }

    private long candidateCount(BaseEdge queryEdge){
        return candidateEdges(queryEdge).count();
    }

    private Stream<BaseEdge> candidateEdges(BaseEdge queryEdge){
        return graph.edges.stream().filter(e ->
                e.getSource().match(queryEdge.getSource())
                        && e.getTarget().match(queryEdge.getTarget())
                        && e.getLabel().equals(queryEdge.getLabel()));
    }

    public List<Map<String, String>> query(BaseGraph query, BaseGraph graph, boolean isInjective){
        this.graph = graph;
        this.query = query;
        this.isInjective = isInjective;

        List<Map<BaseEdge, BaseEdge>> matchings = new ArrayList<>();

        List<BaseEdge> queryEdges = new ArrayList<>(query.edges);
        queryEdges.sort(Comparator.comparingLong(this::candidateCount));
        BaseEdge e = queryEdges.remove(0);
        candidateEdges(e).forEach(edge -> {
            Map<BaseNode, BaseNode> match = new HashMap<>();
            Map<BaseEdge, BaseEdge> matchedEdges = new HashMap<>();
            match.put(e.getSource(), edge.getSource());
            match.put(e.getTarget(), edge.getTarget());
            matchedEdges.put(e, edge);
            matchings.addAll(query(queryEdges, match, matchedEdges));
        });
        this.matchings = matchings;
        if (timedout){
            this.matchings.clear();
            System.err.println("timed out");
        }
        List<Map<BaseNode, BaseNode>> nodeMatchings =  createNodeMatchings(matchings);
        // remove self loops that can happen as part of cross product
        List<Map<BaseNode, BaseNode>> withoutLoops = new ArrayList<>();
        for (Map<BaseNode, BaseNode> match: nodeMatchings){
            if (query.getEdges().stream().noneMatch(edge -> match.get(edge.getSource()).equals(match.get(edge.getTarget())))){
                withoutLoops.add(match);
            }
        }
        return expandCrossProduct(withoutLoops);
    }

    private List<Map<BaseNode,BaseNode>> createNodeMatchings(List<Map<BaseEdge, BaseEdge>> matchings) {
        List<Map<BaseNode, BaseNode>> nodeMatchings = new ArrayList<>();
        for (Map<BaseEdge, BaseEdge> matching: matchings){
            Map<BaseNode, BaseNode> result = new HashMap<>();
            for (BaseEdge m: matching.keySet()){
                if (!result.containsKey(m.getSource())){
                    result.put(m.getSource(), matching.get(m).getSource());
                }
                if (!result.containsKey(m.getTarget())){
                    result.put(m.getTarget(), matching.get(m).getTarget());
                }
            }
            nodeMatchings.add(result);
        }
        return nodeMatchings;
    }

    private List<Map<String, String>> expandCrossProduct(List<Map<BaseNode, BaseNode>> nodeMatchings) {
        List<Map<String, String>> results = new ArrayList<>();
        for (Map<BaseNode, BaseNode> matching: nodeMatchings){
            CrossProductUnfolder graphResults = new CrossProductUnfolder(matching);
            while (graphResults.hasNext()){
                Map<Integer, Integer> intResult = graphResults.next();
                Map<String, String> labelResult = new HashMap<>();
                for (int key: intResult.keySet()){
                    labelResult.put(Dataset.I.labelFrom(key), Dataset.I.labelFrom(intResult.get(key)));
                }
                results.add(labelResult);
            }
        }
        return results;
    }

    private List<Map<BaseEdge, BaseEdge>> query(List<BaseEdge> _queryEdges, Map<BaseNode, BaseNode> match, Map<BaseEdge, BaseEdge> matchedEdges){
        if (startTime > 0 && System.currentTimeMillis() > startTime + timeout * 1000){
            timedout = true;
            return new ArrayList<>();
        }
        if (_queryEdges.isEmpty()){
            List<Map<BaseEdge, BaseEdge>> result = new ArrayList<>();
            result.add(matchedEdges);
            return result;
        } else{
            List <BaseEdge> queryEdges = new ArrayList<>(_queryEdges);
            BaseEdge queryEdge = queryEdges.stream().filter(e -> match.containsKey(e.getSource()) || match.containsKey(e.getTarget())).findFirst().get();
            queryEdges.remove(queryEdge);
            if (match.containsKey(queryEdge.getSource()) && match.containsKey(queryEdge.getTarget())){
                return querySourceTarget(queryEdges, queryEdge, match, matchedEdges);
            } else if (match.containsKey(queryEdge.getSource())) {
                return queryWithSource(queryEdges, queryEdge, match, matchedEdges);
            } else if (match.containsKey(queryEdge.getTarget())){
                return queryWithTarget(queryEdges, queryEdge, match, matchedEdges);
            } else{
                return queryWithoutPreviousMatch(queryEdges, queryEdge, match, matchedEdges);
            }
        }
    }

    private List<Map<BaseEdge, BaseEdge>> querySourceTarget(List<BaseEdge> queryEdges, BaseEdge queryEdge, Map<BaseNode, BaseNode> match, Map<BaseEdge, BaseEdge> matchedEdges) {
        boolean existsEdge = graph.edges.stream().anyMatch(e ->
                e.getLabel().equals(queryEdge.getLabel())
                        && e.getSource() == match.get(queryEdge.getSource())
                        && e.getTarget() == match.get(queryEdge.getTarget()));
        if (existsEdge){
            return query(queryEdges, match, matchedEdges);
        }
        return new ArrayList<>();
    }

    private List<Map<BaseEdge, BaseEdge>> queryWithSource(List<BaseEdge> queryEdges, BaseEdge queryEdge, Map<BaseNode, BaseNode> match, Map<BaseEdge, BaseEdge> matchedEdges) {
        List<Map<BaseEdge, BaseEdge>> results = new ArrayList<>();
        Stream<BaseEdge> candidates = graph.outEdgesFor(match.get(queryEdge.getSource()).getId()).stream().filter(e ->
                e.getLabel().equals(queryEdge.getLabel())
                && e.getTarget().match(queryEdge.getTarget()));
        if (isInjective){
            candidates = candidates.filter(e -> !match.values().contains(e.getTarget()));
        }
        candidates.forEach(e -> {
            HashMap<BaseNode, BaseNode> newMatch = new HashMap<>(match);
            newMatch.put(queryEdge.getTarget(), e.getTarget());
            Map<BaseEdge, BaseEdge> newMatchedEdges = new HashMap<>(matchedEdges);
            newMatchedEdges.put(queryEdge, e);
            results.addAll(query(queryEdges, newMatch, newMatchedEdges));
        });
        return results;
    }

    private List<Map<BaseEdge, BaseEdge>> queryWithTarget(List<BaseEdge> queryEdges, BaseEdge queryEdge, Map<BaseNode, BaseNode> match, Map<BaseEdge, BaseEdge> matchedEdges) {
        List<Map<BaseEdge, BaseEdge>> results = new ArrayList<>();
        Stream<BaseEdge> candidates = graph.inEdgesFor(match.get(queryEdge.getTarget()).getId()).stream().filter(e ->
                e.getLabel().equals(queryEdge.getLabel())
                && e.getSource().match(queryEdge.getSource()));
        if (isInjective){
            candidates = candidates.filter(e -> !match.values().contains(e.getSource()));
        }
        candidates.forEach(e -> {
            HashMap<BaseNode, BaseNode> newMatch = new HashMap<>(match);
            newMatch.put(queryEdge.getSource(), e.getSource());
            Map<BaseEdge, BaseEdge> newMatchedEdges = new HashMap<>(matchedEdges);
            newMatchedEdges.put(queryEdge, e);
            results.addAll(query(queryEdges, newMatch, newMatchedEdges));
        });
        return results;
    }

    private List<Map<BaseEdge,BaseEdge>> queryWithoutPreviousMatch(List<BaseEdge> queryEdges, BaseEdge queryEdge, Map<BaseNode, BaseNode> match, Map<BaseEdge, BaseEdge> matchedEdges) {
        List<Map<BaseEdge, BaseEdge>> results = new ArrayList<>();
        Stream<BaseEdge> candidateEdges = candidateEdges(queryEdge);
        candidateEdges.forEach(e -> {
            match.put(e.getSource(), queryEdge.getSource());
            match.put(e.getTarget(), queryEdge.getTarget());
            matchedEdges.put(e, queryEdge);
            results.addAll(query(queryEdges, match, matchedEdges));
        });
        return results;
    }
}
