package splitstrategies;

import graph.summary.Summary;
import graph.summary.SummaryEdge;
import graph.summary.SummaryNode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by lukas on 14.03.18.
 */
public class VarianceSplitStrategy extends SplitStrategy {

    @Override
    public void split(Summary summary) {
        int newNodeId = summary.getNodeMapping().size();

        SummaryEdge criticalEdge = summary.getEdges().stream().map(e -> (SummaryEdge) e)
                .min(Comparator.comparingDouble(SummaryEdge::getSupport)).get();

        System.out.println(criticalEdge.getLabel());
        System.out.println(criticalEdge.getSSource() + "  ,   " + criticalEdge.getSTarget());
        long actual = criticalEdge.getActual();

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


//        if (sourceConns.values().contains(0L) || targetConns.values().contains(0L)){
//            System.out.println("do existential split");
//            new ExistentialSplitStrategy().split(summary);
//            return;
//        }

        double sourceMean, sourceVariance;
        double targetMean, targetVariance;

        sourceMean = sourceConns.values().stream().mapToLong(Long::longValue).sum() / 1.0 / sourceConns.size();
        targetMean = targetConns.values().stream().mapToLong(Long::longValue).sum() / 1.0 / targetConns.size();

        sourceVariance = sourceConns.values().stream().map(l -> (l - sourceMean) * (l - sourceMean))
                .mapToDouble(Double::doubleValue).sum();

        targetVariance = targetConns.values().stream().map(l -> (l - targetMean) * (l - targetMean))
                .mapToDouble(Double::doubleValue).sum();

//        System.out.println(criticalEdge.getSSource().size() + "    " + criticalEdge.getSTarget().size());
//        System.out.println(String.format("%f.3(%d,%d)    + %f.3(%d,%d)",
//                sourceVariance, Collections.max(sourceConns.values()), Collections.min(sourceConns.values()),
//                targetVariance, Collections.max(targetConns.values()), Collections.min(targetConns.values())));

        SummaryNode splitNode;
        Set<String> new1 = new HashSet<>();
        Set<String> new2 = new HashSet<>();

        if (sourceVariance >= targetVariance){
            splitNode = criticalEdge.getSSource();
            long minConn = sourceConns.values().stream().min(Long::compare).get();
            long maxConn = sourceConns.values().stream().max(Long::compare).get();
            for (String label: sourceConns.keySet()){
                if (maxConn - sourceConns.get(label) <= sourceConns.get(label) - minConn){
                    new1.add(label);
                } else{
                    new2.add(label);
                }
            }
        } else{
            splitNode = criticalEdge.getSTarget();
            long minConn = targetConns.values().stream().min(Long::compare).get();
            long maxConn = targetConns.values().stream().max(Long::compare).get();
            for (String label: targetConns.keySet()){
                if (maxConn - targetConns.get(label) < targetConns.get(label) - minConn){
                    new1.add(label);
                } else{
                    new2.add(label);
                }
            }
        }

        SummaryNode newNode1 = new SummaryNode(splitNode.getId(), new1);
        SummaryNode newNode2 = new SummaryNode(newNodeId, new2);

        if (new1.size() > 0 && new2.size() > 0){
            adjustSummary(summary, splitNode, newNode1, newNode2);
        } else{
            System.err.println("Split did not work, empty Node created");
        }
    }
}
