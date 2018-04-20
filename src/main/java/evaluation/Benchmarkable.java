package evaluation;

import graph.BaseGraph;
import graph.GraphQueryAble;

import java.util.List;

/**
 * Created by lukas on 19.04.18.
 */
public interface Benchmarkable extends GraphQueryAble {

    public void train(List<BaseGraph> queries);
}
