package messaging.helpers;

public class AcceptMessage extends Message {
    final int messageType = 2;

    public String getProposalNumber() {
        return proposalNumber;
    }

    public void setProposalNumber(String proposalNumber) {
        this.proposalNumber = proposalNumber;
    }

    String proposalNumber;

    public String getProposedValue() {
        return proposedValue;
    }

    public void setProposedValue(String proposedValue) {
        this.proposedValue = proposedValue;
    }

    String proposedValue;

    public AcceptMessage(int logPosition, String latestProposalCombination,String proposedValue, int siteNumber){
            super.setMessageType(messageType);
            super.setLogPosition(logPosition);
            setProposalNumber(latestProposalCombination);
            setProposedValue(proposedValue);
            super.setFrom(siteNumber);

    }
}
