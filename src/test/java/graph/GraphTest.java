package graph;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Created by lukas on 13.03.18.
 */
public class GraphTest {

    private Graph graph;

    @Before
    public void setup(){
        graph = Graph.parseGraph(Graph.class.getResource("/minigraph").getPath());

    }

    @Test
    public void testAddNode(){
        Graph graph = new Graph();
        graph.addNode(0, "label");
        assertTrue(graph.getNodeMapping().containsKey(0));
        assertEquals("label", graph.getNodeMapping().get(0).getLabel());
        graph.addNode(new Node(1, "label2"));
        assertEquals(2, graph.getNodes().size());
        graph.addNode(new Node(0, "label3"));
        assertEquals(2, graph.getNodes().size());
        assertEquals("label3", graph.getNodeMapping().get(0).getLabel());
    }

    @Test
    public void testParseGraph(){
        assertEquals(5, graph.getNodes().size());
        assertEquals(6, graph.getEdges().size());
    }

    @Test
    public void testQuery(){
        Query query = new Query();
        query.addNode(new Node(0, "?"));
        query.addNode(new Node(1, "label2"));
        query.addEdge(0, 1, "edge1");
        List<List<String>> result = graph.query(query);
        assertTrue(result.contains(Collections.singletonList("label1")));
        assertTrue(result.contains(Collections.singletonList("label5")));
    }
}