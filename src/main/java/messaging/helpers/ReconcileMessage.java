package messaging.helpers;

import java.util.List;

public class ReconcileMessage extends Message {

    public void setLog(List<String> log) {
        this.log = log;
    }

    public List<String> log;

    public boolean isAck() {
        return Ack;
    }

    public void setAck(boolean ack) {
        Ack = ack;
    }

    boolean Ack;


    public List<String> getLog() {
        return log;
    }

}
