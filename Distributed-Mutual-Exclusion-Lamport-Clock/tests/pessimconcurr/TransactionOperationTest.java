package pessimconcurr;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import mutexlamport.TimeStamp;
import mutexlamport.Operation.OperationType;
import mutexlamport.Operation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TransactionOperationTest {
    
    TransactionOperation readOperation;
    TransactionOperation writeOperation;
    
    @Before
    public void setUp () {
        readOperation = new TransactionOperation("1 R x");
        writeOperation = new TransactionOperation("2 W y 53");
        // LogicalClock l = new LogicalClock();
        Operation o = new Operation();
        TimeStamp t = new TimeStamp();
    }
    
    @After
    public void tearDown () {}

    /**
     * Test method for {@link Operation#Operation()}.
     */
    @Test
    public final void testOperation (){
        assertEquals(OperationType.READ,
                     readOperation.operationType);
        assertEquals("x", readOperation.dataItemLabel); 

        assertEquals(OperationType.WRITE,
                     writeOperation.operationType);
        assertEquals("y", writeOperation.dataItemLabel); 
        assertEquals("53", writeOperation.parameter);
    }

    /**
     * Test method for {@link TransactionOperation#fromTimeStampedString()}.
     */
    @Test
    public final void testFromTimeStampedString(){
        String s1 = "PID 31 TS 2937";
        String s2 = "2 W y 53";
        TransactionOperation writeOperation = new TransactionOperation(s2);
        writeOperation.transactionTimeStamp = TransactionOperation.getTimeStamp(s1);
        assertEquals(writeOperation, TransactionOperation.fromTimeStampedString(
            s1 + " : " + s2));
    }

    /**
     * Test method for {@link Operation#toString()}.
     */
    @Test
    public final void testToString(){
        // Initially, Timestamp will be null
        assertEquals("null : 1 R x", readOperation.toString());
        assertEquals("null : 2 W y 53", writeOperation.toString());
    }

}

