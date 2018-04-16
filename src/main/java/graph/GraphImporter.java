package graph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by lukas on 16.04.18.
 */
public class GraphImporter {

    public static BaseGraph parseGraph(String filename){
        BaseGraph g = new BaseGraph();
        try (BufferedReader br = new BufferedReader(new FileReader(new File(filename)))){
            String line;
            while ((line = br.readLine()) != null){
                if (line.trim().isEmpty()){
                    continue;
                }
                if (line.startsWith("#")){
                    continue;
                }
                String[] token = line.split(" ");
                if (token[0].equals("v")){
                    int id = Integer.parseInt(token[1]);
                    String label = token[2];
                    g.addNode(id, label);
                    g.idMapping.get(id).containedNodes.add(id);
                }
                if (token[0].equals("e")){
                    int source = Integer.parseInt(token[1]);
                    int target = Integer.parseInt(token[2]);
                    String label = token[3];
                    g.addEdge(source, target, label);
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return g;
    }
}
