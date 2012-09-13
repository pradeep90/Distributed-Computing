package mutexlamport;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import static mutexlamport.TimeStamp.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TimeStampTest {
    TimeStamp smallTimeStamp;
    TimeStamp smallTimeStampGreaterPid;
    TimeStamp mediumTimeStamp;
 
    @Before
    public void setUp() {
        smallTimeStamp = new TimeStamp (1, 2);
        smallTimeStampGreaterPid = new TimeStamp (1, 3);
        mediumTimeStamp = new TimeStamp (3, 4);
    }
 
    @After
    public void tearDown() {
    }
    
    @Test
    public void testCompareTo_before () {
        assertEquals (BEFORE,
                      smallTimeStamp.compareTo (mediumTimeStamp));
    }

    /**
     * Test method for {@link TimeStamp#compareTo()}.
     */
    @Test
    public final void testCompareTo_after (){
        assertEquals (AFTER,
                      mediumTimeStamp.compareTo (smallTimeStamp));
        assertEquals (AFTER,
                      smallTimeStampGreaterPid.compareTo (smallTimeStamp));
    }

    /**
     * Test method for {@link TimeStamp#compareTo()}.
     */
    @Test
    public final void testCompareTo_equal (){
        assertEquals (EQUAL,
                      smallTimeStamp.compareTo (smallTimeStamp));
    }

    /**
     * Test method for {@link TimeStamp#equals()}.
     */
    @Test
    public final void testEquals (){
        String s = "";
        assertFalse (smallTimeStamp.equals (s));
        assertTrue (smallTimeStamp.equals (new TimeStamp (smallTimeStamp)));
    }

}
