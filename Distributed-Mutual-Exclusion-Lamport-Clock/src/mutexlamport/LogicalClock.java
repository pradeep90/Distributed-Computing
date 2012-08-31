package mutexlamport;

public class LogicalClock {
    private int timeStamp;
    private int processId;
    
    LogicalClock (int processId){
        this.timeStamp = 0;
        this.processId = processId;
    }

    public static void main (String argv[]){
        int testProcessId = 7;
        LogicalClock clock = new LogicalClock (testProcessId);
        for (int i = 0; i < 10; i++){
            clock.update ();
            System.out.println (clock.getTimeStamp ());
        }
    }
   
    int update (){
        return ++timeStamp;
    }

    int update (int eventTimeStamp){
        if (eventTimeStamp > timeStamp){
            timeStamp = eventTimeStamp;
        }
        return update ();
    }

    int getTimeStamp (){
        return timeStamp;
    }

    int setTimeStamp (){
        return timeStamp;
    }

    /**
     * Return the message formatted with the current TS.
     */
    public String getTimeStampedString (String message){
        // Later have the processId within the Timestamp itself
        return "TS " + timeStamp
                + " PID " + processId
                + " : " + message;
    }
}
