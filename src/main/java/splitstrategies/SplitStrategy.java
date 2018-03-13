package splitstrategies;

import graph.Edge;
import graph.summary.Summary;
import graph.summary.SummaryEdge;
import graph.summary.SummaryNode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by lukas on 13.03.18.
 */
public abstract class SplitStrategy {

    protected Summary summary;

    public SplitStrategy(Summary summary){
        this.summary = summary;
        summary.setStrategy(this);
    }

    public abstract void split();

    protected void adjustLabels(SummaryNode splitNode, SummaryNode new1, SummaryNode new2){
        summary.getNodes().remove(splitNode);
        summary.getNodeMapping().remove(splitNode.getId());
        summary.addNode(new1);
        summary.addNode(new2);
        List<SummaryEdge> toDoEdges = summary.getEdges().stream().map(e -> (SummaryEdge) e)
                .filter(e -> e.getSource() == splitNode || e.getTarget() == splitNode).collect(Collectors.toList());
        for (SummaryEdge e: toDoEdges){
            summary.getEdges().remove(e);
            if (e.getSource() == splitNode && e.getTarget() == splitNode){
                checkNewEdge(new1, new1, e.getLabel());
                checkNewEdge(new1, new2, e.getLabel());
                checkNewEdge(new2, new1, e.getLabel());
                checkNewEdge(new2, new2, e.getLabel());
            } else if (e.getSource() == splitNode){
                checkNewEdge(new1, e.getSTarget(), e.getLabel());
                checkNewEdge(new2, e.getSTarget(), e.getLabel());
            } else{
                checkNewEdge(e.getSSource(), new1, e.getLabel());
                checkNewEdge(e.getSSource(), new2, e.getLabel());
            }
        }
    }

    public void checkNewEdge(SummaryNode source, SummaryNode target, String label){
        if (supportCountFor(source, target, label) > 0){
            summary.addEdge(source.getId(), target.getId(), label);
        }
    }

    public long supportCountFor(SummaryNode source, SummaryNode target, String label){
        return summary.getGraph().getEdges().stream().filter(e -> label.equals(e.getLabel())
                && source.getLabels().contains(e.getSource().getLabel())
                && target.getLabels().contains(e.getTarget().getLabel())).count();
    }
}
