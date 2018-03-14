package graph;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by lukas on 13.03.18.
 */
public class BaseEdgeTest {

    @Test
    public void testEquals(){
        BaseEdge edge1 = new BaseEdge(new BaseNode(0, "l1"), new BaseNode(1, "l2"), "edgelabel");
        BaseEdge edge2 = new BaseEdge(new BaseNode(2, "l1"), new BaseNode(3, "l2"), "edgelabel");
        assertFalse(edge1.equals(edge2));
        edge2 = new BaseEdge(new BaseNode(0, "otherlabel"), new BaseNode(1, "l2"), "edgelabel");
        assertTrue(edge1.equals(edge2));
    }
}