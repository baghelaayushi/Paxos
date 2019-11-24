package messaging.helpers;

import roles.Learner;

public class AcceptMessage extends Message {
    final int messageType = 2;
    public int getProposalNumber() {
        return proposalNumber;
    }

    public void setProposalNumber(int proposalNumber) {
        this.proposalNumber = proposalNumber;
    }

    public String getCompleteProposalNumber() {
        return completeProposalNumber;
    }

    public void setCompleteProposalNumber(String completeProposalNumber) {
        this.completeProposalNumber = completeProposalNumber;
    }

    String completeProposalNumber;

    int proposalNumber;

    public String getProposedValue() {
        return proposedValue;
    }

    public void setProposedValue(String proposedValue) {
        this.proposedValue = proposedValue;
    }

    String proposedValue;

    public AcceptMessage(int logPosition, String latestProposalCombination, int maxProposalNumber, String proposedValue, int siteNumber){
            super.setMessageType(messageType);
            super.setLogPosition(logPosition);
            setCompleteProposalNumber(latestProposalCombination);
            setProposalNumber(maxProposalNumber);
            setProposedValue(proposedValue);
            super.setFrom(siteNumber);

    }
}
