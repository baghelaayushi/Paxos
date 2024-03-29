package messaging.helpers;

import roles.Learner;

import java.io.Serializable;

public class PrepareMessage extends Message implements Serializable {


    String proposalNumber;
    final int messageType = 1;

    public String getProposalNumber() {
        return proposalNumber;
    }

    public void setProposalNumber(String proposalNumber) {
        this.proposalNumber = proposalNumber;
    }

    public PrepareMessage(String proposalNumber, int position, int siteNumber){

        super.setLogPosition(position);
        super.setFrom(siteNumber);
        super.setMessageType(messageType);
        setProposalNumber(proposalNumber);

    }

}
