package mutexlamport;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LogicalClockTest {
    @Test
    public void testUpdate () {
        LogicalClock clock = new LogicalClock (7);
        clock.update ();
        assertEquals (7, clock.getTimeStamp ().getProcessId ());
    }
}
