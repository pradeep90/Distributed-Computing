package mutexlamport;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MutexMessage {
    private TimeStamp timeStamp;
    private String message;
    private static final Pattern MESSAGE_PATTERN =
            Pattern.compile("^PID (\\d+) TS (\\d+) : \\[ (.*) \\]$");
    private static final String ACK_REGEX = "^(ACK (\\d+) from (\\d+))$";
    private static final String REQUEST_REGEX =
            "^(REQUEST)$";
    private static final String RELEASE_REGEX =
            "^(RELEASE)$";
    private static final String INIT_REQUEST_REGEX =
            "^(INIT) .*$";
    private static final String INIT_RESPONSE_REGEX =
            "^(GO_AHEAD_INIT)$";
    
    public MutexMessage (TimeStamp timeStamp, String message){
        this.timeStamp = timeStamp;
        this.message = message;
    }
    
    /**
     * The regex parsing is not needed for Ack because for Acks you
     * simply check message.isAck and don't parse its contents.
     */
    public MutexMessage (String timeStampedMessage){
        Matcher m = MESSAGE_PATTERN.matcher(timeStampedMessage);

        if (m.find()) {
            this.timeStamp = new TimeStamp (m.group (2), m.group (1));
            this.message = m.group (3);
        } else {
            System.out.println ("Invalid MutexMessage string");
            System.exit (1);
        }
        
        // TODO(spradeep): Else, cup or throw Exception
        // (Check for Ack though)
    }
   
    public TimeStamp getTimeStamp (){
        return new TimeStamp (timeStamp);
    }

    public String getMessage (){
        return message;
    }
    
    /**
     * @return String equivalent of MutexMessage.
     */
    public String toString (){
        return timeStamp + " : " + message;
    }

    public boolean isAck (){
        return this.message.matches (ACK_REGEX);
    }

    public boolean isRelease (){
        return this.message.matches (RELEASE_REGEX);
    }

    public boolean isRequest (){
        return this.message.matches (REQUEST_REGEX);
    }

    public boolean isInitRequest(){
        return message.matches(INIT_REQUEST_REGEX);
    }
    
    public boolean isInitResponse(){
        return message.matches(INIT_RESPONSE_REGEX);
    }
}
    
