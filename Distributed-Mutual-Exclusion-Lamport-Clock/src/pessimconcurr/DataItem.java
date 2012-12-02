package pessimconcurr;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import mutexlamport.TimeStamp;

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
    
    public DataItem (String label) {
        this.label = label;
        this.value = "";
        readList = new ArrayList<TransactionOperation>();
        writeList = new ArrayList<TransactionOperation>();
        RTM = null;
        WTM = null;
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

    /**
     * Try adding a prewrite to the data item.
     *
     * @return true iff the prewrite was added successfully.
     */
    public boolean doPreWrite(TransactionOperation op){
        TimeStamp TS = op.transactionTimeStamp;
        if (TS.compareTo(RTM) == TimeStamp.BEFORE
            || TS.compareTo(WTM) == TimeStamp.BEFORE){
            return false;
        } else {
            // TODO(spradeep): Buffer the prewrite
            return true;
        }
    }


    public void write(TransactionOperation op){
        writeList.add(op);
        this.value = op.parameter;
    }

    public String toString(){
        String currValue = value.equals("") ? "None": value;
        return "<DataItem " + label + ": " + currValue + " " + "Reads: " + readList.toString() + " Writes: " + writeList.toString() + ">";
    }

}
