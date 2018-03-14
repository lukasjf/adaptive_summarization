package graph;

import org.junit.Before;
import org.junit.Test;

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
        Query query = new Query();
        query.addNode(new BaseNode(0, "?"));
        query.addNode(new BaseNode(1, "label2"));
        query.addEdge(0, 1, "edge1");
        List<List<String>> result = baseGraph.query(query);
        assertTrue(result.contains(Collections.singletonList("label1")));
        assertTrue(result.contains(Collections.singletonList("label5")));
    }
}