package graph;

import java.util.List;
import java.util.Map;

/**
 * Created by lukas on 16.04.18.
 */
public interface GraphQueryAble {

    List<Map<String, String>> query (BaseGraph query);

    List<Map<String, String>> query (BaseGraph query, int timeout);
}
