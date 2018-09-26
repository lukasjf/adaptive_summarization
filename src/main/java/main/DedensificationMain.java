package main;

import graph.Dataset;
import summary.dedensification.DedensifiedSummary;

/**
 * Created by lukas on 25.09.18.
 */
public class DedensificationMain {

    public static void main(String[] args){
        new Dataset(args[0]);
        int maxTau = Integer.parseInt(args[1]);
        int stepSize = Integer.parseInt(args[2]);
        for (int i = 2; i < maxTau; i+=stepSize) {
            new DedensifiedSummary(Dataset.I.getGraph(), i);
        }
    }
}
