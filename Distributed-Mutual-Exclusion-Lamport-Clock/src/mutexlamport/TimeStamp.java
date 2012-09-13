package mutexlamport;

public class TimeStamp implements Comparable<TimeStamp> {
    public static final int EQUAL = 0;
    public static final int AFTER = 1;
    public static final int BEFORE = -1;
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

    public TimeStamp (TimeStamp timeStamp){
        this.timeValue = timeStamp.getTimeValue ();
        this.processId = timeStamp.getProcessId ();
    }
    
    /**
     * @return BEFORE/EQUAL/AFTER if this is less/equal/greater than
     * otherTimeStamp.
     */
    public int compareTo (TimeStamp otherTimeStamp){
        if (this == otherTimeStamp){
            return EQUAL;
        }

        int otherTimeValue = otherTimeStamp.getTimeValue ();
        int otherProcessId = otherTimeStamp.getProcessId ();

        if (timeValue < otherTimeValue){
            return BEFORE;
        }
        else if (timeValue == otherTimeValue) {
            if (processId < otherProcessId){
                return BEFORE;
            } else if (processId == otherProcessId) {
                return EQUAL;
            } else {
                return AFTER;
            }
        } else {
            return AFTER;
        }
    }

    public boolean equals (Object o){
        if (!(o instanceof TimeStamp)){
            return false;
        }
        TimeStamp otherTimeStamp = (TimeStamp) o;
        return timeValue == otherTimeStamp.getTimeValue ()
                && processId == otherTimeStamp.getProcessId ();
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
        return "PID " + processId
                + " TS " + timeValue;
    }
}
