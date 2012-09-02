package mutexlamport;

public class LogicalClock {
    private TimeStamp timeStamp;
    
    LogicalClock (int processId){
        timeStamp = new TimeStamp (0, processId);
    }

    public static void main (String argv[]){
        int testProcessId = 7;
        LogicalClock clock = new LogicalClock (testProcessId);
        for (int i = 0; i < 10; i++){
            clock.update ();
            System.out.println (clock.getTimeStamp ().getTimeValue ());
        }
    }
   
    public void update (){
        timeStamp.increment ();
    }

    public void update (int eventTimeStamp){
        if (eventTimeStamp > timeStamp.getTimeValue ()){
            timeStamp.setTimeValue (eventTimeStamp);
        }
        update ();
    }

    TimeStamp getTimeStamp (){
        return timeStamp;
    }

    void setTimeStamp (int timeValue){
        this.timeStamp.setTimeValue (timeValue);
    }

    /**
     * Return the message formatted with the current TS.
     */
    public String getTimeStampedString (String message){
        // Later have the processId within the Timestamp itself
        return timeStamp + " : " + message;
    }

    /**
     * Return processId embedded in message.
     * 
     * TODO(spradeep): This says message should ideally be a Class.
     */
    public static int extractProcessId (String message){
        return 0;
    }
    
    /**
     * Return TimeStamp embedded in message.
     */
    public static int extractTimeStamp (String message){
        return 7;
    }
}
