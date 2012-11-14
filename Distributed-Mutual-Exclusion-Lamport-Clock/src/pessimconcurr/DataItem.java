package pessimconcurr;

/** 
 * Class to represent a distributed data item.
 */
public class DataItem {
    String label;
    String value;
    
    public DataItem (String label) {
        this.label = label;
        this.value = "";
    }

    public String read(){
        return value;
    }

    public void write(String value){
        this.value = value;
    }
}
