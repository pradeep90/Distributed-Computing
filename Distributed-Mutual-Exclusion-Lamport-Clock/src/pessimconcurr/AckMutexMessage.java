package pessimconcurr;

import mutexlamport.Operation;
import mutexlamport.MutexMessage;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/** 
 * Class to represent Acks when accessing a data item.
 */
public class AckMutexMessage extends MutexMessage {
    TransactionOperation op;
    boolean is_success;
    int from_pid;
    String val;

    static final Pattern ACK_MESSAGE_PATTERN =
            Pattern.compile("^ACK from (\\d+) (.*) (true|false)(?: (.*))?$");

    public AckMutexMessage(String timeStampedMessage){
        super(timeStampedMessage);
        setFromString(this.message);
    }
    
    public AckMutexMessage(TransactionOperation op, boolean is_success, int from_pid) {
        this.op = op;
        this.is_success = is_success;
        this.from_pid = from_pid;

        val = null;
    }
    
    public AckMutexMessage setVal(String val){
        this.val = val;
        return this;
    }

    /** 
     * Set fields of AckMutexMessage using message.
     */
    public void setFromString(String message){
        Matcher m = ACK_MESSAGE_PATTERN.matcher(message);

        if (m.find()) {
            from_pid = Integer.parseInt(m.group(1));
            op = TransactionOperation.fromTimeStampedString(m.group(2));
            is_success = Boolean.valueOf(m.group(3));

            if (m.start(4) != -1){
                // i.e., if val is missing
                val = m.group(4);
            }
        } else {
            System.out.println ("Invalid AckMutexMessage string");
            System.exit (1);
        }
    }

    public String toString(){
        String result = "ACK from " + from_pid + " " + op + " " + is_success;
        if (op.operationType == Operation.OperationType.READ){
            result += " " + val;
        }
        return result;
    }

    /** 
     * @return true iff message is an AckMutexMessage.
     */
    public static boolean isAckMutexMessage(String message){
        Matcher mutex_matcher = MESSAGE_PATTERN.matcher(message);

        if (!mutex_matcher.matches()){
            return false;
        }

        MutexMessage mutexMessage = new MutexMessage(message);
        Matcher m = ACK_MESSAGE_PATTERN.matcher(mutexMessage.getMessage());
        return m.matches();
    }
}
