package mutexlamport;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MutexMessageTest {
    String requestString = "PID 31 TS 2937 : [ REQUEST ]";
    String releaseString = "PID 44 TS 9730 : [ RELEASE ]";
    String ackString = "PID 44 TS 9730 : [ ACK 93 from 537 ]";

    MutexMessage releaseMessage = new MutexMessage (releaseString);
    MutexMessage requestMessage = new MutexMessage (requestString);
    MutexMessage ackMessage = new MutexMessage (ackString);

    @Before
    public void setUp () {
    }
 
    @After
    public void tearDown () {
    }
    
    /**
     * Test method for {@link MutexMessage#MutexMessage ()}.
     */
    @Test
    public final void testMutexMessage (){
        assertEquals (new TimeStamp (2937, 31),
                      requestMessage.getTimeStamp ());
        assertEquals ("REQUEST",
                      requestMessage.getMessage ());
        
        assertEquals (new TimeStamp (9730, 44),
                      releaseMessage.getTimeStamp ());
        assertEquals ("RELEASE",
                      releaseMessage.getMessage ());
    }

    /**
     * Test method for {@link MutexMessage#isAck()}.
     */
    @Test
    public final void testIsAck (){
        assertTrue ("ackMessage.isAck ()", ackMessage.isAck ());
        assertFalse ("requestMessage.isAck ()", requestMessage.isAck ());
        assertFalse ("releaseMessage.isAck ()", releaseMessage.isAck ());
    }

    /**
     * Test method for {@link MutexMessage#isRelease()}.
     */
    @Test
    public final void testIsRelease (){
        assertTrue ("releaseMessage.isRelease ()", releaseMessage.isRelease ());
        assertFalse ("ackMessage.isRelease ()", ackMessage.isRelease ());
        assertFalse ("requestMessage.isRelease ()", requestMessage.isRelease ());
    }

    /**
     * Test method for {@link MutexMessage#isRequest()}.
     */
    @Test
    public final void testIsRequest (){
        assertTrue ("requestMessage.isRequest ()", requestMessage.isRequest ());
        assertFalse ("releaseMessage.isRequest ()", releaseMessage.isRequest ());
        assertFalse ("ackMessage.isRequest ()", ackMessage.isRequest ());
    }
}
