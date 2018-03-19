package main;

import graph.BaseGraph;
import graph.summary.Summary;
import splitstrategies.VarianceSplitStrategy;

import java.util.List;

/**
 * Created by lukas on 12.03.18.
 */
public class Playground {

    public static void main(String[] args){
        BaseGraph g = BaseGraph.parseGraph("/home/lukas/studium/thesis/code/data/citation/graph_3");

        BaseGraph q = new BaseGraph();
        q.addNode(0, "aut:davide_mottin");
        q.addNode(1, "?");
        //q.addNode(2, "?2");
        q.addEdge(0, 1 , "works_at");
        //q.addEdge(0, 2, "collaborate");

        List<List<String>> result = g.query(q);
        for (List<String> l: result){
            System.out.println(l);
        }

        result = g.query(BaseGraph.parseGraph("/home/lukas/studium/thesis/code/data/citation/queries/query5"));
        for (List<String> l: result){
            System.out.println(l);
        }

        Summary s = Summary.createFromGraph(g, new VarianceSplitStrategy());
        for (int i = 0; i < 5; i++){
            s.split();
        }
        for (int i = 0; i < 10; i++){
            System.out.println("split");
            s.split();
            System.out.println(s.measure(q));
            s.draw();
        }
    }
}
