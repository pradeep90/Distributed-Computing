package pessimconcurr;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DataItemTest {
    DataItem testDataItem;

    @Before
    public void setUp () {
        testDataItem = new DataItem("x");
    }
    
    @After
    public void tearDown () {
    }

    /**
     * Test method for {@link DataItem#DataItem()}.
     */
    @Test
    public final void testDataItem (){
        DataItem d = new DataItem("x");
        assertEquals("x", d.label);
        assertEquals("", d.value);
    }

    /**
     * Test method for {@link DataItem#read()}.
     */
    @Test
    public final void testRead(){
        assertEquals("", testDataItem.read());
    }

    /**
     * Test method for {@link DataItem#write()}.
     */
    @Test
    public final void testWrite(){
        String value = "Yo, boyz!";
        testDataItem.write(value);
        assertEquals(value, testDataItem.read());
    }

}
