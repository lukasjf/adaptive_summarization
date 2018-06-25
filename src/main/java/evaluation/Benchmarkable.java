package evaluation;

import graph.BaseGraph;
import graph.GraphQueryAble;

import java.util.List;
import java.util.Map;

/**
 * Created by lukas on 19.04.18.
 */
public interface Benchmarkable extends GraphQueryAble {

    public void train(Map<BaseGraph, List<Map<String, String>>> queries);

    public long size();
}
