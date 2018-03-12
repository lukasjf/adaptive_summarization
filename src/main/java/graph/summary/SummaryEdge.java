package graph.summary;

import graph.Edge;

/**
 * Created by lukas on 12.03.18.
 */
class SummaryEdge extends Edge {

    int actual;

    public SummaryEdge(SummaryNode source, SummaryNode target, String label, int actual){
        super(source, target, label);
        this.actual = actual;
    }

    public double support(){
        return actual / 1.0 / size();
    }

    public int size(){
        int size = getSSource().size() * getSTarget().size();
        if (getSTarget() == getSSource()){
            size = size - getSTarget().size();
        }
        return size;
    }

    SummaryNode getSSource(){
        return (SummaryNode) getSource();
    }

    SummaryNode getSTarget(){
        return (SummaryNode) getTarget();
    }
}
