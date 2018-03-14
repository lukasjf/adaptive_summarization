package graph;

/**
 * Created by lukas on 12.03.18.
 */
public class BaseEdge {

    private BaseNode source;
    private BaseNode target;
    private String label;

    public BaseEdge(BaseNode source, BaseNode target, String label){
        this.source = source;
        this.target = target;
        this.label = label;
    }

    @Override
    public String toString(){
        return label;
    }

    @Override
    public boolean equals(Object other){
        if (other == null || getClass() != other.getClass()){
            return false;
        } else{
            BaseEdge otherEdge = (BaseEdge) other;
            return source.getId() == otherEdge.source.getId()
                    && target.getId() == otherEdge.target.getId()
                    && label.equals(otherEdge.label);
        }
    }

    public BaseNode getSource() {
        return source;
    }

    public BaseNode getTarget() {
        return target;
    }

    public String getLabel() {
        return label;
    }
}
