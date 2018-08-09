package evaluation;

import graph.BaseGraph;
import graph.GraphImporter;
import main.Runner;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by lukas on 19.04.18.
 */
public class Benchmark {

    private List<BaseGraph> queries = new ArrayList<>();
    private List<BaseGraph> trainingQueries = new ArrayList<>();
    private List<BaseGraph> testQueries = new ArrayList<>();

    private Random random = new Random();

    public Benchmark(String queryDir) {
        for (File f: new File(queryDir).listFiles()){
            queries.add(GraphImporter.parseGraph(f.getAbsolutePath()));
        }
    }

    public Result run(Benchmarkable b, BaseGraph g){
        Result run = new Result();
        double quality = 0.0;
        long start, graphtime = 0, summarytime = 0;
        for (BaseGraph q: queries){
            start = System.currentTimeMillis();
            List<Map<String, String>> graphResults = g.query(q);
            graphtime += System.currentTimeMillis() - start;
            start = System.currentTimeMillis();
            List<Map<String, String>> summaryResults = b.query(q, 15);
            summarytime += System.currentTimeMillis() - start;
            quality += F1Score.fqScoreFor(graphResults, summaryResults);
            System.out.print(".");
        }
        run.trainingF1 = quality / queries.size();
        run.testF1 = run.trainingF1;
        run.size = b.size();
        run.graphtime = graphtime / 1000.0;
        run.summarytime = summarytime / 1000.0;
        return run;
    }

    public List<Result> run(Benchmarkable[] bs, BaseGraph g){
        int folds = bs.length;
        List<Result> runs = new ArrayList<>();

        for (int i = 0; i < folds; i++){
            long graphtime = 0, summarytime = 0;
            Benchmarkable b = bs[i];
            Result run = new Result();
            // create train/test split
            Collections.shuffle(queries);
            for (int j = 0; j < Runner.queryLimit; j++){
                BaseGraph query = queries.get(random.nextInt(queries.size()));
                while (trainingQueries.contains(query)){
                    query = queries.get(random.nextInt(queries.size()));
                }
                trainingQueries.add(query);
            }
            testQueries = queries.stream().filter(q -> !trainingQueries.contains(q)).collect(Collectors.toList());

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
            for (BaseGraph q: trainingQueries){
                start = System.currentTimeMillis();
                List<Map<String, String>> summaryResults = b.query(q, 15);
                summarytime += System.currentTimeMillis() - start;
                trainingResult += F1Score.fqScoreFor(trainingSet.get(q), summaryResults);
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
                System.out.print("*");
            }
            run.graphtime = graphtime / 1000.0;
            run.summarytime = summarytime / 1000.0;
            run.size = b.size();
            run.trainingF1 = trainingResult / trainingQueries.size();
            run.testF1 = testResult / testQueries.size();
            runs.add(run);
        }
        return runs;
    }

    public class Result{
        public double objective;
        public double trainingF1;
        public double testF1;
        public long size;
        public double graphtime;
        public double summarytime;
        public double trainingtime;
    }
}
