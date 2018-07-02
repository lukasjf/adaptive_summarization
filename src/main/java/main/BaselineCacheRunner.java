package main;

import evaluation.Benchmark;
import evaluation.Benchmarkable;
import graph.Dataset;
import summary.caching.SummaryCache;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

/**
 * Created by lukas on 02.07.18.
 */
public class BaselineCacheRunner {

    private static String HEADER = "method,queryset,storage,size,trainingF1,testF1,creationTime,graphTime,summaryTime";
    private static String TEMPLATE = "%s,%s,%d,%d,%f,%f,%f,%f,%f,%f\n";

    private static int FOLDSIZE = 10;

    public static void main(String[] args) throws IOException {
        long sizeLimit = Long.parseLong(args[0]);
        String graphFile = args[1];
        String[] benchmarks = Arrays.copyOfRange(args, 2, args.length);

        new Dataset(graphFile);

        File resultFile = new File("cache.csv");
        if (!resultFile.exists()) {
            resultFile.createNewFile();
            new PrintStream(resultFile).println(HEADER);
        }
        FileWriter output = new FileWriter(resultFile, true);
        long start = System.currentTimeMillis();

        for (String dir: benchmarks){
            System.out.println(dir);
            Benchmark benchmark = new Benchmark(dir);

            Benchmarkable[] summaries = new Benchmarkable[FOLDSIZE];
            for (int i = 0; i < FOLDSIZE; i++){
                summaries[i] = new SummaryCache(sizeLimit);
            }
            List<Benchmark.Result> results = benchmark.run(summaries, Dataset.I.getGraph());
            for (Benchmark.Result r: results){
                output.write(String.format(TEMPLATE, "cache", dir, sizeLimit, r.size, r.trainingF1,
                        r.testF1, r.trainingtime, r.graphtime, r.summarytime));
            }
        }
        output.flush();
        output.close();
    }
}
