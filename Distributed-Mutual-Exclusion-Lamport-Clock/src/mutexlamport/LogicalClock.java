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

    /**
     * Send a copy of the timestamp.
     *
     * Must send a copy and not the timestamp itself because we do not
     * want it to be modified later when the clock is updated.
     */ 
    TimeStamp getTimeStamp (){
        return new TimeStamp (timeStamp);
    }

    void setTimeStamp (int timeValue){
        this.timeStamp.setTimeValue (timeValue);
    }
}
