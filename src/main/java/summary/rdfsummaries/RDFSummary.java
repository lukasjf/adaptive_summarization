package summary.rdfsummaries;

import encoding.SummaryEncoder;
import evaluation.Benchmarkable;
import graph.BaseGraph;
import graph.Dataset;
import graph.SubgraphIsomorphism;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by lukas on 03.07.18.
 */
public class RDFSummary implements Benchmarkable{

    private BaseGraph graph;
    public BaseGraph summmary;

    public RDFSummary(String graphFile){
        if (Dataset.I == null){
            new Dataset(graphFile);
        }
        this.graph = Dataset.I.getGraph();
        this.summmary = readSummary(graphFile.replace("graph", "rdfsummary"));
    }

    private BaseGraph readSummary(String replace) {
        BaseGraph summary = new BaseGraph();
        try(BufferedReader br = new BufferedReader(new FileReader(replace))){
            String line;
            while ((line = br.readLine()) != null){
                if (line.startsWith("v")){
                    int id = Integer.parseInt(line.split(" ")[1]);
                    summary.addNode(id, "");
                    String[] contained = line.split(" ")[2].split(",");
                    summary.nodeWithId(id).getContainedNodes().addAll(Arrays.stream(contained).map(Integer::parseInt).collect(Collectors.toList()));
                } else if (line.startsWith("e")){
                    int source = Integer.parseInt(line.split(" ")[1]);
                    int target = Integer.parseInt(line.split(" ")[2]);
                    String label = line.split(" ")[3];
                    summary.addEdge(source, target, label);
                }
            }
        } catch (IOException e){

        }
        return summary;
    }

    @Override
    public List<Map<String, String>> query(BaseGraph query) {
        return new SubgraphIsomorphism().query(query,summmary, false);
    }

    @Override
    public List<Map<String, String>> query(BaseGraph query, int timeout) {
        return new SubgraphIsomorphism(timeout).query(query,summmary, false);
    }

    @Override
    public void train(Map<BaseGraph, List<Map<String, String>>> queries) {

    }

    @Override
    public long size() {
        return new SummaryEncoder().encode(summmary);
    }
}
