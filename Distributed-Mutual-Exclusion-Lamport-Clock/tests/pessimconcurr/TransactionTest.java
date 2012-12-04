package pessimconcurr;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.util.List;
import java.util.ArrayList;
import mutexlamport.TimeStamp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TransactionTest {
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    /**
     * Test method for {@link Transaction#toString()}.
     */
    @Test
    public final void testToString(){
        TransactionOperation readOperation1 = new TransactionOperation("1 R x");
        TransactionOperation readOperation2 = new TransactionOperation("1 R y");
        TransactionOperation readOperation3 = new TransactionOperation("1 R z");
        List<TransactionOperation> tempList = new ArrayList<TransactionOperation>();
        tempList.add(readOperation1);
        tempList.add(readOperation2);
        tempList.add(readOperation3);
        RequestHandler requestHandler = new RequestHandler();
        Transaction foo = new Transaction(3, new TimeStamp(7, 2),
                                          tempList, requestHandler, null);
        assertEquals("operationList.toString(): [null : 1 R x null, null : 1 R y null, null : 1 R z null] isWaitingForAck: false isTransactionRejected: false transactionId: 3 TS: PID 2 TS 7 numOutstandingAcks: 0 ", foo.toString());
    }

}
