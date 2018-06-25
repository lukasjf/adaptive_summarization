package summary.merging;

import encoding.GraphEncoder;
import evaluation.Benchmarkable;
import evaluation.F1Score;
import graph.*;

import java.util.*;

/**
 * Created by lukas on 25.06.18.
 */
public class MergedSummary implements Benchmarkable {

    private BaseGraph original;
    private BaseGraph summary;
    int sizeLimit;

    public MergedSummary(BaseGraph originalGraph, int sizeLimit){
        this.original = originalGraph;
        this.summary = new BaseGraph();
        this.sizeLimit = sizeLimit;
    }

    @Override
    public List<Map<String, String>> query(BaseGraph query) {
        return new SubgraphIsomorphism().query(query, summary, false);
    }

    @Override
    public void train(Map<BaseGraph, List<Map<String, String>>> queries) {

        for (BaseNode n: original.getNodes()){
            summary.addNode(n.getId(), Dataset.I.labelFrom(n.getId()));
            summary.nodeWithId(n.getId()).getContainedNodes().addAll(n.getContainedNodes());
        }
        summary.addNode(Integer.MAX_VALUE, "");

        for (BaseEdge e: original.getEdges()){
            summary.addEdge(e.getSource().getId(), e.getTarget().getId(), e.getLabel());
        }



        GraphEncoder encoder = new GraphEncoder();
        while (encoder.encode(summary) > sizeLimit){

            int[] bestMerge = new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE};
            double bestObjective = Double.MIN_VALUE;

            System.out.println(encoder.encode(summary));
            for (BaseNode n1: summary.getNodes()){
                for (BaseNode n2: summary.getNodes()){
                    if (n2.getId() <= n1.getId()){
                        continue;
                    }

                    Set<Integer> containedNodesBackup1 = new HashSet<>(n1.getContainedNodes());
                    Set<Integer> containedNodesBackup2 = new HashSet<>(n2.getContainedNodes());

                    List<BaseEdge> outEdgesBackup1 = new ArrayList<>(summary.outEdgesFor(n1.getId()));
                    List<BaseEdge> inEdgesBackup1 = new ArrayList<>(summary.inEdgesFor(n1.getId()));

                    List<BaseEdge> outEdgesBackup2 = new ArrayList<>(summary.outEdgesFor(n2.getId()));
                    List<BaseEdge> inEdgesBackup2 = new ArrayList<>(summary.inEdgesFor(n2.getId()));

                    summary.nodeWithId(Integer.MAX_VALUE).getContainedNodes().addAll(n1.getContainedNodes());
                    summary.nodeWithId(Integer.MAX_VALUE).getContainedNodes().addAll(n2.getContainedNodes());

                    for (BaseEdge e: outEdgesBackup1){
                        summary.addEdge(Integer.MAX_VALUE, e.getTarget().getId(), e.getLabel());
                    }
                    for (BaseEdge e: outEdgesBackup2){
                        summary.addEdge(Integer.MAX_VALUE, e.getTarget().getId(), e.getLabel());
                    }
                    for (BaseEdge e: inEdgesBackup1){
                        summary.addEdge(e.getSource().getId(), Integer.MAX_VALUE, e.getLabel());
                    }
                    for (BaseEdge e: inEdgesBackup2){
                        summary.addEdge(e.getSource().getId(), Integer.MAX_VALUE, e.getLabel());
                    }
                    
                    n1.getContainedNodes().clear();
                    n2.getContainedNodes().clear();


                    double objective = computeTrainingObjective(queries);
                    if (objective > bestObjective){
                        bestObjective = objective;
                        bestMerge = new int[] {n1.getId(), n2.getId()};
                    }

                    summary.nodeWithId(Integer.MAX_VALUE).getContainedNodes().clear();
                    List<BaseEdge> edges = new ArrayList<>(summary.outEdgesFor(Integer.MAX_VALUE));
                    edges.addAll(summary.inEdgesFor(Integer.MAX_VALUE));
                    for (BaseEdge e: edges){
                        summary.removeEdge(e);
                    }
                    n1.getContainedNodes().addAll(containedNodesBackup1);
                    n2.getContainedNodes().addAll(containedNodesBackup2);
                }
            }

            System.out.println(Arrays.toString(bestMerge));
        }
    }

    private double computeTrainingObjective(Map<BaseGraph, List<Map<String, String>>> queries) {
        double objective = 0.0;
        for (BaseGraph query: queries.keySet()){
            List<Map<String, String>> summaryResults = new SubgraphIsomorphism().query(query, summary, false);
            objective += F1Score.fqScoreFor(queries.get(query), summaryResults);
        }
        return objective / queries.size();
    }
}
