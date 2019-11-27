package messaging.helpers;

public class LogPositionMessage extends Message {



    @Override
    public int getLogPosition() {
        return logPosition;
    }

    @Override
    public void setLogPosition(int logPosition) {
        this.logPosition = logPosition;
    }

    int logPosition = -1;

    public LogPositionMessage(int logPosition, int from){
        super.setMessageType(10);
        this.setLogPosition(10);
        super.setFrom(from);
        this.logPosition = logPosition;
    }

}
