package graph.summary;

import graph.BaseEdge;
import graph.BaseGraph;
import graph.BaseNode;

import java.util.*;
import java.util.stream.Stream;

/**
 * Created by lukas on 26.03.18.
 */
public class SummaryIsomorphism {

    private Summary summary;

    public SummaryIsomorphism(Summary summary){
        this.summary = summary;
    }

    private long candidateCount(BaseEdge queryEdge){
        return candidateEdges(queryEdge).count();
    }

    private Stream<BaseEdge> candidateEdges(BaseEdge queryEdge){
        return summary.getEdges().stream().filter(e ->
                e.getSource().match(queryEdge.getSource())
                        && e.getTarget().match(queryEdge.getTarget())
                        && e.getLabel().equals(queryEdge.getLabel()));
    }

    public List<Map<BaseEdge, SummaryEdge>> query(BaseGraph query){
        List<List<String>> result = new ArrayList<>();
        List<Map<BaseEdge, SummaryEdge>> matchings = new ArrayList<>();

        List<BaseEdge> queryEdges = new ArrayList<>(query.getEdges());
        queryEdges.sort(Comparator.comparingLong(this::candidateCount));
        BaseEdge e = queryEdges.remove(0);
        candidateEdges(e).map(edge -> (SummaryEdge) edge).forEach(edge -> {
            Map<BaseNode, BaseNode> match = new HashMap<>();
            match.put(e.getSource(), edge.getSource());
            match.put(e.getTarget(), edge.getTarget());
            Map<BaseEdge, SummaryEdge> matchedEdges = new HashMap<>();
            matchedEdges.put(e, edge);
            matchings.addAll(query(queryEdges, match, matchedEdges));
        });

        return matchings;
    }

    private List<Map<BaseEdge, SummaryEdge>> query(List<BaseEdge> _queryEdges, Map<BaseNode, BaseNode> match, Map<BaseEdge, SummaryEdge> matchedEdges){
        if (_queryEdges.isEmpty()){
            List<Map<BaseEdge, SummaryEdge>> result = new ArrayList<>();
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

    private List<Map<BaseEdge, SummaryEdge>> querySourceTarget(List<BaseEdge> queryEdges, BaseEdge queryEdge, Map<BaseNode, BaseNode> match, Map<BaseEdge, SummaryEdge> matchedEdges) {
        boolean existsEdge = summary.getEdges().stream().anyMatch(e ->
                e.getLabel().equals(queryEdge.getLabel())
                        && e.getSource() == match.get(queryEdge.getSource())
                        && e.getTarget() == match.get(queryEdge.getTarget()));
        if (existsEdge){
            return query(queryEdges, match, matchedEdges);
        }
        return new ArrayList<>();
    }

    private List<Map<BaseEdge, SummaryEdge>> queryWithSource(List<BaseEdge> queryEdges, BaseEdge queryEdge, Map<BaseNode, BaseNode> match, Map<BaseEdge, SummaryEdge> matchedEdges) {
        List<Map<BaseEdge, SummaryEdge>> results = new ArrayList<>();
        Stream<BaseEdge> candidates = summary.getOutIndex().get(match.get(queryEdge.getSource())).stream().filter(e ->
                e.getLabel().equals(queryEdge.getLabel())
                && e.getTarget().match(queryEdge.getTarget()));
        candidates.map(edge -> (SummaryEdge) edge).forEach(e -> {
            Map<BaseNode, BaseNode> newMatch = new HashMap<>(match);
            newMatch.put(queryEdge.getTarget(), e.getTarget());
            Map<BaseEdge, SummaryEdge> newMatchedEdges = new HashMap<>(matchedEdges);
            newMatchedEdges.put(queryEdge, e);
            results.addAll(query(queryEdges, newMatch, newMatchedEdges));
        });
        return results;
    }

    private List<Map<BaseEdge, SummaryEdge>> queryWithTarget(List<BaseEdge> queryEdges, BaseEdge queryEdge, Map<BaseNode, BaseNode> match, Map<BaseEdge, SummaryEdge> matchedEdges) {
        List<Map<BaseEdge, SummaryEdge>> results = new ArrayList<>();
        Stream<BaseEdge> candidates = summary.getInIndex().get(match.get(queryEdge.getTarget())).stream().filter(e ->
                e.getLabel().equals(queryEdge.getLabel())
                && e.getSource().match(queryEdge.getSource()));
        candidates.map(edge -> (SummaryEdge) edge).forEach(e -> {
            HashMap<BaseNode, BaseNode> newMatch = new HashMap<>(match);
            newMatch.put(queryEdge.getSource(), e.getSource());
            Map<BaseEdge, SummaryEdge> newMatchedEdges = new HashMap<>(matchedEdges);
            newMatchedEdges.put(queryEdge, e);
            results.addAll(query(queryEdges, newMatch, newMatchedEdges));
        });
        return results;
    }
}
