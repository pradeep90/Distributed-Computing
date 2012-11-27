package pessimconcurr;

import mutexlamport.Operation;
import mutexlamport.TimeStamp;

public class TransactionOperation extends Operation{
    public int transactionId;
    public TimeStamp transactionTimeStamp;
    public String dataItemLabel;
    public static final String OPERATION_TS_DELIMITER = " \\: ";

    /**
     * operationString should be of the form "TransactionID (R|W) dataItemLabel [parameter]".
     */
    public TransactionOperation(String operationString) {
        String[] tokens = operationString.split (" ");
        transactionId = Integer.parseInt(tokens[0]);
        operationType = OperationType.getOperationType (tokens[1]);
        dataItemLabel = tokens[2];
        if (this.operationType == OperationType.WRITE){
            this.parameter = tokens[3];
        }
        transactionTimeStamp = null;
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

    public String toString (){
        if (operationType == OperationType.READ){
            return transactionTimeStamp + " : "
                    + transactionId + " "
                    + operationType.getValue() + " " + dataItemLabel;
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
