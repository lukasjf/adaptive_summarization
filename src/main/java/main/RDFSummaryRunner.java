package main;

import evaluation.Benchmark;
import evaluation.Benchmarkable;
import graph.Dataset;
import summary.caching.SummaryCache;
import summary.rdfsummaries.RDFSummary;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

/**
 * Created by lukas on 03.07.18.
 */
public class RDFSummaryRunner {

    private static String HEADER = "graph,method,queryset,storage,size,trainingF1,testF1,creationTime,graphTime,summaryTime";
    private static String TEMPLATE = "%s,%s,%s,%d,%d,%f,%f,%f,%f,%f\n";

    private static int FOLDSIZE = 2;

    public static void main(String[] args) throws IOException {
        long sizeLimit = Long.parseLong(args[0]);
        String graphFile = args[1];
        String[] benchmarks = Arrays.copyOfRange(args, 2, args.length);

        File resultFile = new File("rdfsum.csv");
        if (!resultFile.exists()) {
            resultFile.createNewFile();
            new PrintStream(resultFile).println(HEADER);
        }
        FileWriter output = new FileWriter(resultFile, true);
        long start = System.currentTimeMillis();

        for (String dir: benchmarks){
            System.out.println(dir);
            Benchmark benchmark = new Benchmark(dir);

            Benchmark.Result result = benchmark.run(new RDFSummary(graphFile), Dataset.I.getGraph());
            output.write(String.format(TEMPLATE, graphFile, "rdfsum", dir, sizeLimit, result.size, result.trainingF1,
                        result.testF1, -111.11, result.graphtime, result.summarytime));
            output.flush();
        }
        output.close();
    }
}
