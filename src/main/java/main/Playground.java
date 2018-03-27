package main;

import graph.BaseGraph;
import graph.summary.Summary;
import splitstrategies.ExistentialSplitStrategy;
import splitstrategies.RandomSplitStrategy;
import splitstrategies.VarianceSplitStrategy;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by lukas on 12.03.18.
 */
public class Playground {

    private static HashMap<String, Integer> queryResults = new HashMap<>();

    public static double runBenchmark(Summary s, File[] queries){
        double precision = 0.0;
        List<String> queriyfiles = new ArrayList<>();
        List<Integer> actuals = new ArrayList<>();
        List<Long> summary = new ArrayList<>();
        for (File f: queries){
            BaseGraph q = BaseGraph.parseGraph(f.getAbsolutePath());
            int actualResults;
            if (queryResults.containsKey(f.getAbsolutePath())){
                actualResults = queryResults.get(f.getAbsolutePath());
            } else{
                actualResults = s.getBaseGraph().query(q).size();
                queryResults.put(f.getAbsolutePath(), actualResults);
            }
            long summaryResults = s.getResultSize(q);
            queriyfiles.add(f.getAbsolutePath());
            actuals.add(actualResults);
            summary.add(summaryResults);
        }
        for (int i = 0; i < actuals.size(); i++){
            precision += actuals.get(i) / 1.0 / summary.get(i);
        }
        return precision / queries.length;
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        BaseGraph graph = BaseGraph.parseGraph("/home/lukas/studium/thesis/code/data/citation/graph_3");

        File queryDir = new File("/home/lukas/studium/thesis/code/data/citation/queries");


//        for (int i = 0; i < 100; i++){
//            s.split();
//            System.out.println("split" + i);
//        }
//        new ObjectOutputStream(new FileOutputStream("summary.ser")).writeObject(s);

//        Summary s = (Summary) new ObjectInputStream(new FileInputStream("summary.ser")).readObject();
//        System.out.println("summary loaded");

        Summary s = Summary.createFromGraph(graph, new ExistentialSplitStrategy());
        PrintStream variance = new PrintStream("variance");
        for (int i = 0; i < 250; i++) {
            double objective = Playground.runBenchmark(s, queryDir.listFiles());
            System.out.println(i + ": " + objective);
            variance.println(s.getNodes().size() / 1.0 / graph.getNodes().size()+ "," + objective);
            //s.draw();
            s.split();
        }
    }
}
