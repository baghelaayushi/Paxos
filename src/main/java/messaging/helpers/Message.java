package messaging.helpers;

import java.io.Serializable;

public class Message implements Serializable {

    int messageType;
    int from;
    int logPosition;

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public void setLogPosition(int logPosition) {this.logPosition = logPosition;}

    public int getLogPosition(){return logPosition;}

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    String to;


    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }




}