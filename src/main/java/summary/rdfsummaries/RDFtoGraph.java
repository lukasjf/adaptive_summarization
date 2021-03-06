package summary.rdfsummaries;

import graph.BaseEdge;
import graph.BaseGraph;
import graph.BaseNode;
import graph.Dataset;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by lukas on 19.04.18.
 */
public class RDFtoGraph {

    public static BaseGraph buildSummary(String rdfSummaryPath, String mapDBPath, String mappingName,
                                         String dbserver, String dbPort, String dbname, String schema, String username, String password){
        new Dataset("/home/lukas/studium/thesis/code/data/movie/graph");
        BaseGraph rdfsummary = new BaseGraph();

        try (BufferedReader br = new BufferedReader(new FileReader(new File(rdfSummaryPath)))){
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (line.trim().isEmpty()){
                    continue;
                }
                String[] triple = line.split(" ");
                int subject = Integer.parseInt(extractFromURL(triple[0]));
                String property = extractFromURL(triple[1]);
                int object = Integer.parseInt(extractFromURL(triple[2]));

                if (!rdfsummary.getIdMapping().containsKey(subject)){
                    rdfsummary.addNode(subject, "");
                }
                if (!rdfsummary.getIdMapping().containsKey(object)){
                    rdfsummary.addNode(object, "");
                }

                rdfsummary.addEdge(subject, object, property);

                System.out.println(subject + " " + property + " " + object);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<Integer, Integer> rdfToRealID = new HashMap<>();

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try(Connection conn = DriverManager.getConnection(String.format("jdbc:postgresql://%s:%s/%s", dbserver, dbPort, dbname), username, password)) {
            Statement stmt = conn.createStatement();
            ResultSet results = stmt.executeQuery(String.format("SELECT * FROM %s.g_dict", schema));
            for (; results.next();){
                int id = results.getInt("key");
                String label = extractFromURL(results.getString("value"));
                if (label.startsWith("http://example.org/entity/")){
                    rdfToRealID.put(id, Dataset.I.IDFrom(label.split("entity/")[1]));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }


        DB mapdb = DBMaker.newFileDB(new File(mapDBPath)).make();
        HTreeMap<Integer, Integer> supernodeMappings = mapdb.getHashMap(mappingName);
        for (int normalNodeId: supernodeMappings.keySet()){
            int superNodeId = supernodeMappings.get(normalNodeId);
            rdfsummary.nodeWithId(superNodeId).getContainedNodes().add(rdfToRealID.get(normalNodeId));
        }

        try(PrintStream ps = new PrintStream("rdfsummary")){
            for (BaseNode n: rdfsummary.getNodes()){
                ps.println(String.format("v %d %s", n.getId(),
                        n.getContainedNodes().stream().map(Object::toString).collect(Collectors.joining(","))));
            }
            for (BaseEdge e: rdfsummary.getEdges()){
                ps.println(String.format("e %d %d %s", e.getSource().getId(), e.getTarget().getId(), e.getLabel().split("/property/")[1]));
            }
        } catch (IOException e){

        }

        return rdfsummary;
    }

    private static String extractFromURL(String s) {
        int startIndex = s.lastIndexOf("/d") + 2;
        int endIndex = s.length() - 1;
        return s.substring(startIndex, endIndex);
    }

    public static void main(String[] args){
        String rdfSummaryPath = "/home/lukas/studium/thesis/code/rdfsummary/src/main/resources/data/fw(movie).nt";
        String mapDBPath = "/home/lukas/studium/thesis/code/rdfsummary/abc/citation/fw/fw(movie).db";
        String mappingName = "fw(movie)_s_data_node_by_g_data_node";
        String databaseServer = "localhost";
        String databasePort = "5432";
        String dbname = "rdf";
        String schema = "movie";
        String username = "postgres";
        String password = "postgres";

        RDFtoGraph.buildSummary(rdfSummaryPath, mapDBPath, mappingName, databaseServer, databasePort, dbname, schema, username, password);
    }
}
