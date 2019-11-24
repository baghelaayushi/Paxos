package messaging.helpers;

import roles.Learner;

public class PrepareMessage extends Message {


    String proposalNumber;
    static final int messageType = 1;

    public String getProposalNumber() {
        return proposalNumber;
    }

    public void setProposalNumber(String proposalNumber) {
        this.proposalNumber = proposalNumber;
    }

    public PrepareMessage(String proposalNumber, int logSize, int siteNumber){

        super.setLogPosition(logPosition);
        super.setFrom(siteNumber);
        super.setMessageType(messageType);
        setProposalNumber(proposalNumber);

    }

}
