package main;


import java.io.IOException;
import java.util.Arrays;

/**
 * Created by lukas on 10.07.18.
 */
public class Runner {

    public static String outputFile;
    public static int queryLimit = Integer.MAX_VALUE;

    public static void main(String[] args) throws IOException {
        String method = args[0];
        String output = args[1];
        Runner.queryLimit = Integer.parseInt(args[2]);
        Runner.outputFile = output;

        switch (method){
            case "tcm":
                TCMRunner.main(Arrays.copyOfRange(args, 3, args.length));
                break;
            case "rdf":
                RDFSummaryRunner.main(Arrays.copyOfRange(args, 3, args.length));
                break;
            case "cache":
                BaselineCacheRunner.main(Arrays.copyOfRange(args, 3, args.length));
                break;
            case "merge":
                MergedRunner.main(Arrays.copyOfRange(args, 3, args.length));
                break;
            case "topdown":
                TopDownRunner.main(Arrays.copyOfRange(args, 3, args.length));
                break;
        }
    }
}
