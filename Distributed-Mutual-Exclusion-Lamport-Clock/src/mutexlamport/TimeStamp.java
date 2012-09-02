package mutexlamport;

public class TimeStamp {
    private int timeValue;
    private int processId;

    public TimeStamp (int timeValue, int processId){
        this.timeValue = timeValue;
        this.processId = processId;
    }

    public TimeStamp (String timeValue, String processId){
        this.timeValue = Integer.parseInt (timeValue);
        this.processId = Integer.parseInt (processId);
    }

    public void increment (){
        timeValue++;
    }

    public void setTimeValue (int timeValue){
        this.timeValue = timeValue;
    }

    public int getTimeValue (){
        return timeValue;
    }

    public int getProcessId (){
        return processId;
    }


    public String toString (){
        return "TS " + timeValue + " PID " + processId;
    }
}
