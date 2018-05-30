package graph;

import javax.xml.crypto.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by lukas on 03.05.18.
 */
public class Dataset {

    public static Dataset I;

    private Map<String, Integer> labelToID;
    private Map<Integer, String> idtoLabel = new HashMap<>();
    private BaseGraph graph;

    public Dataset(String filename){
        Dataset.I = this;
        labelToID = new HashMap<>();
        idtoLabel = new HashMap<>();
        this.graph = GraphImporter.parseGraph(filename, true);
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
