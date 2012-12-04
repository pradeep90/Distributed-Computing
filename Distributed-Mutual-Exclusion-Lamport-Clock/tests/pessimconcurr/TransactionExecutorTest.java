package pessimconcurr;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.util.ArrayList;
import java.util.List;
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
        // transactionExecutor.clock = new LogicalClock(0);
        // transactionExecutor.transactionTimeStampHash = new HashMap<Integer, TimeStamp>();
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test method for {@link TransactionExecutor#getTransactions()}.
     */
    @Test
    public final void testGetTransactions(){
        transactionExecutor.transactionOperationList = new ArrayList<TransactionOperation> ();
        TransactionOperation readOperation1 = new TransactionOperation("1 R x");
        TransactionOperation readOperation2 = new TransactionOperation("1 R y");
        TransactionOperation readOperation3 = new TransactionOperation("1 R z");
        TransactionOperation writeOperation1 = new TransactionOperation("2 W x 52");
        TransactionOperation writeOperation2 = new TransactionOperation("2 W y 53");
        TransactionOperation writeOperation3 = new TransactionOperation("3 W z 54");
        transactionExecutor.transactionOperationList.add(readOperation1);
        transactionExecutor.transactionOperationList.add(readOperation2);
        transactionExecutor.transactionOperationList.add(readOperation3);
        transactionExecutor.transactionOperationList.add(writeOperation1);
        transactionExecutor.transactionOperationList.add(writeOperation2);
        transactionExecutor.transactionOperationList.add(writeOperation3);

        
        List<TransactionOperation> tempList = new ArrayList<TransactionOperation>();
        tempList.add(readOperation1);
        tempList.add(readOperation2);
        tempList.add(readOperation3);
        
        Transaction transaction1 = new Transaction(1,
                                                   null,
                                                   tempList,
                                                   null,
                                                   null);

        transactionExecutor.getTransactions();
        assertEquals(3, transactionExecutor.transactionList.size());
        assertEquals(transaction1.transactionId,
                     transactionExecutor.transactionList.get(0).transactionId); 
        assertEquals(transaction1.operationList.size(),
                     transactionExecutor.transactionList.get(0).operationList.size()); 
    }
    
    // /**
    //  * Test method for {@link TransactionExecutor#getNextTransactionOperation()}.
    //  */
    // @Test
    // public final void testGetNextTransactionOperation(){
    //     transactionExecutor.transactionOperationList = new ArrayList<TransactionOperation> ();
    //     TransactionOperation readOperation = new TransactionOperation("1 R x");
    //     TransactionOperation writeOperation = new TransactionOperation("2 W y 53");
    //     transactionExecutor.transactionOperationList.add(readOperation);
    //     transactionExecutor.transactionOperationList.add(writeOperation);

    //     assertEquals(readOperation, transactionExecutor.getNextTransactionOperation());
    //     assertEquals(1, transactionExecutor.operationIndex); 
    //     assertEquals(readOperation.transactionTimeStamp,
    //                  transactionExecutor.clock.getTimeStamp()); 
    //     assertEquals(readOperation.transactionTimeStamp,
    //                  transactionExecutor.transactionTimeStampHash.get(
    //                      readOperation.transactionId)); 
    //     assertTrue(transactionExecutor.transactionTimeStampHash.containsKey(
    //         readOperation.transactionId));
    //     assertEquals(0, transactionExecutor.transactionStartIndex); 
    //     transactionExecutor.getNextTransactionOperation();
    //     assertEquals(1, transactionExecutor.transactionStartIndex); 
    //     assertEquals(2, transactionExecutor.operationIndex); 
    // }

    /**
     * Test method for {@link TransactionExecutor#canExecuteOperation()}.
     */
    @Test
    public final void testCanExecuteOperation(){
        // assertEquals();
    }

}
