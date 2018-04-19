package summary.rdfsummaries;

import graph.BaseEdge;
import graph.BaseGraph;
import graph.BaseNode;
import graph.GraphImporter;

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
            BaseGraph graph = GraphImporter.parseGraph(graphPath);

            Map<Integer, String> entityMap = new HashMap<>();

            for (BaseNode n: graph.getNodes()){
                entityMap.put(n.getId(), String.format(ENTITYBASE, graph.invertedIndex.get(n.getId())));
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
        String graphPath = "/home/lukas/studium/thesis/code/data/citation/graph_3";
        String rdfPath = "/home/lukas/studium/thesis/code/data/citation/graph_3.rdf";
        GraphToRDF.parseGraphToRDF(graphPath, rdfPath);
    }
}
