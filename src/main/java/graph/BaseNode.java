package graph;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by lukas on 12.03.18.
 */
public class BaseNode {

    private int id;
    private String label;

    public BaseNode(int id, String label){
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
            BaseNode otherNode = (BaseNode) other;
            return id == otherNode.id;
        }
    }

    public boolean isVariable(){
        return label.startsWith("?");
    }

    public boolean match(BaseNode queryNode){
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

    public void setLabel(String label) {
        this.label = label;
    }
}
