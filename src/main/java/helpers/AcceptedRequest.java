package helpers;

import roles.Acceptor;

public class AcceptedRequest {

    public AcceptedRequest(int maxPrepare){
        this.maxPrepare = maxPrepare;
    }
    public int getMaxPrepare() {
        return maxPrepare;
    }

    public void setMaxPrepare(int maxPrepare) {
        this.maxPrepare = maxPrepare;
    }

    public int getAccNum() {
        return accNum;
    }

    public void setAccNum(int accNum) {
        this.accNum = accNum;
    }

    public String getAccVal() {
        return accVal;
    }

    public void setAccVal(String accVal) {
        this.accVal = accVal;
    }

    int maxPrepare;
    int accNum = -1;
    String accVal;
}
