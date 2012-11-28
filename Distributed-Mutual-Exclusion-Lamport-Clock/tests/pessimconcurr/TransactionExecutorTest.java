package pessimconcurr;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.util.ArrayList;
import java.util.HashMap;

import mutexlamport.LogicalClock;
import mutexlamport.TimeStamp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TransactionExecutorTest {

    TransactionExecutor transactionExecutor;

    @Before
    public void setUp() {
        transactionExecutor = new TransactionExecutor();
        transactionExecutor.clock = new LogicalClock(0);
        transactionExecutor.transactionTimeStampHash = new HashMap<Integer, TimeStamp>();
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test method for {@link TransactionExecutor#getNextTransactionOperation()}.
     */
    @Test
    public final void testGetNextTransactionOperation(){
        transactionExecutor.transactionOperationList = new ArrayList<TransactionOperation> ();
        TransactionOperation readOperation = new TransactionOperation("1 R x");
        TransactionOperation writeOperation = new TransactionOperation("2 W y 53");
        transactionExecutor.transactionOperationList.add(readOperation);
        transactionExecutor.transactionOperationList.add(writeOperation);

        assertEquals(readOperation, transactionExecutor.getNextTransactionOperation());
        assertEquals(1, transactionExecutor.operationIndex); 
        assertEquals(readOperation.transactionTimeStamp,
                     transactionExecutor.clock.getTimeStamp()); 
        assertEquals(readOperation.transactionTimeStamp,
                     transactionExecutor.transactionTimeStampHash.get(
                         readOperation.transactionId)); 
        assertTrue(transactionExecutor.transactionTimeStampHash.containsKey(
            readOperation.transactionId));
        assertEquals(0, transactionExecutor.transactionStartIndex); 
        transactionExecutor.getNextTransactionOperation();
        assertEquals(1, transactionExecutor.transactionStartIndex); 
        assertEquals(2, transactionExecutor.operationIndex); 
    }
}
