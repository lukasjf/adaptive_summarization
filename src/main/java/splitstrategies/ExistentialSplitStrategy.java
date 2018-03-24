package splitstrategies;

import graph.summary.Summary;
import graph.summary.SummaryEdge;
import graph.summary.SummaryNode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by lukas on 14.03.18.
 */
public class ExistentialSplitStrategy extends SplitStrategy{


    @Override
    public void split(Summary summary) {
        int newNodeId = summary.getNodeMapping().size();

        SummaryEdge criticalEdge = summary.getEdges().stream().map(e -> (SummaryEdge) e)
                .min(Comparator.comparingDouble(SummaryEdge::getSupport)).get();

        Map<String, Long> sourceConns = new HashMap<>();
        Map<String, Long> targetConns = new HashMap<>();

        for (String label: criticalEdge.getSSource().getLabels()){
            long connectivity = summary.getBaseGraph().getOutIndex().get(summary.getBaseGraph().getLabelMapping().get(label)).stream()
                    .filter(e -> e.getLabel().equals(criticalEdge.getLabel())
                            && criticalEdge.getSTarget().getLabels().contains(e.getTarget().getLabel())).count();
            sourceConns.put(label, connectivity);
        }
        for (String label: criticalEdge.getSTarget().getLabels()) {
            long connectivity = summary.getBaseGraph().getInIndex().get(summary.getBaseGraph().getLabelMapping().get(label)).stream()
                    .filter(e -> e.getLabel().equals(criticalEdge.getLabel())
                            && criticalEdge.getSSource().getLabels().contains(e.getSource().getLabel())).count();
            targetConns.put(label, connectivity);
        }

        double sourceSupport = criticalEdge.getSSource().getLabels().stream().filter(l -> sourceConns.get(l) > 0)
                .count() / 1.0 / criticalEdge.getSSource().size();
        double targetSupport = criticalEdge.getSTarget().getLabels().stream().filter(l -> targetConns.get(l) > 0)
                .count() / 1.0 / criticalEdge.getSTarget().size();

        SummaryNode splitNode;
        boolean isSource;

        if (sourceSupport <= targetSupport){
            splitNode = criticalEdge.getSSource();
            isSource = true;
        } else{
            splitNode = criticalEdge.getSTarget();
            isSource = false;
        }

        Set<String> nodesWithEdge= new HashSet<>();
        Set<String> nodesWithoutEdge = new HashSet<>();

        if (isSource){
            for (String label: splitNode.getLabels()){
                if (sourceConns.get(label) > 0L){
                    nodesWithEdge.add(label);
                } else{
                    nodesWithoutEdge.add(label);
                }
            }
        } else{
            for (String label: splitNode.getLabels()){
                if (targetConns.get(label) > 0L){
                    nodesWithEdge.add(label);
                } else{
                    nodesWithoutEdge.add(label);
                }
            }
        }

        SummaryNode new1 = new SummaryNode(splitNode.getId(), nodesWithEdge);
        SummaryNode new2 = new SummaryNode(newNodeId, nodesWithoutEdge);
        adjustSummary(summary, splitNode, new1, new2);
    }
}
