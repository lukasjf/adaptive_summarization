package graph;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by lukas on 13.03.18.
 */
public class QueryTest {

    @Test
    public void testParseFromString(){

    }

    @Test
    public void testGetVariables(){
        Query query = new Query();
        query.addNode(0, "?");
        query.addNode(1, "label");
        assertEquals(1 , query.getVariables().size());
        assertEquals("?", query.getVariables().get(0));
    }
}