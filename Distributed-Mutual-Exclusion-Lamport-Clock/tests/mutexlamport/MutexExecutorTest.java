package mutexlamport;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MutexExecutorTest {
    @Before
    public void setUp () {
    }
    
    @After
    public void tearDown () {
    }
    
    /**
     * Test method for {@link MutexExecutor#foo()}.
     */
    @Test
    public final void testFoo (){
        assertEquals (7, 3 + 4);
    }
}

