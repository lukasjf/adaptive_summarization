package splitstrategies;

import graph.summary.Summary;
import graph.summary.SummaryEdge;
import graph.summary.SummaryNode;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

        List<String> trueSourceLabels = summary.getGraph().getEdges().stream().filter(e ->
                e.getLabel().equals(criticalEdge.getLabel())
                && criticalEdge.getSTarget().getLabels().contains(e.getTarget().getLabel()))
            .map(e -> e.getSource().getLabel()).collect(Collectors.toList());

        List<String> trueTargetLabels = summary.getGraph().getEdges().stream().filter(e ->
                e.getLabel().equals(criticalEdge.getLabel())
                && criticalEdge.getSSource().getLabels().contains(e.getSource().getLabel()))
            .map(e -> e.getTarget().getLabel()).collect(Collectors.toList());

        double sourceSupport = criticalEdge.getSSource().getLabels().stream().filter(trueSourceLabels::contains)
                .count() / 1.0 / criticalEdge.getSSource().size();
        double targetSupport = criticalEdge.getSTarget().getLabels().stream().filter(trueTargetLabels::contains)
                .count() / 1.0 / criticalEdge.getSTarget().size();

        SummaryNode splitNode;
        boolean splitSource;

        if (sourceSupport <= targetSupport){
            splitNode = criticalEdge.getSSource();
            splitSource = true;
        } else{
            splitNode = criticalEdge.getSTarget();
            splitSource = false;
        }

        Set<String> nodesWithEdge= new HashSet<>();
        Set<String> nodesWithoutEdge = new HashSet<>();

        if (splitSource){
            for (String label: splitNode.getLabels()){
                if (trueSourceLabels.contains(label)){
                    nodesWithEdge.add(label);
                } else{
                    nodesWithoutEdge.add(label);
                }
            }
        } else{
            for (String label: splitNode.getLabels()){
                if (trueTargetLabels.contains(label)){
                    nodesWithEdge.add(label);
                } else{
                    nodesWithoutEdge.add(label);
                }
            }
        }

        SummaryNode new1 = new SummaryNode(splitNode.getId(), nodesWithEdge);
        SummaryNode new2 = new SummaryNode(newNodeId, nodesWithoutEdge);
        adjustLabels(summary, splitNode, new1, new2);
    }
}