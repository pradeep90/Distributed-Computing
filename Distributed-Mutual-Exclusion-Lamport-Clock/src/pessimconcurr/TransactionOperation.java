package pessimconcurr;

import mutexlamport.Operation;
import mutexlamport.TimeStamp;

public class TransactionOperation extends Operation{
    public int transactionId;
    public TimeStamp transactionTimeStamp;
    public String dataItemLabel;
    public static final String OPERATION_TS_DELIMITER = " \\: ";
    public boolean isPreWrite;

    /**
     * operationString should be of the form "TransactionID (R|W) dataItemLabel [parameter]".
     */
    public TransactionOperation(String operationString) {
        String[] tokens = operationString.split (" ");
        transactionId = Integer.parseInt(tokens[0]);
        operationType = OperationType.getOperationType (tokens[1]);
        dataItemLabel = tokens[2];
        if (this.operationType == OperationType.READ && tokens.length == 3){
            this.parameter = null;
        } else {
            this.parameter = tokens[3];
        }
        
        transactionTimeStamp = null;
        isPreWrite = false;
    }

    public static TransactionOperation fromTimeStampedString(
        String timeStampedOperationString){

        String timeStampString = timeStampedOperationString.split(
            TransactionOperation.OPERATION_TS_DELIMITER)[0];
        String operationString = timeStampedOperationString.split(
            TransactionOperation.OPERATION_TS_DELIMITER)[1];

        TransactionOperation result = new TransactionOperation(operationString);
        result.transactionTimeStamp = new TimeStamp(timeStampString);
        return result;
    }

    /**
     * TODO:
     * Doing this cos I'm not able to import TimeStamp in
     * TransactionOperationTest.
     */
    public static TimeStamp getTimeStamp(String timeStampString){
        return new TimeStamp(timeStampString);
    }

    public TransactionOperation setTimeStamp(TimeStamp ts){
        this.transactionTimeStamp = ts;
        return this;
    }

    public String toString (){
        if (operationType == OperationType.READ){
            return transactionTimeStamp + " : "
                    + transactionId + " "
                    + operationType.getValue() + " " + dataItemLabel + " " + parameter;
        }
        else {
            return transactionTimeStamp + " : " + transactionId
                    + " " + operationType.getValue ()
                    + " " + dataItemLabel + " " + parameter;
        }
    }

    public boolean equals(Object obj){
        if (!(obj instanceof TransactionOperation)){
            return false;
        }
        TransactionOperation other = (TransactionOperation) obj;
        return transactionId == other.transactionId
                && dataItemLabel.equals(other.dataItemLabel)
                && transactionTimeStamp.equals(other.transactionTimeStamp);
    }

}
