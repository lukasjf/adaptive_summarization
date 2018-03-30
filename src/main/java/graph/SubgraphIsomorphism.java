package graph;

import graph.summary.SummaryEdge;

import java.util.*;
import java.util.stream.Stream;

/**
 * Created by lukas on 26.03.18.
 */
public class SubgraphIsomorphism {

    protected BaseGraph graph;

    public SubgraphIsomorphism(BaseGraph graph) {
        this.graph = graph;
    }

    private long candidateCount(BaseEdge queryEdge){
        return candidateEdges(queryEdge).count();
    }

    private Stream<BaseEdge> candidateEdges(BaseEdge queryEdge){
        return graph.getEdges().stream().filter(e ->
                e.getSource().match(queryEdge.getSource())
                        && e.getTarget().match(queryEdge.getTarget())
                        && e.getLabel().equals(queryEdge.getLabel()));
    }

    public List<Map<BaseEdge, BaseEdge>> query(BaseGraph query){
        List<Map<BaseEdge, BaseEdge>> matchings = new ArrayList<>();

        List<BaseEdge> queryEdges = new ArrayList<>(query.getEdges());
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
        return matchings;
    }

    private List<Map<BaseEdge, BaseEdge>> query(List<BaseEdge> _queryEdges, Map<BaseNode, BaseNode> match, Map<BaseEdge, BaseEdge> matchedEdges){
        if (_queryEdges.isEmpty()){
            List<Map<BaseEdge, BaseEdge>> result = new ArrayList<>();
            result.add(matchedEdges);
            return result;
        } else{
            List <BaseEdge> queryEdges = new ArrayList<>(_queryEdges);
            BaseEdge queryEdge = queryEdges.remove(0);
            if (match.containsKey(queryEdge.getSource()) && match.containsKey(queryEdge.getTarget())){
                return querySourceTarget(queryEdges, queryEdge, match, matchedEdges);
            } else if (match.containsKey(queryEdge.getSource())) {
                return queryWithSource(queryEdges, queryEdge, match, matchedEdges);
            } else{
                return queryWithTarget(queryEdges, queryEdge, match, matchedEdges);
            }
        }
    }

    private List<Map<BaseEdge, BaseEdge>> querySourceTarget(List<BaseEdge> queryEdges, BaseEdge queryEdge, Map<BaseNode, BaseNode> match, Map<BaseEdge, BaseEdge> matchedEdges) {
        boolean existsEdge = graph.getEdges().stream().anyMatch(e ->
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
        Stream<BaseEdge> candidates = graph.getOutIndex().get(match.get(queryEdge.getSource())).stream().filter(e ->
                e.getLabel().equals(queryEdge.getLabel())
                && e.getTarget().match(queryEdge.getTarget())
                && !match.values().contains(e.getTarget()));
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
        Stream<BaseEdge> candidates = graph.getInIndex().get(match.get(queryEdge.getTarget())).stream().filter(e ->
                e.getLabel().equals(queryEdge.getLabel())
                && e.getSource().match(queryEdge.getSource())
                && !match.values().contains(e.getSource()));
        candidates.forEach(e -> {
            HashMap<BaseNode, BaseNode> newMatch = new HashMap<>(match);
            newMatch.put(queryEdge.getSource(), e.getSource());
            Map<BaseEdge, BaseEdge> newMatchedEdges = new HashMap<>(matchedEdges);
            newMatchedEdges.put(queryEdge, e);
            results.addAll(query(queryEdges, newMatch, newMatchedEdges));
        });
        return results;
    }
}
