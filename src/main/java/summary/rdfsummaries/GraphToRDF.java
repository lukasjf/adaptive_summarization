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


        Map<String, String> labelMap = new HashMap<>();
        labelMap.put("p:", "<http://example.org/type/paper>");
        labelMap.put("c:", "<http://example.org/type/conference>");
        labelMap.put("au", "<http://example.org/type/author>");
        labelMap.put("af", "<http://example.org/type/affiliation>");

        try(PrintStream output = new PrintStream(rdfPath)){
            BaseGraph graph = new Dataset(graphPath).getGraph();

            Map<Integer, String> entityMap = new HashMap<>();

            for (BaseNode n: graph.getNodes()){
                entityMap.put(n.getId(), String.format(ENTITYBASE, Dataset.I.labelFrom(n.getId())));

                String prefix = Dataset.I.labelFrom(n.getId()).substring(0,2);
                output.println(entityMap.get(n.getId()) + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>" + " " + labelMap.get(prefix) + " .");

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
        String graphPath = "/home/lukas/studium/thesis/code/data/citation/graph";
        String rdfPath = "/home/lukas/studium/thesis/code/data/citation/typedgraph.rdf";
        GraphToRDF.parseGraphToRDF(graphPath, rdfPath);
    }
}
