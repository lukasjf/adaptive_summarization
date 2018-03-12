package graph.summary;

import graph.Node;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by lukas on 12.03.18.
 */
class SummaryNode extends Node {

    private Set<String> labels;

    public SummaryNode(int id, Set<String> labels){
        super(id, "");
        this.labels = labels;
    }

    @Override
    public String toString(){
        String labelString = labels.stream().collect(Collectors.joining("|"));
        if (labelString.length() < 25){
            return labelString;
        } else{
            return labelString.substring(0, 25) + "...";
        }
    }

    @Override
    public boolean match(Node queryNode){
        if (queryNode.getLabel().startsWith("?")){
            return !labels.isEmpty();
        } else{
            return labels.contains(queryNode.getLabel());
        }
    }

    @Override
    public String getLabel(){
        return toString();
    }

    public int size(){
        return labels.size();
    }
}
