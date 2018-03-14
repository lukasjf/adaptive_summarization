package graph;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by lukas on 13.03.18.
 */
public class NodeTest {

    @Test
    public void testToString(){
        String atimes25 = "aaaaaaaaaaaaaaaaaaaaaaaaa";
        Node node = new Node(0, atimes25);
        assertEquals(atimes25, node.toString());
        node = new Node(0, atimes25+"a");
        assertEquals(atimes25+"...", node.toString());
    }

    @Test
    public void testIdVariable(){
        Node node = new Node(0, "");
        assertEquals(false, node.isVariable());
        node = new Node(0, "?");
        assertEquals(true, node.isVariable());
        node = new Node(0, "?alsoVariable");
        assertEquals(true, node.isVariable());
    }

    @Test
    public void testEquals(){
        Node node1 = new Node(0, "label");
        Node node2 = new Node(0, "label2");
        assertTrue(node1.equals(node2));
        node2 = new Node(1, "label");
        assertFalse(node1.equals(node2));
    }

    @Test
    public void testMatch(){
        Node node = new Node(0, "label");
        Node queryNode = new Node(1, "label");
        assertTrue(node.match(queryNode));
        queryNode = new Node(0, "label2");
        assertFalse(node.match(queryNode));

        queryNode = new Node(0, "?");
        assertTrue(node.match(queryNode));
        node = new Node(0, "");
        assertFalse(node.match(queryNode));
    }
}