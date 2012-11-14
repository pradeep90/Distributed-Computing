package pessimconcurr;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import mutexlamport.Operation.OperationType;

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
    }
    
    @After
    public void tearDown () {
    }

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
     * Test method for {@link Operation#toString()}.
     */
    @Test
    public final void testToString(){
        assertEquals("1 R x", readOperation.toString());
        assertEquals("2 W y 53", writeOperation.toString());
    }

}

