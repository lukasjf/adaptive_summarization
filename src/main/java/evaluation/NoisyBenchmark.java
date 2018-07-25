package evaluation;

import graph.BaseGraph;
import graph.Dataset;
import graph.GraphImporter;
import main.Runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by lukas on 19.04.18.
 */
public class NoisyBenchmark {

    private List<BaseGraph> queries = new ArrayList<>();
    private List<BaseGraph> trainingQueries = new ArrayList<>();
    private List<BaseGraph> testQueries = new ArrayList<>();

    private Set<String> focusLabels;

    public NoisyBenchmark(String queryDir, String focusFile) {
        for (File f: new File(queryDir).listFiles()){
            String path = f.getAbsolutePath();
            String[] pathParts = path.split("/");
            if (Integer.parseInt(pathParts[pathParts.length-1]) <= Runner.queryLimit) {
                queries.add(GraphImporter.parseGraph(f.getAbsolutePath()));
            }
        }
        try (BufferedReader br = new BufferedReader(new FileReader(new File(focusFile)))){
            String focus = br.readLine();
            focusLabels = Arrays.stream(focus.split(",")).map(Integer::parseInt)
                    .map(i-> Dataset.I.labelFrom(i)).collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Result run(Benchmarkable b, BaseGraph g){
        long graphtime = 0, summarytime = 0;
        Result run = new Result();
        // create train/test split
        Collections.shuffle(queries);
        int splitIndex = (int) (0.7 * queries.size());
        trainingQueries = queries.subList(0, splitIndex);
        testQueries = queries.subList(splitIndex + 1, queries.size()-1);

        long start;
        Map<BaseGraph, List<Map<String, String>>> trainingSet = new HashMap<>();
        for (BaseGraph query: trainingQueries){
            start = System.currentTimeMillis();
            List<Map<String, String>> results = g.query(query);
            graphtime += System.currentTimeMillis() - start;
            trainingSet.put(query, results);
        }
        start = System.currentTimeMillis();
        b.train(trainingSet);
        run.trainingtime = System.currentTimeMillis() - start;
        run.trainingtime = run.trainingtime / 1000.0;

        double trainingResult = 0.0, testResult = 0.0;
        double focusTraining = 0.0, focusTest = 0.0;
        for (BaseGraph q: trainingQueries){
            start = System.currentTimeMillis();
            List<Map<String, String>> graphResults = trainingSet.get(q);
            List<Map<String, String>> summaryResults = b.query(q, 15);
            summarytime += System.currentTimeMillis() - start;
            trainingResult += F1Score.fqScoreFor(trainingSet.get(q), summaryResults);

            List<Map<String, String>> focused = graphResults.stream().filter(r ->
                    r.values().stream().allMatch(focusLabels::contains)).collect(Collectors.toList());
            List<Map<String, String>> focusedSummary = summaryResults.stream().filter(r ->
                    r.values().stream().allMatch(focusLabels::contains)).collect(Collectors.toList());
            focusTraining += F1Score.fqScoreFor(focused, focusedSummary);
            System.out.print(".");
        }

        for (BaseGraph q: testQueries){
            start = System.currentTimeMillis();
            List<Map<String, String>> graphResults = g.query(q);
            graphtime += System.currentTimeMillis() - start;
            start = System.currentTimeMillis();
            List<Map<String, String>> summaryResults = b.query(q, 15);
            summarytime += System.currentTimeMillis() - start;
            testResult += F1Score.fqScoreFor(graphResults, summaryResults);

            List<Map<String, String>> focused = graphResults.stream().filter(r ->
                    r.values().stream().allMatch(focusLabels::contains)).collect(Collectors.toList());
            List<Map<String, String>> focusedSummary = summaryResults.stream().filter(r ->
                    r.values().stream().allMatch(focusLabels::contains)).collect(Collectors.toList());
            focusTest += F1Score.fqScoreFor(focused, focusedSummary);
            System.out.print("*");
        }
        run.graphtime = graphtime / 1000.0;
        run.summarytime = summarytime / 1000.0;
        run.size = b.size();
        run.trainingF1 = trainingResult / trainingQueries.size();
        run.testF1 = testResult / testQueries.size();
        run.cleanTrainingF1 = focusTraining / trainingQueries.size();
        run.cleanTestF1 = focusTest / testQueries.size();
        return run;
    }

    public class Result{
        public double objective;
        public double trainingF1;
        public double testF1;
        public double cleanTrainingF1;
        public double cleanTestF1;
        public long size;
        public double graphtime;
        public double summarytime;
        public double trainingtime;
    }
}
