package mutexlamport;

import mutexlamport.Operation.OperationType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OperationTest {
    Operation readOperation;
    Operation writeOperation;
    
    @Before
    public void setUp () {
        readOperation = new Operation ("R");
        writeOperation = new Operation ("W 53");
    }
    
    @After
    public void tearDown () {
    }

    /**
     * Test method for {@link Operation#Operation()}.
     */
    @Test
    public final void testOperation (){
        assertEquals (OperationType.READ,
                      readOperation.operationType);
        assertEquals (OperationType.WRITE,
                      writeOperation.operationType);
        assertEquals ("53",
                      writeOperation.parameter);
    }

}
