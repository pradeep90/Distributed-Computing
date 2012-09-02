package mutexlamport;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MutexMessage {
    private TimeStamp timeStamp;
    private String message;
    private static final Pattern REQUEST_PATTERN =
            Pattern.compile("^TS (\\d+) PID (\\d+) : (.*)$");
    private static final String ACK_REGEX = "^ACK (\\d+)$";
    
    public MutexMessage (TimeStamp timeStamp, String message){
        this.timeStamp = timeStamp;
        this.message = message;
    }
    
    public MutexMessage (String timeStampedMessage){
        Matcher m = REQUEST_PATTERN.matcher(timeStampedMessage);

        System.out.println (timeStampedMessage);

        if (m.find()) {
            this.timeStamp = new TimeStamp (m.group (1), m.group (2));
            this.message = m.group (3);
        } 
        // TODO(spradeep): Else, cup or throw Exception
    }
   
    public TimeStamp getTimeStamp (){
        return timeStamp;
    }

    public String getMessage (){
        return message;
    }
    
    /**
     * Return String equivalent of MutexMessage.
     */
    public String toString (){
        return timeStamp + " : " + message;
    }

    public static boolean isAck (String message){
        return message.matches (ACK_REGEX);
    }
}
    
