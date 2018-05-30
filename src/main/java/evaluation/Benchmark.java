package evaluation;

import graph.BaseGraph;
import graph.GraphImporter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lukas on 19.04.18.
 */
public class Benchmark {

    private List<BaseGraph> trainingQueries = new ArrayList<>();
    private List<BaseGraph> testQueries = new ArrayList<>();

    public Benchmark(String queryDir) {
        for (File f : new File(queryDir + "train/").listFiles()) {
            trainingQueries.add(GraphImporter.parseGraph(f.getAbsolutePath()));
        }
        for (File f : new File(queryDir + "test/").listFiles()) {
            testQueries.add(GraphImporter.parseGraph(f.getAbsolutePath()));
        }
    }

    public double[] run(Benchmarkable b, BaseGraph g){
        Map<BaseGraph, List<Map<String, String>>> trainingSet = new HashMap<>();
        for (BaseGraph query: trainingQueries){
            trainingSet.put(query, g.query(query));
        }
        b.train(trainingSet);
        double trainingResult = 0.0, testResult = 0.0;
        for (BaseGraph q: trainingQueries){
            List<Map<String, String>> graphResults = g.query(q);
            List<Map<String, String>> summaryResults = b.query(q);
            trainingResult += F1Score.fqScoreFor(graphResults, summaryResults);
            System.out.print(".");
        }
        for (BaseGraph q: testQueries){
            List<Map<String, String>> graphResults = g.query(q);
            List<Map<String, String>> summaryResults = b.query(q);
            testResult += F1Score.fqScoreFor(graphResults, summaryResults);
            System.out.print("*");
        }
        return new double[] {trainingResult / trainingQueries.size(), testResult / testQueries.size()};
    }
}
