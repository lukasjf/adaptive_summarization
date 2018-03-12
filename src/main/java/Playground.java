import graph.Graph;

import java.util.List;
import java.util.Map;

/**
 * Created by lukas on 12.03.18.
 */
public class Playground {

    public static void main(String[] args){
        Graph g = Graph.parseGraph("/home/lukas/studium/thesis/code/data/graph_3");

        Graph q = new Graph();
        q.addNode(0, "aut:davide_mottin");
        q.addNode(1, "?");
        q.addEdge(0, 1 , "works_at");

        List<Map<String, String>> result = g.query(q);
        System.out.println(result);
    }
}
