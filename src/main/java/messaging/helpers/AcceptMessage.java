package messaging.helpers;

import helpers.Event;

public class AcceptMessage extends Message {
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

    public Event getProposedValue() {
        return proposedValue;
    }

    public void setProposedValue(Event proposedValue) {
        this.proposedValue = proposedValue;
    }

    Event proposedValue;

}
