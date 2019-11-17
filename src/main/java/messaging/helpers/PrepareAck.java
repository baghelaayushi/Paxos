package messaging.helpers;

import helpers.Event;

public class PrepareAck extends Message {
    boolean Ack;
    String accNum;
    Event accValue;

    public void setAck(boolean Ack){
        this.Ack = Ack;
    }
    public void setaccNum(String accNum){
        this.accNum = accNum;
    }
    public void setAccValue(Event accValue){
        this.accValue = accValue;
    }
}
