package summary.rdfsummaries;

import graph.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by lukas on 19.04.18.
 */
public class GraphToRDF {

    public static String ENTITYBASE = "<http://example.org/entity/%s>";
    public static String PROPERTYBASE = "<http://example.org/property/%s>";

    public static void parseGraphToRDF(String graphPath, String rdfPath) {
        try(PrintStream output = new PrintStream(rdfPath)){
            BaseGraph graph = new Dataset(graphPath).getGraph();

            Map<Integer, String> entityMap = new HashMap<>();

            for (BaseNode n: graph.getNodes()){
                entityMap.put(n.getId(), String.format(ENTITYBASE, Dataset.I.labelFrom(n.getId())));
            }
            for (BaseEdge e: graph.getEdges()){
                String property = String.format(PROPERTYBASE, e.getLabel());
                output.println(entityMap.get(e.getSource().getId()) + " " + property + " " + entityMap.get(e.getTarget().getId()) + " .");
            }
            output.flush();
        } catch (IOException e){
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws FileNotFoundException {
        String graphPath = "/home/lukas/studium/thesis/code/data/movie/graph";
        String rdfPath = "/home/lukas/studium/thesis/code/data/movie/graph.rdf";
        GraphToRDF.parseGraphToRDF(graphPath, rdfPath);
    }
}
