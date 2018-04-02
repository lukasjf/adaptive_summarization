package splitstrategies;

import graph.summary.Summary;
import graph.summary.SummaryEdge;
import graph.summary.SummaryNode;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by lukas on 14.03.18.
 */
public class ExistentialSplitStrategy extends SplitStrategy{


    @Override
    public void split(Summary summary) {
        List<SummaryEdge> criticalEdges = summary.getEdges().stream().map(e -> (SummaryEdge) e)
                .filter(e -> (double) e.bookKeeping.getOrDefault("queryLoss", 0.0) > 0)
                .sorted(Comparator.comparingDouble(sEdge ->
                        -1 * (double) sEdge.bookKeeping.getOrDefault("queryLoss", 0.0)))
                .collect(Collectors.toList());
        SummaryEdge criticalEdge = null;
        do {
            criticalEdge = null;
            if (criticalEdges.isEmpty()){
                break;
            }
            criticalEdge = criticalEdges.remove(0);
        } while(!splitOnEdge(criticalEdge, summary));
        if (criticalEdge == null){
            System.err.println("Existential got stuck");
            System.exit(0);
        }
    }

    public boolean splitOnEdge(SummaryEdge criticalEdge, Summary summary){
        int newNodeId = summary.getNodeMapping().size();

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
        return adjustSummary(summary, splitNode, new1, new2);
    }
}
