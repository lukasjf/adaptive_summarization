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

    protected boolean adjustSummary(Summary summary, SummaryNode splitNode, SummaryNode new1, SummaryNode new2){

        if (new1.size() == 0 || new2.size() == 0) {
            System.err.println("Split did not work, empty Node created");
            return false;
        }

        List<SummaryEdge> toDoEdges = summary.getEdges().stream().map(e -> (SummaryEdge) e)
                .filter(e -> e.getSource() == splitNode || e.getTarget() == splitNode).collect(Collectors.toList());

        summary.removeNode(splitNode);
        summary.addNode(new1);
        summary.addNode(new2);

        for (SummaryEdge e: toDoEdges){
            if (e.getSource() == splitNode && e.getTarget() == splitNode){
                summary.addSEdge(new1, new1, e.getLabel());
                summary.addSEdge(new1, new2, e.getLabel());
                summary.addSEdge(new2, new1, e.getLabel());
                summary.addSEdge(new2, new2, e.getLabel());
            } else if (e.getSource() == splitNode){
                summary.addSEdge(new1, e.getSTarget(), e.getLabel());
                summary.addSEdge(new2, e.getSTarget(), e.getLabel());
            } else{
                summary.addSEdge(e.getSSource(), new1, e.getLabel());
                summary.addSEdge(e.getSSource(), new2, e.getLabel());
            }
        }
        return true;
    }
}
