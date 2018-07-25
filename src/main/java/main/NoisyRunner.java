package main;

import evaluation.Benchmarkable;
import evaluation.NoisyBenchmark;
import graph.Dataset;
import summary.caching.SummaryCache;
import summary.merging.Stupid2;
import summary.merging.StupidMerge;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Created by lukas on 25.07.18.
 */
public class NoisyRunner {

    public static int FOLDSIZE = 5;

    private static String HEADER = "graph,method,queryset,numq,k,t,storage,size,trainingF1,testF1,cleanTrF1,cleanTeF1,creationTime,graphTime,summaryTime";
    private static String TEMPLATE = "%s,%s,%s,%d,%d,%f,%d,%d,%f,%f,%f,%f,%f,%f,%f\n";


    public static void main(String[] args) throws IOException {
        String method = args[0];
        String output = args[1];
        Runner.outputFile = output;
        Runner.queryLimit = Integer.parseInt(args[2]);
        long sizeLimit = Long.parseLong(args[3]);
        String graph = args[4];
        new Dataset(graph);
        String queryset = args[5];
        String foucsset = args[6];
        Benchmarkable b = null;

        int k = -1;
        double t = -1;

        File resultFile = new File(Runner.outputFile);
        if (!resultFile.exists()) {
            resultFile.createNewFile();
            new PrintStream(resultFile).println(HEADER);
        }
        FileWriter outputFile = new FileWriter(resultFile, true);

        System.out.println(queryset);
        for (int i = 0; i < FOLDSIZE; i++){
            switch (method){
                case "cache":
                    b = new SummaryCache(sizeLimit);
                    break;
                case "stupid":
                    k = Integer.parseInt(args[7]);
                    t = Double.parseDouble(args[8]);
                    b = new StupidMerge(Dataset.I.getGraph(), "", sizeLimit, k, t);
                    break;
                case "stupid2":
                    k = Integer.parseInt(args[7]);
                    t = Double.parseDouble(args[8]);
                    b = new Stupid2(Dataset.I.getGraph(), sizeLimit, k, t);
                    break;
            }
            NoisyBenchmark.Result r = new NoisyBenchmark(queryset, foucsset).run(b, Dataset.I.getGraph());
            outputFile.write(String.format(TEMPLATE, graph, method, queryset, Runner.queryLimit, k, t, sizeLimit,
                    b.size(), r.trainingF1, r.testF1, r.cleanTrainingF1, r.cleanTestF1, r.trainingtime,
                    r.graphtime, r.summarytime));
            outputFile.flush();
        }
    }
}