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
    String initRequestString = "PID 0 TS 51 : [ INIT localhost:36330 ]";
    String initResponseString = "PID 0 TS 51 : [ GO_AHEAD_INIT ]";

    MutexMessage releaseMessage = new MutexMessage (releaseString);
    MutexMessage requestMessage = new MutexMessage (requestString);
    MutexMessage ackMessage = new MutexMessage (ackString);
    MutexMessage initRequestMessage = new MutexMessage(initRequestString);
    MutexMessage initResponseMessage = new MutexMessage(initResponseString);

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

    /**
     * Test method for {@link MutexMessage#isInitRequest()}.
     */
    @Test
    public final void testIsInitRequest (){
        assertFalse ("requestMessage.isInitRequest ()", requestMessage.isInitRequest ());
        assertFalse ("releaseMessage.isInitRequest ()", releaseMessage.isInitRequest ());
        assertFalse ("ackMessage.isInitRequest ()", ackMessage.isInitRequest ());
        assertTrue("initRequestMessage.isInitRequest ()", initRequestMessage.isInitRequest());
    }

    /**
     * Test method for {@link MutexMessage#isInitResponse()}.
     */
    @Test
    public final void testIsInitResponse (){
        assertFalse("requestMessage.isInitResponse ()", requestMessage.isInitResponse ());
        assertFalse("releaseMessage.isInitResponse ()", releaseMessage.isInitResponse ());
        assertFalse("ackMessage.isInitResponse ()", ackMessage.isInitResponse ());
        assertFalse("initRequestMessage.isInitResponse ()",
                    initRequestMessage.isInitResponse());
        assertTrue("initResponseMessage.isInitResponse ()",
                   initResponseMessage.isInitResponse());
    }
}
