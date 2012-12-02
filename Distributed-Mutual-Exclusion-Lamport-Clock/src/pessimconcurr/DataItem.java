package pessimconcurr;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import mutexlamport.TimeStamp;
import mutexlamport.Operation;

/** 
 * Class to represent a distributed data item.
 */
public class DataItem {
    public String label;
    public String value;

    TimeStamp RTM;
    TimeStamp WTM;

    public List<TransactionOperation> readList;
    public List<TransactionOperation> writeList;

    public int nextReadAckIndex;
    public int nextWriteAckIndex;

    public List<TransactionOperation> preOperationBuffer;
    
    public DataItem (String label) {
        this.label = label;
        this.value = "";
        readList = new ArrayList<TransactionOperation>();
        writeList = new ArrayList<TransactionOperation>();
        preOperationBuffer = new ArrayList<TransactionOperation>();
        RTM = null;
        WTM = null;
        nextReadAckIndex = 0;
        nextWriteAckIndex = 0;
    }

    /** 
     * Set RTM to be max (RTM, TS).
     */
    public void setMaxRTM(TimeStamp TS){
        if (RTM == null){
            RTM = TS;
        } else if (TS.compareTo(RTM) == TimeStamp.AFTER){
            RTM = TS;
        }
    }

    /** 
     * Remove all operations for this Transaction from readList and
     * writeList.
     */
    public void handleRejectedTransaction(int transactionId){
        Iterator<TransactionOperation> it = readList.iterator();
        while (it.hasNext()) {
            if(it.next().transactionId == transactionId)
                it.remove();
        }

        Iterator<TransactionOperation> writeIter = writeList.iterator();
        while (writeIter.hasNext()) {
            if(writeIter.next().transactionId == transactionId)
                writeIter.remove();
        }
    }

    /** 
     * @return true iff the read or write op can be executed.
     */
    public boolean canExecuteOperation(TransactionOperation op){
        if (op.operationType == Operation.OperationType.READ){
            return canRead(op);
        } else {
            return canWrite(op);
        }
    }

    /**
     * Note that you should call read() pretty soon after this, else
     * you could end up with a READ when you shouldn't be able to.
     */
    public boolean canRead(TransactionOperation op){
        TimeStamp TS = op.transactionTimeStamp;

        if (TS.compareTo(WTM) == TimeStamp.BEFORE){
            return false;
        } else {
            setMaxRTM(TS);
            return true;
        }
    }

    public boolean canWrite(TransactionOperation op){
        TimeStamp TS = op.transactionTimeStamp;
        if (TS.compareTo(RTM) == TimeStamp.BEFORE
            || TS.compareTo(WTM) == TimeStamp.BEFORE){
            return false;
        } else {
            WTM = TS;
            return true;
        }
    }

    public String read(TransactionOperation op){
        readList.add(op);
        return value;
    }

    public void write(TransactionOperation op){
        writeList.add(op);
        this.value = op.parameter;
    }

    /**
     * Try adding a prewrite to the data item.
     *
     * @return true iff the prewrite was added successfully.
     */
    public void doPreWrite(TransactionOperation op){
        op.isPreWrite = true;
        preOperationBuffer.add(op);
    }

    /** 
     * Add op to the preOperationBuffer (so that it will be read
     * automatically after all prior prewrites are done.
     */
    public void addReadToBuffer(TransactionOperation op){
        preOperationBuffer.add(op);
    }

    /** 
     * Mark all operations for Transaction with TS as non-prewrites
     * (i.e., ready to commit).
     */
    public void markTransactionForCommit(TimeStamp TS){
        for (TransactionOperation op : preOperationBuffer){
            if (op.transactionTimeStamp == TS){
                op.isPreWrite = false;
            }
        }
    }

    /** 
     * Execute as many operations as possible from the front of the
     * preOperationBuffer.
     */
    public void tryExecuteOps(){
        Iterator<TransactionOperation> iter = preOperationBuffer.iterator();
        while (iter.hasNext()) {
            TransactionOperation currOp = iter.next();

            if (currOp.isPreWrite){
                break;
            }

            if (currOp.operationType == Operation.OperationType.READ){
                currOp.parameter = read(currOp);
            } else {
                write(currOp);
                currOp.isPreWrite = false;
            }
            
            iter.remove();
        }
    }

    public String toString(){
        String currValue = value.equals("") ? "None": value;
        return "<DataItem " + label + ": " + currValue + " " + "Reads: " + readList.toString() + " Writes: " + writeList.toString() + ">";
    }
}
