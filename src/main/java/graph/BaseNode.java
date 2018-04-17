package graph;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by lukas on 12.03.18.
 */
public class BaseNode implements Serializable {

    private int id;
    Set<Integer> containedNodes;

    public BaseNode(int id){
        this.id = id;
        containedNodes = new HashSet<>();
    }

    @Override
    public String toString(){
        return "Node: " + id;
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
        return id < 0;
    }

    public boolean match(BaseNode queryNode){
        if (queryNode.isVariable()){
            return true;
        } else{
            return containedNodes.contains(queryNode.getId());
        }
    }

    public Set<Integer> getContainedNodes(){
        return containedNodes;
    }

    public int getId() {
        return id;
    }
}
