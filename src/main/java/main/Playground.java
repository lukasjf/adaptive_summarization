package main;

import graph.BaseGraph;
import graph.summary.Summary;
import graph.summary.SummaryEdge;
import splitstrategies.*;

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
            precision += -1 * Math.log(actuals.get(i) / 1.0 / summary.get(i));
        }
        return precision;
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        BaseGraph graph = BaseGraph.parseGraph("/home/lukas/studium/thesis/code/data/citation/graph_3");

        String queryDir = "/home/lukas/studium/thesis/code/data/citation/queries";
        String kddDir = "/home/lukas/studium/thesis/code/data/citation/querieskdd";
        String danaiDir = "/home/lukas/studium/thesis/code/data/citation/queriesdanai";

//        for (int i = 0; i < 100; i++){
//            s.split();
//            System.out.println("split" + i);
//        }
//        new ObjectOutputStream(new FileOutputStream("summary.ser")).writeObject(s);

//        Summary s = (Summary) new ObjectInputStream(new FileInputStream("summary.ser")).readObject();
//        System.out.println("summary loaded");

        runExperiment(graph, new ExistentialSplitStrategy(), queryDir, "results_existential");
        runExperiment(graph, new VarianceSplitStrategy(), queryDir, "results_variance");
        runExperiment(graph, new CombinedSplitStrategy(), queryDir, "results_combined");

        runExperiment(graph, new ExistentialSplitStrategy(), kddDir, "results_kdd_existential");
        runExperiment(graph, new VarianceSplitStrategy(), kddDir, "results_kdd_variance");
        runExperiment(graph, new CombinedSplitStrategy(), kddDir, "results_kdd_combined");

        runExperiment(graph, new ExistentialSplitStrategy(), danaiDir, "results_danai_existential");
        runExperiment(graph, new VarianceSplitStrategy(), danaiDir, "results_danai_variance");
        runExperiment(graph, new CombinedSplitStrategy(), danaiDir, "results_danai_combined");
    }

    private static void runExperiment(BaseGraph graph, SplitStrategy strategy, String queryDir, String fileName) throws FileNotFoundException {
        Summary s = Summary.createFromGraph(graph, strategy);
        PrintStream ps = new PrintStream(fileName);
        int nodeThreshold = graph.getNodes().size() / 10;
        int edgeThreshold = graph.getEdges().size() / 10;
        ps.println("SummaryNodes,SummaryEdges,NodeThreshold,EdgeThreshold,Objective");
        do {
            double objective = Playground.runBenchmark(s, new File(queryDir).listFiles());
            String output = String.format("%d %d %d %d %.4f", s.getNodes().size(), s.getEdges().size(),
                    nodeThreshold, edgeThreshold, objective);
            System.out.println(output);
            ps.println(output.replace(" ", ","));
            s.split();
            s.getEdges().stream().map(e -> (SummaryEdge) e).forEach(e -> e.bookKeeping.clear());
        } while (s.getNodes().size() <= nodeThreshold && s.getEdges().size() <= edgeThreshold);
    }
}
