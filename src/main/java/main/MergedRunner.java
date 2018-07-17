package main;

import evaluation.Benchmark;
import evaluation.Benchmarkable;
import graph.Dataset;
import summary.merging.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by lukas on 02.07.18.
 */
public class MergedRunner {

    private static String HEADER = "graph,method,queryset,method,storage,size,trainingF1,testF1,creationTime,graphTime,summaryTime";
    private static String TEMPLATE = "%s,%s,%s,%s,%d,%d,%f,%f,%f,%f,%f\n";

    private static int FOLDSIZE = 2;

    public static void main(String[] args) throws IOException {
        String mergeMethod = args[0];
        long sizeLimit = Long.parseLong(args[1]);
        String graphFile = args[2];
        String[] benchmarks;
        int k = 0;
        double t = 0.0;
        switch (mergeMethod){
            case "normal":
                benchmarks = Arrays.copyOfRange(args, 3, args.length);
                break;
            default:
                k = Integer.parseInt(args[3]);
                t = Double.parseDouble(args[4]);
                benchmarks = Arrays.copyOfRange(args, 5, args.length);
        }

        new Dataset(graphFile);

        File resultFile = new File(Runner.outputFile);
        if (!resultFile.exists()) {
            resultFile.createNewFile();
            new PrintStream(resultFile).println(HEADER);
        }
        FileWriter output = new FileWriter(resultFile, true);

        for (String dir: benchmarks){
            System.out.println(dir);
            Benchmark benchmark = new Benchmark(dir);

            for (int i = 0; i < FOLDSIZE; i++){
                Benchmarkable summary;
                switch (mergeMethod){
                    case "reg":
                        summary = new MergedSummary(Dataset.I.getGraph(), "full", sizeLimit, new KHopNeighborWeights(1, 0.15));
                        break;
                    case "neighbor":
                        summary = new MergedSummary(Dataset.I.getGraph(), "full", sizeLimit, new KNeighborWeightsNormalized(1, 0.15));
                        break;
                    case "heat":
                        summary = new MergedSummary(Dataset.I.getGraph(), "random", sizeLimit, new HeatWeights(k,t));
                        break;
                    default:
                        summary = new MergedSummary(Dataset.I.getGraph(), "random", sizeLimit, new PlainWeights());
                        break;
                }
                Benchmark.Result r = benchmark.run(new Benchmarkable[]{summary}, Dataset.I.getGraph()).get(0);
                output.write(String.format(TEMPLATE, graphFile, "merge", dir, mergeMethod, sizeLimit, r.size, r.trainingF1,
                        r.testF1, r.trainingtime, r.graphtime, r.summarytime));
                output.flush();
            }
        }
        output.close();
    }
}
