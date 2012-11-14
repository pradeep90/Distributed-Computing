package pessimconcurr;

import mutexlamport.Operation;

public class TransactionOperation extends Operation{
    public int transactionId;
    public String dataItemLabel;

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
    }

    public String toString (){
        if (operationType == OperationType.READ){
            return transactionId + " " + operationType.getValue() + " " + dataItemLabel;
        }
        else {
            return transactionId + " " + operationType.getValue ()
                    + " " + dataItemLabel + " " + parameter;
        }
    }
}
