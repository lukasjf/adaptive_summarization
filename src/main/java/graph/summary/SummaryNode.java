package graph.summary;

import graph.BaseNode;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by lukas on 12.03.18.
 */
public class SummaryNode extends BaseNode {

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

    public Set<String> getLabels(){
        return labels;
    }

    public int size(){
        return labels.size();
    }
}
