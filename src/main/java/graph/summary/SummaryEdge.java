package graph.summary;

import graph.BaseEdge;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lukas on 12.03.18.
 */
public class SummaryEdge extends BaseEdge {

    private long actual;

    public Map<String, Object> bookKeeping = new HashMap<>();

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

    public double getSupport(){
        return actual / 1.0 / size();
    }

    @Override
    public String toString(){
        return String.format("%.4f%s", getSupport(), getLabel());
    }

}
