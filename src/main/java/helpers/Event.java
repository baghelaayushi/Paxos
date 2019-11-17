package helpers;

import java.io.Serializable;
import java.sql.Timestamp;

public class Event implements Serializable {

    private String operationType;
    private int time;
    private int NodeId;

    public Event(String operationType,int NodeId, int time){
        this.operationType = operationType;
        this.NodeId = NodeId;
        this.time = time;
    }

    public String getOperationType(){
        return this.operationType;
    }


    public int getTime() {

        return this.time;
    }

    public int getNodeId() {
        return this.NodeId;
    }

}
