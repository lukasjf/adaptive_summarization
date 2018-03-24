package splitstrategies;

import graph.BaseEdge;
import graph.summary.Summary;
import graph.summary.SummaryEdge;
import graph.summary.SummaryNode;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by lukas on 13.03.18.
 */
public abstract class SplitStrategy implements Serializable {

    public abstract void split(Summary summary);

    protected void adjustSummary(Summary summary, SummaryNode splitNode, SummaryNode new1, SummaryNode new2){

        if (new1.size() == 0 || new2.size() == 0) {
            System.err.println("Split did not work, empty Node created");
            return;
        }

        summary.getNodes().remove(splitNode);
        summary.getNodeMapping().remove(splitNode.getId());
        summary.getLabelMapping().remove(splitNode.getLabel());
        for (BaseEdge e : summary.getInIndex().remove(splitNode)){
            summary.getOutIndex().get(e.getSource()).remove(e);
        }
        for (BaseEdge e : summary.getOutIndex().remove(splitNode)){
            summary.getInIndex().get(e.getTarget()).remove(e);
        }
        summary.addNode(new1);
        summary.addNode(new2);
        List<SummaryEdge> toDoEdges = summary.getEdges().stream().map(e -> (SummaryEdge) e)
                .filter(e -> e.getSource() == splitNode || e.getTarget() == splitNode).collect(Collectors.toList());
        for (SummaryEdge e: toDoEdges){
            summary.getEdges().remove(e);
            if (e.getSource() == splitNode && e.getTarget() == splitNode){
                checkNewEdge(summary, new1, new1, e.getLabel());
                checkNewEdge(summary, new1, new2, e.getLabel());
                checkNewEdge(summary, new2, new1, e.getLabel());
                checkNewEdge(summary, new2, new2, e.getLabel());
            } else if (e.getSource() == splitNode){
                checkNewEdge(summary, new1, e.getSTarget(), e.getLabel());
                checkNewEdge(summary, new2, e.getSTarget(), e.getLabel());
            } else{
                checkNewEdge(summary, e.getSSource(), new1, e.getLabel());
                checkNewEdge(summary, e.getSSource(), new2, e.getLabel());
            }
        }
    }

    public void checkNewEdge(Summary summary, SummaryNode source, SummaryNode target, String label){
        if (supportCountFor(summary, source, target, label) > 0){
            summary.addSEdge(source, target, label);
        }
    }

    public long supportCountFor(Summary summary, SummaryNode source, SummaryNode target, String label){
        return summary.getBaseGraph().getEdges().stream().filter(e -> label.equals(e.getLabel())
                && source.getLabels().contains(e.getSource().getLabel())
                && target.getLabels().contains(e.getTarget().getLabel())).count();
    }
}
