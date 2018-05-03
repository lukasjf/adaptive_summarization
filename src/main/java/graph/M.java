package graph;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lukas on 03.05.18.
 */
public class M {

    private static Map<String, Integer> labelToID = new HashMap<>();
    private static Map<Integer, String> idtoLabel = new HashMap<>();

    public static int IDFrom(String label){
        return labelToID.get(label);
    }

    public static String labelFrom(int id){
        return idtoLabel.get(id);
    }

    public static void addPair(String label, int id){
        labelToID.put(label, id);
        idtoLabel.put(id, label);
    }

    public static void remove(int id){
        String label = idtoLabel.get(id);
        idtoLabel.remove(id);
        labelToID.remove(label);
    }
}
