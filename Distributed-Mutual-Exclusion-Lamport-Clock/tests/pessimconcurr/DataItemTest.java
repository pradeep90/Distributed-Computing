package pessimconcurr;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import mutexlamport.TimeStamp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DataItemTest {
    DataItem testDataItem;

    @Before
    public void setUp () {
        testDataItem = new DataItem("x");
    }
    
    @After
    public void tearDown () {
    }

    /**
     * Test method for {@link DataItem#DataItem()}.
     */
    @Test
    public final void testDataItem (){
        DataItem d = new DataItem("x");
        assertEquals("x", d.label);
        assertEquals("", d.value);
    }

    /**
     * Test method for {@link DataItem#canRead()}.
     */
    @Test
    public final void testCanReadWTMNull(){
        TimeStamp TS = new TimeStamp(74, 2);
        TimeStamp lowerRTM = new TimeStamp(74, 2);
        TimeStamp higherRTM = new TimeStamp(75, 2);
        TransactionOperation readOperation = new TransactionOperation("1 R x")
                .setTimeStamp(TS);

        // WTM Null
        assertEquals(null, testDataItem.WTM); 
        assertTrue(testDataItem.canRead(readOperation)); 
        assertEquals(TS, testDataItem.RTM); 
    }

    /**
     * Test method for {@link DataItem#canRead()}.
     */
    @Test
    public final void testCanReadRTMNull(){
        TimeStamp TS = new TimeStamp(74, 2);
        TimeStamp lowerTM = new TimeStamp(74, 2);
        TimeStamp higherTM = new TimeStamp(75, 2);
        TransactionOperation readOperation = new TransactionOperation("1 R x")
                .setTimeStamp(TS);

        // RTM Null
        assertEquals(null, testDataItem.RTM);
        assertTrue(testDataItem.canRead(readOperation)); 
        assertEquals(TS, testDataItem.RTM); 
    }

    /**
     * Test method for {@link DataItem#canRead()}.
     */
    @Test
    public final void testCanReadHigherWTM(){
        TimeStamp TS = new TimeStamp(74, 2);
        TimeStamp lowerTM = new TimeStamp(74, 2);
        TimeStamp higherTM = new TimeStamp(75, 2);
        TransactionOperation readOperation = new TransactionOperation("1 R x")
                .setTimeStamp(TS);

        testDataItem.WTM = higherTM;
        assertFalse(testDataItem.canRead(readOperation)); 
        assertEquals(null, testDataItem.RTM); 
    }

    /**
     * Test method for {@link DataItem#canWrite()}.
     */
    @Test
    public final void testCanWrite(){
        TimeStamp TS = new TimeStamp(74, 2);
        TimeStamp lowerTM = new TimeStamp(74, 2);
        TimeStamp higherTM = new TimeStamp(75, 2);
        TransactionOperation writeOperation = new TransactionOperation("2 W y 53")
                .setTimeStamp(TS);

        testDataItem.WTM = null;
        testDataItem.RTM = null;
        assertTrue(testDataItem.canWrite(writeOperation)); 
        assertEquals(TS, testDataItem.WTM); 

        testDataItem.WTM = lowerTM;
        testDataItem.RTM = null;
        assertTrue(testDataItem.canWrite(writeOperation)); 
        assertEquals(TS, testDataItem.WTM); 

        testDataItem.WTM = null;
        testDataItem.RTM = lowerTM;
        assertTrue(testDataItem.canWrite(writeOperation)); 
        assertEquals(TS, testDataItem.WTM); 

        testDataItem.WTM = lowerTM;
        testDataItem.RTM = lowerTM;
        assertTrue(testDataItem.canWrite(writeOperation)); 
        assertEquals(TS, testDataItem.WTM); 

        testDataItem.WTM = higherTM;
        testDataItem.RTM = lowerTM;
        assertFalse(testDataItem.canWrite(writeOperation)); 
        assertEquals(higherTM, testDataItem.WTM); 

        testDataItem.WTM = lowerTM;
        testDataItem.RTM = higherTM;
        assertFalse(testDataItem.canWrite(writeOperation)); 
        assertEquals(lowerTM, testDataItem.WTM); 
    }

    /**
     * Test method for {@link DataItem#handleRejectedTransaction()}.
     */
    @Test
    public final void testHandleRejectedTransaction(){
        TimeStamp TS = new TimeStamp(74, 2);
        TimeStamp lowerRTM = new TimeStamp(74, 2);
        TimeStamp higherRTM = new TimeStamp(75, 2);

        TransactionOperation readOperation1 = new TransactionOperation("2 R x")
                .setTimeStamp(TS);
        TransactionOperation readOperation2 = new TransactionOperation("1 R x")
                .setTimeStamp(TS);
        TransactionOperation readOperation3 = new TransactionOperation("1 R x")
                .setTimeStamp(TS);

        TransactionOperation writeOperation1 = new TransactionOperation("2 W y 53")
                .setTimeStamp(TS);
        TransactionOperation writeOperation2 = new TransactionOperation("2 W y 53")
                .setTimeStamp(TS);
        TransactionOperation writeOperation3 = new TransactionOperation("1 W y 53")
                .setTimeStamp(TS);
        
        testDataItem.readList.add(readOperation1);
        testDataItem.readList.add(readOperation2);
        testDataItem.readList.add(readOperation3);

        testDataItem.writeList.add(writeOperation1);
        testDataItem.writeList.add(writeOperation2);
        testDataItem.writeList.add(writeOperation3);

        assertEquals(3, testDataItem.readList.size()); 
        assertEquals(3, testDataItem.writeList.size()); 

        testDataItem.handleRejectedTransaction(3);
        assertEquals(3, testDataItem.readList.size()); 
        assertEquals(3, testDataItem.writeList.size()); 

        testDataItem.handleRejectedTransaction(1);
        assertEquals(1, testDataItem.readList.size()); 
        assertEquals(2, testDataItem.writeList.size()); 

        testDataItem.handleRejectedTransaction(2);
        assertEquals(0, testDataItem.readList.size()); 
        assertEquals(0, testDataItem.writeList.size()); 
    }

    /**
     * Test method for {@link DataItem#read()}.
     */
    @Test
    public final void testRead(){
        String s1 = "PID 31 TS 2937";
        TransactionOperation readOperation = new TransactionOperation("1 R x")
                .setTimeStamp(TransactionOperation.getTimeStamp(s1));
        List<TransactionOperation> readList = new ArrayList<TransactionOperation>();
        readList.add(readOperation);
        assertEquals("", testDataItem.read(readOperation));
        System.out.println("readList");
        System.out.println(readList); 
        System.out.println("testDataItem.readList");
        System.out.println(testDataItem.readList); 
        assertEquals(readList, testDataItem.readList); 
    }

    /**
     * Test method for {@link DataItem#write()}.
     */
    @Test
    public final void testWrite(){
        TransactionOperation writeOperation = new TransactionOperation("2 W y Yo!");
        String value = "Yo!";
        testDataItem.write(writeOperation);
        assertEquals(value, testDataItem.value);
    }

}
