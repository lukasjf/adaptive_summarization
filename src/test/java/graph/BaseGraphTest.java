package graph;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by lukas on 13.03.18.
 */
public class BaseGraphTest {

    private BaseGraph baseGraph;

    @Before
    public void setup(){
        baseGraph = BaseGraph.parseGraph(BaseGraph.class.getResource("/minigraph").getPath());

    }

    @Test
    public void testAddNode(){
        BaseGraph baseGraph = new BaseGraph();
        baseGraph.addNode(0, "label");
        assertTrue(baseGraph.getNodeMapping().containsKey(0));
        assertEquals("label", baseGraph.getNodeMapping().get(0).getLabel());
        baseGraph.addNode(new BaseNode(1, "label2"));
        assertEquals(2, baseGraph.getNodes().size());
        baseGraph.addNode(new BaseNode(0, "label3"));
        assertEquals(2, baseGraph.getNodes().size());
        assertEquals("label3", baseGraph.getNodeMapping().get(0).getLabel());
    }

    @Test
    public void testParseGraph(){
        assertEquals(5, baseGraph.getNodes().size());
        assertEquals(6, baseGraph.getEdges().size());
    }

    @Test
    public void testQuery(){
        BaseGraph query = new BaseGraph();
        query.addNode(new BaseNode(0, "?"));
        query.addNode(new BaseNode(1, "label2"));
        query.addEdge(0, 1, "edge1");
        List<String[]> result = baseGraph.query(query);

        assertTrue(result.stream().anyMatch(a -> Arrays.equals(a, new String[] {"label1"})));
        assertTrue(result.stream().anyMatch(a -> Arrays.equals(a, new String[] {"label5"})));
    }

    @Test
    public void testGetVariables(){
        BaseGraph query = new BaseGraph();
        query.addNode(0, "?");
        query.addNode(1, "label");
        assertEquals(1 , query.getVariables().size());
        assertEquals("?", query.getVariables().get(0));
    }
}