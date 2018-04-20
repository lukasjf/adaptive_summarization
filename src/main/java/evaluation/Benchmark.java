package evaluation;

import graph.BaseGraph;
import graph.F1Score;
import graph.GraphImporter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
        b.train(trainingQueries);
        double trainingResult = 0.0, testResult = 0.0;
        for (BaseGraph q: trainingQueries){
            trainingResult += F1Score.fqScoreFor(g.query(q), b.query(q));
            System.out.print(".");
        }
        for (BaseGraph q: testQueries){
            testResult += F1Score.fqScoreFor(g.query(q), b.query(q));
            System.out.print("*");
        }
        return new double[] {trainingResult / trainingQueries.size(), testResult / testQueries.size()};
    }
}
