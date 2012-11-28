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

    public boolean canRead(TransactionOperation op){
        return true;
    }

    public boolean canWrite(TransactionOperation op){
        return true;
    }

    public void write(String value){
        this.value = value;
    }

    public String toString(){
        String currValue = value.equals("") ? "None": value;
        return "<DataItem " + label + ": " + currValue + ">";
    }

}
