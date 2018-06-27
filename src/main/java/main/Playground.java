package main;

import evaluation.Benchmark;
import evaluation.Benchmarkable;
import evaluation.F1Score;
import graph.*;
import summary.caching.SummaryCache;
import summary.equivalences.MinimalEquivalence;
import summary.equivalences.QueryNodeEquivalence;
import summary.equivalences.QuotientGraph;
import summary.equivalences.TotalEquivalence;
import summary.merging.MergedSummary;
import summary.topdown.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lukas on 12.03.18.
 */
public class Playground {

    private static HashMap<BaseGraph, List<Map<String, String>>> queryResults = new HashMap<>();

    public static double runBenchmark(GraphQueryAble summaryGraph, BaseGraph graph, File[] queries){
        double precision = 0.0;
        for (File f: queries) {
            BaseGraph q = GraphImporter.parseGraph(f.getAbsolutePath());
            precision += runQuery(q, graph, summaryGraph);
            System.out.print(".");
        }
        return precision / queries.length;
    }

    private static double runQuery(BaseGraph q, BaseGraph graph, GraphQueryAble summaryGraph) {
        List<Map<String, String>> actualResults;
        if (queryResults.containsKey(q)) {
            actualResults = queryResults.get(q);
        } else {
            actualResults = graph.query(q);
            queryResults.put(q, actualResults);
        }
        double f1 = F1Score.fqScoreFor(actualResults, summaryGraph.query(q));
        System.out.println(f1);
        return f1;
    }

    public static void main(String[] args) throws IOException {
        SplitStrategy.algorithms.put("existential", new ExistentialSplitStrategy());
        SplitStrategy.algorithms.put("variance", new VarianceSplitStrategy());
        SplitStrategy.algorithms.put("combined", new CombinedSplitStrategy());
        SplitChoiceStrategy.algorithms.put("greedy", new GreedyChoice());
        SplitChoiceStrategy.algorithms.put("loss", new LossChoice());

        int sizeLimit = Integer.parseInt(args[0]);
        String dataPath = args[1];
        String queriesPath = args[2];

        Dataset dataset = new Dataset(dataPath);
        //Benchmarkable summary = new QuotientGraph(dataset.getGraph(), new MinimalEquivalence());
        //Benchmarkable summary = new MergedSummary(dataset.getGraph(), sizeLimit);
        Benchmarkable summary = new SummaryCache(sizeLimit);
        Benchmark b = new Benchmark(queriesPath);
        double[] result = b.run(summary, dataset.getGraph());
        System.out.println();
        System.out.println("Training: " + result[0]);
        System.out.println("Test: " + result[1]);
    }
}
