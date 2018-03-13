import graph.Graph;
import graph.Query;
import graph.summary.Summary;
import splitstrategies.RandomSplitStrategy;
import splitstrategies.SplitStrategy;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by lukas on 12.03.18.
 */
public class Playground {

    public static void main(String[] args){
        Graph g = Graph.parseGraph("/home/lukas/studium/thesis/code/data/graph_3");

        Query q = new Query();
        q.addNode(0, "aut:davide_mottin");
        q.addNode(1, "?");
        //q.addNode(2, "?2");
        q.addEdge(0, 1 , "works_at");
        //q.addEdge(0, 2, "collaborate");

        List<List<String>> result = g.query(q);
        for (List<String> l: result){
            System.out.println(l);
        }

        Summary s = Summary.createFromGraph(g);
        SplitStrategy strategy = new RandomSplitStrategy(s);
        result = s.query(q);
        System.out.println(result.size());
        s.split();
        System.out.println(s.getNodes().size());
        System.out.println(s.getNodeMapping());
        System.out.println(s.getEdges().size());
    }
}
