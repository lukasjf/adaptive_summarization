package summary.merging;

import encoding.GraphEncoder;
import encoding.SummaryEncoder;
import evaluation.Benchmarkable;
import evaluation.F1Score;
import graph.*;

import java.util.*;

/**
 * Created by lukas on 25.06.18.
 */
public class MergedSummary implements Benchmarkable {

    BaseGraph original;
    BaseGraph summary;
    private int sizeLimit;
    private MergeStrategy strategy;

    public MergedSummary(BaseGraph originalGraph, MergeStrategy strategy, int sizeLimit){
        this.original = originalGraph;
        this.summary = new BaseGraph();
        this.sizeLimit = sizeLimit;
        this.strategy = strategy;
    }

    @Override
    public List<Map<String, String>> query(BaseGraph query) {
        return new SubgraphIsomorphism().query(query, summary, false);
    }

    @Override
    public List<Map<String, String>> query(BaseGraph query, int timeout) {
        return new SubgraphIsomorphism(timeout).query(query, summary, false);
    }

    @Override
    public void train(Map<BaseGraph, List<Map<String, String>>> queries) {

        for (BaseNode n: original.getNodes()){
            summary.addNode(n.getId(), Dataset.I.labelFrom(n.getId()));
            summary.nodeWithId(n.getId()).getContainedNodes().addAll(n.getContainedNodes());
        }

        for (BaseEdge e: original.getEdges()){
            summary.addEdge(e.getSource().getId(), e.getTarget().getId(), e.getLabel());
        }



        GraphEncoder encoder = new GraphEncoder();
        while (encoder.encode(summary) > sizeLimit && summary.getNodes().size() > 1){
            summary.addNode(Integer.MAX_VALUE, "");

            System.out.println(encoder.encode(summary));

            int[] bestMerge = strategy.search(this, queries);
            System.out.println(Arrays.toString(bestMerge));
            summary.removeNode(Integer.MAX_VALUE);
            int to = bestMerge[0];
            int from = bestMerge[1];
            summary.nodeWithId(to).getContainedNodes().addAll(summary.nodeWithId(from).getContainedNodes());
            for (BaseEdge e: summary.outEdgesFor(from)){
                summary.addEdge(to, e.getTarget().getId(), e.getLabel());
            }
            for (BaseEdge e: summary.inEdgesFor(from)){
                summary.addEdge(e.getSource().getId(), to, e.getLabel());
            }
            summary.removeNode(from);
        }
    }

    @Override
    public long size() {
        return new SummaryEncoder().encode(summary);
    }

    double computeTrainingObjective(Map<BaseGraph, List<Map<String, String>>> queries) {
        double objective = 0.0;
        for (BaseGraph query: queries.keySet()){
            List<Map<String, String>> summaryResults = new SubgraphIsomorphism().query(query, summary, false);
            objective += F1Score.fqScoreFor(queries.get(query), summaryResults);
        }
        return objective / queries.size();
    }
}
