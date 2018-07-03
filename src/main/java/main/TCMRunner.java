package main;

import evaluation.Benchmark;
import evaluation.Benchmarkable;
import graph.Dataset;
import summary.tcm.TCMSummary;

import java.io.*;
import java.util.Arrays;

/**
 * Created by lukas on 28.06.18.
 */
public class TCMRunner {

    private static String HEADER = "graph,method,queryset,storage,size,hashes,trainingF1,testF1,creationTime,graphTime,summaryTime";
    private static String TEMPLATE = "%s,%s,%s,%d,%d,%d,%f,%f,%f,%f,%f\n";

    public static void main(String[] args) throws IOException {
        long sizeLimit = Long.parseLong(args[0]);
        int numberHashes = Integer.parseInt(args[1]);
        String graphFile = args[2];
        String[] benchmarks = Arrays.copyOfRange(args, 3, args.length);

        new Dataset(graphFile);
        long start = System.currentTimeMillis();
        TCMSummary summary = TCMSummary.createFromGraph(Dataset.I.getGraph(), numberHashes, sizeLimit);
        double trainingTime = (System.currentTimeMillis() - start) / 1000.0;

        File resultFile = new File("tcm.csv");
        if (! resultFile.exists()){
            resultFile.createNewFile();
            new PrintStream(resultFile).println(HEADER);
        }
        FileWriter output = new FileWriter(resultFile, true);

        for (String dir: benchmarks){
            System.out.println(dir);
            if (summary == null){
                output.write(String.format(TEMPLATE, graphFile, "tcm", dir, sizeLimit, -1, numberHashes, -1.0, -1.0, -1.0, -1.0, -1.0));
                continue;
            }

            Benchmark benchmark = new Benchmark(dir);
            Benchmark.Result r = benchmark.run(summary, Dataset.I.getGraph());
            output.write(String.format(TEMPLATE, graphFile, "tcm", dir, sizeLimit, r.size, numberHashes, r.trainingF1,
                    r.testF1, trainingTime, r.graphtime, r.summarytime));
        }
        output.flush();
        output.close();
    }
}
