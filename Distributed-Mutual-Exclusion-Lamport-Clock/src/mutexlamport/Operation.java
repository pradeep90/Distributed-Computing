package mutexlamport;

public class Operation {
    public enum OperationType {
        READ ("R"), WRITE ("W");
        private final String value;

        OperationType (String value){
            this.value = value;
        }

        public static OperationType getOperationType (String value){
            if (value.equals (READ.value)){
                return READ;
            } else {
                return WRITE;
            }
        }

        public String getValue (){
            return this.value;
        }
    }

    public OperationType operationType;
    public String parameter = null;

    public Operation(){}
    
    /**
     * operationString should be of the form "(R|W) parameter".
     */
    public Operation (String operationString) {
        String[] tokens = operationString.split (" ");
        this.operationType = OperationType.getOperationType (tokens[0]);
        if (this.operationType == OperationType.WRITE){
            this.parameter = tokens[1];
        }
    }

    public String toString (){
        return operationType.getValue () + " " + parameter;
    }


    public static void main (String[] args){
        Operation operation = new Operation ("R 34");
        System.out.println (operation.operationType);
        System.out.println (operation.operationType.getValue ());
        System.out.println (operation.parameter);

        Operation operation2 = new Operation ("W 07");
        System.out.println (operation2.operationType);
        System.out.println (operation2.operationType.getValue ());
        System.out.println (operation2.parameter);
    }
}
