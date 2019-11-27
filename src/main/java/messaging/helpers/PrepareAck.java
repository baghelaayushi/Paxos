package messaging.helpers;

public class PrepareAck extends Message {
    public boolean isAck() {
        return Ack;
    }

    boolean Ack;
    String accNum;

    public String getAccNum() {
        return accNum;
    }

    public void setAccNum(String accNum) {
        this.accNum = accNum;
    }

    public String getAccValue() {
        return accValue;
    }

    String accValue;

    public void setAck(boolean Ack){
        this.Ack = Ack;
    }

    public void setAccValue(String accValue){
        this.accValue = accValue;
    }


}
