package roles;

import messaging.helpers.Message;
import messaging.helpers.PrepareMessage;
import helpers.Site;
import messaging.MessagingClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Proposer {

    Proposer instance = null;

    Site site = null;
    int maxPrepare = -1;
    List<String> log = null;
    HashMap<String, Site> siteHashMap = null;


    public Proposer(Site siteInformation, List<String> log, HashMap<String, Site> siteMap){

        //TODO:System could have crashed, check that.
        this.site = siteInformation;
        this.log = log;
        this.siteHashMap = siteMap;
    }
    public Proposer getInstance(Site siteInformation, List<String> log, HashMap<String, Site> siteMap){
        if (instance == null){
            instance = new Proposer(siteInformation, log, siteMap);
        }
        return instance;
    }

    public String getProposalNumber(){

        //get Proposal Value
        maxPrepare++;
        //getLocationInLog
        int logPosition = log.size()-1;
        //Compose the Number
        String proposalNumber = maxPrepare+"-"+logPosition+"-"+site.getSiteNumber();
        return proposalNumber;
    }

    public void sendPrepare(){

        for(Map.Entry<String, Site> client :siteHashMap.entrySet()){

            if(client.getValue().getSiteNumber() == site.getSiteNumber())
                continue;

            try {
                String destinationAddress = client.getValue().getIpAddress();
                int port = client.getValue().getRandomPort();
                MessagingClient mClient = new MessagingClient(destinationAddress, port);

                mClient.send(composeProposal());
                mClient.close();
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }

    }


    public Message composeProposal(){

        PrepareMessage proposalMessage = new PrepareMessage();
        proposalMessage.setProposalNumber(getProposalNumber());
        proposalMessage.setMessageType(1);

        return proposalMessage;

    }



    public void sendAccept(){
        //TODO:Implement a sendAccept message to the acceptors
    }



}
