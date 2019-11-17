package messaging.helpers;

public class PrepareAck extends Message {
    boolean Ack;
    String accNum;
    String accValue;

    public void setAck(boolean Ack){
        this.Ack = Ack;
    }
    public void setaccNum(String accNum){
        this.accNum = accNum;
    }
    public void setAccValue(String accValue){
        this.accValue = accValue;
    }
}
