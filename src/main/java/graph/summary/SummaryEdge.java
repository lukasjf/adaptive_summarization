package graph.summary;

import graph.Edge;

/**
 * Created by lukas on 12.03.18.
 */
public class SummaryEdge extends Edge {

    private long actual;

    public SummaryEdge(SummaryNode source, SummaryNode target, String label){
        super(source, target, label);
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

    public SummaryNode getSSource(){
        return (SummaryNode) getSource();
    }

    public SummaryNode getSTarget(){
        return (SummaryNode) getTarget();
    }

    public long getActual() {
        return actual;
    }

    public void setActual(long actual) {
        this.actual = actual;
    }

}
