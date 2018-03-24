package main;

import graph.BaseGraph;
import graph.summary.Summary;
import splitstrategies.ExistentialSplitStrategy;
import splitstrategies.VarianceSplitStrategy;

import java.io.*;
import java.util.List;

/**
 * Created by lukas on 12.03.18.
 */
public class Playground {

    public static double runBenchmark(Summary s, File[] queries){
        double precision = 0.0;
        for (File f: queries){
            BaseGraph q = BaseGraph.parseGraph(f.getAbsolutePath());
            precision += s.measure2(q);
        }
        return precision / queries.length;
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        BaseGraph graph = BaseGraph.parseGraph("/home/lukas/studium/thesis/code/data/citation/graph_3");

        Summary s = Summary.createFromGraph(graph, new VarianceSplitStrategy());
//        for (int i = 0; i < 100; i++){
//            s.split();
//            System.out.println("split" + i);
//        }
//        new ObjectOutputStream(new FileOutputStream("summary.ser")).writeObject(s);

//        Summary s = (Summary) new ObjectInputStream(new FileInputStream("summary.ser")).readObject();
//        System.out.println("summary loaded");

        File queryDir = new File("/home/lukas/studium/thesis/code/data/citation/queries");
        PrintStream ps = new PrintStream("variance");
        for (int i = 0; i < 1000; i++) {
            double objective = Playground.runBenchmark(s, queryDir.listFiles());
            System.out.println(objective);
            ps.println(s.getNodes().size() / 1.0 / graph.getNodes().size()+ "," + objective);
            s.split();
        }
    }
}
