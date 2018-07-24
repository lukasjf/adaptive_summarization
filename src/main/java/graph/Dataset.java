package graph;

import javax.xml.crypto.Data;
import java.util.*;

/**
 * Created by lukas on 03.05.18.
 */
public class Dataset {

    public static Dataset I;
    public List<Integer> blacklist = new ArrayList<>();

    public Map<String, Integer> labelToID;
    public Map<Integer, String> idtoLabel = new HashMap<>();
    private BaseGraph graph;

    public Dataset(String filename){
        Dataset.I = this;
        labelToID = new HashMap<>();
        idtoLabel = new HashMap<>();
        graph = GraphImporter.parseGraph(filename, true);

        PriorityQueue<BaseNode> pq = new PriorityQueue<>((n1, n2) -> {
            int degree1 = graph.outEdgesFor(n1.getId()).size() + graph.inEdgesFor(n1.getId()).size();
            int degree2 = graph.outEdgesFor(n2.getId()).size() + graph.inEdgesFor(n2.getId()).size();
            return Integer.compare(-degree1, -degree2);
        });
        pq.addAll(graph.getNodes());
        for (int i = 0; i < 0.01 * graph.getNodes().size(); i++){
            blacklist.add(pq.poll().getId());
        }
    }

    public BaseGraph getGraph(){
        return graph;
    }

    public int IDFrom(String label){
        if (label.matches("-\\d+")){
            return Integer.parseInt(label);
        } else{
            return labelToID.get(label);
        }
    }

    public String labelFrom(int id){
        if (id < 0){
            return "" + id;
        } else{
            return idtoLabel.get(id);
        }
    }

    public void addPair(String label, int id){
        labelToID.put(label, id);
        idtoLabel.put(id, label);
    }

    public void remove(int id){
        String label = idtoLabel.get(id);
        idtoLabel.remove(id);
        labelToID.remove(label);
    }
}
