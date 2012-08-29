package mutexlamport;

public class LogicalClock {
    private int timeStamp;
    
    LogicalClock (){
        timeStamp = 0;
    }

    public static void main (String argv[]){
        LogicalClock clock = new LogicalClock ();
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
}
