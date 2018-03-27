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

        List<String> variables = query.getVariables();
        for (Map<String, String> m : res){
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
                return queryWithSource(queryEdges, queryEdge, match);
            } else{
                return queryWithTarget(queryEdges, queryEdge, match);
            }
        }
    }

    private List<Map<String,String>> querySourceTarget(List<BaseEdge> queryEdges, BaseEdge queryEdge, Map<BaseNode, BaseNode> match) {
        boolean existsEdge = summary.getEdges().stream().anyMatch(e ->
                e.getLabel().equals(queryEdge.getLabel())
                        && e.getSource() == match.get(queryEdge.getSource())
                        && e.getTarget() == match.get(queryEdge.getTarget()));
        if (existsEdge){
            return query(queryEdges, match);
        }
        return new ArrayList<>();
    }

    private List<Map<String,String>> queryWithSource(List<BaseEdge> queryEdges, BaseEdge queryEdge, Map<BaseNode, BaseNode> match) {
        List<Map<String, String>> results = new ArrayList<>();
        Stream<BaseEdge> candidates = summary.getOutIndex().get(match.get(queryEdge.getSource())).stream().filter(e ->
                e.getLabel().equals(queryEdge.getLabel())
                && e.getTarget().match(queryEdge.getTarget()));
        candidates.forEach(e -> {
            HashMap<BaseNode, BaseNode> newMatch = new HashMap<>(match);
            newMatch.put(queryEdge.getTarget(), e.getTarget());
            results.addAll(query(queryEdges, newMatch));
        });
        return results;
    }

    private List<Map<String,String>> queryWithTarget(List<BaseEdge> queryEdges, BaseEdge queryEdge, Map<BaseNode, BaseNode> match) {
        List<Map<String, String>> results = new ArrayList<>();
        Stream<BaseEdge> candidates = summary.getInIndex().get(match.get(queryEdge.getTarget())).stream().filter(e ->
                e.getLabel().equals(queryEdge.getLabel())
                && e.getSource().match(queryEdge.getSource()));
        candidates.forEach(e -> {
            HashMap<BaseNode, BaseNode> newMatch = new HashMap<>(match);
            newMatch.put(queryEdge.getSource(), e.getSource());
            results.addAll(query(queryEdges, newMatch));
        });
        return results;
    }
}
