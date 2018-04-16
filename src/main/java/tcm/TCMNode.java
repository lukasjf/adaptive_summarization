package tcm;

import graph.BaseNode;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by lukas on 16.04.18
 */
public class TCMNode  extends BaseNode{

    Set<String> labels = new HashSet<>();

    public TCMNode(int id) {
        super(id, "");
    }


    @Override
    public String toString(){
        String labelString = labels.stream().collect(Collectors.joining("#"));
        if (labelString.length() < 25){
            return labelString;
        } else{
            return labelString.substring(0, 25) + "...";
        }
    }

    @Override
    public boolean match(BaseNode queryNode){
        if (queryNode.getLabel().startsWith("?")){
            return !labels.isEmpty();
        } else{
            return labels.contains(queryNode.getLabel());
        }
    }

    @Override
    public String getLabel(){
        return labels.stream().collect(Collectors.joining("#"));
    }


}
