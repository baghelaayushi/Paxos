package messaging.helpers;

public class LearnMessage extends Message {
    String accNum;
    String accValue;

    public String getAccNum(){
        return accNum;
    }
    public String getAccValue(){
        return accValue;
    }
    public void setAccNum(String accNum){
        this.accNum = accNum;
    }
    public void setAccValue(String accValue){
        this.accValue = accValue;
    }
}
