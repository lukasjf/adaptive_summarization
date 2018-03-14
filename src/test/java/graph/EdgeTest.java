package graph;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by lukas on 13.03.18.
 */
public class EdgeTest {

    @Test
    public void testEquals(){
        Edge edge1 = new Edge(new Node(0, "l1"), new Node(1, "l2"), "edgelabel");
        Edge edge2 = new Edge(new Node(2, "l1"), new Node(3, "l2"), "edgelabel");
        assertFalse(edge1.equals(edge2));
        edge2 = new Edge(new Node(0, "otherlabel"), new Node(1, "l2"), "edgelabel");
        assertTrue(edge1.equals(edge2));
    }
}