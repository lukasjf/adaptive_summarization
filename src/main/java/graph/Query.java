package graph;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by lukas on 12.03.18.
 */
public class Query extends Graph {

    public static Query parseFromString(String query){
        return null;
    }

    public List<String> getVariables(){
        return getNodes().stream().filter(n -> n.getLabel().startsWith("?"))
                .sorted(Comparator.comparingInt(Node::getId)).map(Node::getLabel).collect(Collectors.toList());
    }
}
