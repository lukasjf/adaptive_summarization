package main;

import graph.BaseGraph;
import graph.summary.Summary;
import splitstrategies.ExistentialSplitStrategy;
import splitstrategies.VarianceSplitStrategy;

import java.io.File;
import java.util.List;

/**
 * Created by lukas on 12.03.18.
 */
public class Playground {

    public static double runBenchmark(Summary s, File[] queries){
        double precision = 0.0;
        for (File f: queries){
            BaseGraph q = BaseGraph.parseGraph(f.getAbsolutePath());
            System.out.println(f.getAbsolutePath());
            precision += s.measure(q);
        }
        return precision / queries.length;
    }

    public static void main(String[] args){
        BaseGraph graph = BaseGraph.parseGraph("/home/lukas/studium/thesis/code/data/citation/graph_3");

        Summary s = Summary.createFromGraph(graph, new VarianceSplitStrategy());
        for (int i = 0; i < 50; i++){
            s.split();
            System.out.println("split" + i);
        }

        File queryDir = new File("/home/lukas/studium/thesis/code/data/citation/queries");
        for (int i = 0; i < 50; i++) {
            System.out.println(Playground.runBenchmark(s, queryDir.listFiles()));
            s.split();

        }
    }
}
