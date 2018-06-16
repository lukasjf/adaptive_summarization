package graph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by lukas on 16.04.18.
 */
public class GraphImporter {

    public static BaseGraph parseGraph(String filename){
        return parseGraph(filename, false);
    }

    public static BaseGraph parseGraph(String filename, boolean createMaps){
        int deduplicateCounter = 0;
        Set<String> labels = new HashSet<>();

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
                    while (labels.contains(label)){
                        label = label + deduplicateCounter++;
                    }
                    g.addNode(id, label);
                    g.idMapping.get(id).containedNodes.add(id);
                    if (createMaps){
                        Dataset.I.addPair(label, id);
                    }
                    labels.add(label);
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
