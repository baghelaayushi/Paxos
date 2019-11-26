package messaging.helpers;

import java.io.Serializable;

public class Message implements Serializable {

    int messageType;
    int from;
    int logPosition;
    String originalValue;
    String method;

    String logValue;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getOriginalValue() {
        return originalValue;
    }

    public void setOriginalValue(String originalValue) {
        this.originalValue = originalValue;
    }

    public void setLogValue(String logValue) {
        this.logValue = logValue;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public void setLogPosition(int logPosition) {this.logPosition = logPosition;}

    public int getLogPosition(){return logPosition;}

    public void setlogValue(String logValue){ this.logValue = logValue;}

    public String getLogValue(){ return logValue;}

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