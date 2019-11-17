package messaging.helpers;
import helpers.Event;

public class LearnMessage extends Message {
    String accNum;
    Event accValue;

    public String getAccNum(){
        return accNum;
    }
    public Event getAccValue(){
        return accValue;
    }
    public void setAccNum(String accNum){
        this.accNum = accNum;
    }
    public void setAccValue(Event accValue){
        this.accValue = accValue;
    }
}
