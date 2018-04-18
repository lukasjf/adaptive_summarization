package graph;

import java.util.List;
import java.util.Map;

/**
 * Created by lukas on 18.04.18.
 */
public class F1Score {

    private static double epsilon = 0.00000000001;

    public static double fqScoreFor(List<Map<String, String>> graphResults, List<Map<String, String>> summaryResults){
        int tp = 0, fp = 0, fn = 0;
        for (Map<String, String> result: summaryResults){
            if (graphResults.stream().anyMatch(graphResult -> result.equals(graphResult))){
                tp++;
            } else{
                fp++;
            }
        }
        for (Map<String, String> result: graphResults){
            if (summaryResults.stream().noneMatch(summaryResult -> result.equals(summaryResult))){
                fn++;
            }
        }
        double precision = tp / 1.0 / (tp + fp + epsilon);
        double recall = tp / 1.0 / (tp + fn + epsilon);
        return 2 * precision * recall / (precision + recall + epsilon);
    }
}
