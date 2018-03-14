package graph;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by lukas on 13.03.18.
 */
public class BaseNodeTest {

    @Test
    public void testToString(){
        String atimes25 = "aaaaaaaaaaaaaaaaaaaaaaaaa";
        BaseNode node = new BaseNode(0, atimes25);
        assertEquals(atimes25, node.toString());
        node = new BaseNode(0, atimes25+"a");
        assertEquals(atimes25+"...", node.toString());
    }

    @Test
    public void testIdVariable(){
        BaseNode node = new BaseNode(0, "");
        assertEquals(false, node.isVariable());
        node = new BaseNode(0, "?");
        assertEquals(true, node.isVariable());
        node = new BaseNode(0, "?alsoVariable");
        assertEquals(true, node.isVariable());
    }

    @Test
    public void testEquals(){
        BaseNode node1 = new BaseNode(0, "label");
        BaseNode node2 = new BaseNode(0, "label2");
        assertTrue(node1.equals(node2));
        node2 = new BaseNode(1, "label");
        assertFalse(node1.equals(node2));
    }

    @Test
    public void testMatch(){
        BaseNode node = new BaseNode(0, "label");
        BaseNode queryNode = new BaseNode(1, "label");
        assertTrue(node.match(queryNode));
        queryNode = new BaseNode(0, "label2");
        assertFalse(node.match(queryNode));

        queryNode = new BaseNode(0, "?");
        assertTrue(node.match(queryNode));
        node = new BaseNode(0, "");
        assertFalse(node.match(queryNode));
    }
}