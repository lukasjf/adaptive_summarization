package summary.merging;

import graph.BaseEdge;
import graph.BaseGraph;
import graph.BaseNode;

import java.util.*;

/**
 * Created by lukas on 26.06.18.
 */
public class GlobalBestMerge implements MergeStrategy{
    @Override
    public int[] search(MergedSummary mSummary, Map<BaseGraph, List<Map<String, String>>> queries) {
        BaseGraph summary = mSummary.summary;
        int[] bestMerge = new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE};
        double bestObjective = Double.MIN_VALUE;
        for (BaseNode n1: summary.getNodes()){
            for (BaseNode n2: summary.getNodes()){
                if (n2.getId() <= n1.getId() || n1.getId() == Integer.MAX_VALUE || n2.getId() == Integer.MAX_VALUE){
                    continue;
                }

                Set<Integer> containedNodesBackup1 = new HashSet<>(n1.getContainedNodes());
                Set<Integer> containedNodesBackup2 = new HashSet<>(n2.getContainedNodes());

                summary.nodeWithId(Integer.MAX_VALUE).getContainedNodes().addAll(n1.getContainedNodes());
                summary.nodeWithId(Integer.MAX_VALUE).getContainedNodes().addAll(n2.getContainedNodes());

                for (BaseEdge e: summary.outEdgesFor(n1.getId())){
                    summary.addEdge(Integer.MAX_VALUE, e.getTarget().getId(), e.getLabel());
                }
                for (BaseEdge e: summary.outEdgesFor(n2.getId())){
                    summary.addEdge(Integer.MAX_VALUE, e.getTarget().getId(), e.getLabel());
                }
                for (BaseEdge e: summary.inEdgesFor(n1.getId())){
                    summary.addEdge(e.getSource().getId(), Integer.MAX_VALUE, e.getLabel());
                }
                for (BaseEdge e: summary.inEdgesFor(n2.getId())){
                    summary.addEdge(e.getSource().getId(), Integer.MAX_VALUE, e.getLabel());
                }

                n1.getContainedNodes().clear();
                n2.getContainedNodes().clear();

                double objective = mSummary.computeTrainingObjective(queries);
                if (objective > bestObjective){
                    bestObjective = objective;
                    bestMerge = new int[] {n1.getId(), n2.getId()};
                    if (objective == 1.0){
                        return bestMerge;
                    }
                }
                n1.getContainedNodes().addAll(containedNodesBackup1);
                n2.getContainedNodes().addAll(containedNodesBackup2);

                summary.nodeWithId(Integer.MAX_VALUE).getContainedNodes().clear();
                List<BaseEdge> edges = new ArrayList<>(summary.outEdgesFor(Integer.MAX_VALUE));
                edges.addAll(summary.inEdgesFor(Integer.MAX_VALUE));
                for (BaseEdge e: edges){
                    summary.removeEdge(e);
                }

            }
        }
        return bestMerge;
    }
}
