package pessimconcurr;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import mutexlamport.TimeStamp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AckMutexMessageTest {
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    /**
     * Test method for {@link AckMutexMessage#toString()}.
     */
    @Test
    public final void testToString(){
        TransactionOperation readOperation = new TransactionOperation("1 R x");
        TransactionOperation writeOperation = new TransactionOperation("2 W y 53");

        String expected = "ACK from 3 " + writeOperation + " " + true;
        assertEquals(expected, new AckMutexMessage(writeOperation, true, 3).toString());

        System.out.println ("writeOperation");
        System.out.println (writeOperation); 

        String expectedRead = "ACK from 3 " + readOperation + " " + true + " " + "779";
        assertEquals(expectedRead,
                     new AckMutexMessage(readOperation, true, 3).setVal("779").toString());
    }

    /**
     * Test method for {@link AckMutexMessage#setFromString()}.
     */
    @Test
    public final void testSetFromString(){
        TimeStamp ts = new TimeStamp(18, 2);
        TransactionOperation readOperation = new TransactionOperation("1 R x")
                .setTimeStamp(ts);
        TransactionOperation writeOperation = new TransactionOperation("2 W y 53")
                .setTimeStamp(ts);

        String writeString = "PID 0 TS 51 : [ ACK from 3 "
                + writeOperation + " " + true + " ]";
        String readString = "PID 0 TS 51 : [ ACK from 3 "
                + readOperation + " " + false + " " + "779" + " ]";

        AckMutexMessage writeAck = new AckMutexMessage(writeString);
        assertEquals(writeOperation, writeAck.op);
        assertEquals(3, writeAck.from_pid);
        assertEquals(true, writeAck.is_success); 
        assertEquals(null, writeAck.val); 

        AckMutexMessage readAck = new AckMutexMessage(readString);
        assertEquals(readOperation, readAck.op);
        assertEquals(3, readAck.from_pid);
        assertEquals(false, readAck.is_success); 
        assertEquals("779", readAck.val); 
    }

    /**
     * Test method for {@link AckMutexMessage#isAckMutexMessage()}.
     */
    @Test
    public final void testIsAckMutexMessage(){
        TimeStamp ts = new TimeStamp(18, 2);
        TransactionOperation readOperation = new TransactionOperation("1 R x")
                .setTimeStamp(ts);
        TransactionOperation writeOperation = new TransactionOperation("2 W y 53")
                .setTimeStamp(ts);

        // String writeStringWithoutTS = "ACK from 3 " + writeOperation + " " + true;
        // String readStringWithoutTS = "ACK from 3 " + readOperation + " "
        //         + false + " " + "779";

        String writeString = "PID 0 TS 51 : [ ACK from 3 "
                + writeOperation + " " + true + " ]";
        String readString = "PID 0 TS 51 : [ ACK from 3 "
                + readOperation + " " + false + " " + "779" + " ]";
        assertTrue(AckMutexMessage.isAckMutexMessage(writeString));
        assertTrue(AckMutexMessage.isAckMutexMessage(readString));
        assertFalse(AckMutexMessage.isAckMutexMessage("Yo, boyz!!!"));
    }

}
