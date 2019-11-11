package roles;

import messaging.helpers.AcceptMessage;
import messaging.helpers.Message;
import messaging.helpers.PrepareMessage;
import helpers.Site;
import messaging.MessagingClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class Proposer {

    static Proposer instance = null;

    Site site = null;
    int maxProposalNumber = -1;
    List<String> log = null;
    HashMap<String, Site> siteHashMap = null;
    String currentValue = null;
    HashSet<Integer> approvalFrom = new HashSet<Integer>();
    boolean acceptSent = false;

    public Proposer(Site siteInformation, List<String> log, HashMap<String, Site> siteMap){

        //TODO:System could have crashed, check that.
        this.site = siteInformation;
        this.log = log;
        this.siteHashMap = siteMap;
    }
    public static Proposer getInstance(Site siteInformation, List<String> log, HashMap<String, Site> siteMap){
        if (instance == null){
            instance = new Proposer(siteInformation, log, siteMap);
        }
        return instance;
    }

    private String getProposalNumber(){

        //get Proposal Value
        maxProposalNumber++;
        //getLocationInLog
        int logPosition = log.size()-1;
        //Compose the Number
        String proposalNumber = maxProposalNumber +"-"+logPosition+"-"+site.getSiteNumber();
        return proposalNumber;
    }

    private void sendMessages(int stage){

        for(Map.Entry<String, Site> client :siteHashMap.entrySet()){

            if(client.getValue().getSiteNumber() == site.getSiteNumber())
                continue;

            try {
                String destinationAddress = client.getValue().getIpAddress();
                int port = client.getValue().getRandomPort();
                MessagingClient mClient = new MessagingClient(destinationAddress, port);


                if(stage == 1){
                    String proposalNumber = getProposalNumber();
                    mClient.send(composeProposal(proposalNumber));
                }
                if(stage == 2)
                    mClient.send(composeAccept());
                mClient.close();
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }

    }


    private Message composeProposal(String proposalNumber){

        PrepareMessage proposalMessage = new PrepareMessage();
        proposalMessage.setProposalNumber(proposalNumber);
        proposalMessage.setMessageType(1);
        proposalMessage.setFrom(site.getSiteNumber());
        return proposalMessage;

    }

    private Message composeAccept(){
        AcceptMessage acceptMessage = new AcceptMessage();
        acceptMessage.setMessageType(2);
        acceptMessage.setProposalNumber(maxProposalNumber);
        acceptMessage.setProposedValue(currentValue);
        return acceptMessage;
    }

    public void initiateProposal(String reservation){

        String input[] = reservation.split(" ");
        String clientName = input[1];
        String flightNumbers[] = input[2].split(",");

        sendMessages(1);

        currentValue = reservation;
        //TODO : Figure out how multiple requests will be handled here


    }

    public void processProposalAcks(Message ack, boolean wasSupported){

        if(!wasSupported){
            //TODO:Proposal was denied
        }else{
            //TODO: Add to the set
            approvalFrom.add(ack.getFrom());
            if(approvalFrom.size() > siteHashMap.size()/2 && !acceptSent){
                //TODO:Proposal is approved, begin processing accept

                sendMessages(2);
                acceptSent = true;

            }
        }
    }






}
