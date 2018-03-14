package graph;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by lukas on 12.03.18.
 */
public class Node {

    private int id;
    private String label;
    private Set<Edge> outEdges = new HashSet<>();
    private Set<Edge> inEdges = new HashSet<>();

    public Node(int id, String label){
        this.id = id;
        this.label = label;
    }

    @Override
    public String toString(){
        if (label.length() <= 25){
            return label;
        } else{
            return label.substring(0, 25) + "...";
        }
    }

    @Override
    public boolean equals(Object other){
        if (other == null || other.getClass() != getClass()){
            return false;
        } else{
            Node otherNode = (Node) other;
            return id == otherNode.id;
        }
    }

    public boolean isVariable(){
        return label.startsWith("?");
    }

    public boolean match(Node queryNode){
        if (queryNode.label.startsWith("?")){
            return !label.isEmpty();
        } else{
            return label.equals(queryNode.label);
        }
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public Set<Edge> getOutEdges() {
        return outEdges;
    }

    public Set<Edge> getInEdges() {
        return inEdges;
    }
}
