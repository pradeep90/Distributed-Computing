package pessimconcurr;

import mutexlamport.Operation;
import mutexlamport.TimeStamp;

public class TransactionOperation extends Operation{
    public int transactionId;
    public TimeStamp transactionTimeStamp;
    public String dataItemLabel;
    public static final String OPERATION_TS_DELIMITER = " \\| ";

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

    public TransactionOperation(String timeStampString, String operationString){
        this(operationString);
        transactionTimeStamp = new TimeStamp(timeStampString);
    }


    public String toString (){
        if (operationType == OperationType.READ){
            return transactionTimeStamp + " | "
                    + transactionId + " "
                    + operationType.getValue() + " " + dataItemLabel;
        }
        else {
            return transactionTimeStamp + " | " + transactionId
                    + " " + operationType.getValue ()
                    + " " + dataItemLabel + " " + parameter;
        }
    }
}
