package graph;

/**
 * Created by lukas on 12.03.18.
 */
public class Edge {

    private Node source;
    private Node target;
    private String label;

    public Edge(Node source, Node target, String label){
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
            Edge otherEdge = (Edge) other;
            return source.getId() == otherEdge.source.getId()
                    && target.getId() == otherEdge.target.getId()
                    && label.equals(otherEdge.label);
        }
    }

    public Node getSource() {
        return source;
    }

    public Node getTarget() {
        return target;
    }

    public String getLabel() {
        return label;
    }
}
