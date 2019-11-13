package roles;

import messaging.helpers.*;
import helpers.Site;
import messaging.MessagingClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Acceptor {
    static Acceptor instance = null;
    int maxPrepare;
    static String accNum;
    static String accValue;
    HashMap<String, Site> siteHashMap = null;
    Site site = null;

    public Acceptor(Site siteInformation, HashMap<String, Site> siteMap){
        this.site = siteInformation;
        this.maxPrepare = 0;
        this.siteHashMap = siteMap;
        accNum = null;
        accValue = null;
    }

    public static Acceptor getInstance(Site siteInformation, HashMap<String, Site> siteMap){
        if (instance == null){
            instance = new Acceptor(siteInformation, siteMap);
        }
        return instance;
    }

    public String getAccNumAccVal(){
        //returns ack for the first call
        if(accNum== null && accValue == null)
            return null;

        return accNum+"-"+accValue;
    }

    private void sendMessages(){
        LearnMessage learnmessage = new LearnMessage();
        learnmessage.setAccNum(accNum);
        learnmessage.setAccValue(accValue);
        learnmessage.setMessageType('4');

        for(Map.Entry<String, Site> client :siteHashMap.entrySet()){

            if(client.getValue().getSiteNumber() == site.getSiteNumber())
                continue;

            try {
                String destinationAddress = client.getValue().getIpAddress();
                int port = client.getValue().getRandomPort();
                MessagingClient mClient = new MessagingClient(destinationAddress, port);


                mClient.send(learnmessage);
                mClient.close();
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }

    }

    private void sendAckMessages(int sender, PrepareAck ack){
            try {
                String destinationAddress = siteHashMap.get(sender).getIpAddress();
                int port = siteHashMap.get(sender).getRandomPort();
                MessagingClient mClient = new MessagingClient(destinationAddress, port);


                mClient.send(ack);
                mClient.close();
            }
            catch (IOException e){
                e.printStackTrace();
            }

    }

    public void processPrepareRequest(PrepareMessage message) {
        int sender = message.getFrom();
        String proposalNumber[] = message.getProposalNumber().split("-");
        String proposed = proposalNumber[0] + proposalNumber[2];
        PrepareAck ackmessage = new PrepareAck();
        ackmessage.setaccNum(accNum);
        ackmessage.setAccValue(accValue);

        if (Integer.parseInt(proposed) < maxPrepare) {
            ackmessage.setAck(false);
        }
        else
            ackmessage.setAck(true);

        ackmessage.setMessageType('3');
        sendAckMessages(sender,ackmessage);
        maxPrepare = Integer.parseInt(proposed);

    }

    public void processAcceptRequest(AcceptMessage message) {
        int sender = message.getFrom();
        int proposalNumber = message.getProposalNumber();
        PrepareAck ackmessage = new PrepareAck();
        if (proposalNumber < maxPrepare) {
            //sending max prepare in case of Nack
            ackmessage.setaccNum(String.valueOf(maxPrepare));
            ackmessage.setMessageType('5');
            ackmessage.setAck(false);
            sendAckMessages(sender,ackmessage);

        }

        if (proposalNumber == maxPrepare) {
            accNum = String.valueOf(proposalNumber);
            accValue = String.valueOf(message.getProposedValue());
            maxPrepare = proposalNumber;
            ackmessage.setaccNum(accNum);
            ackmessage.setAccValue(accValue);
            ackmessage.setAck(true);
            ackmessage.setMessageType('5');
            //sending acceptance message to proposer
            sendAckMessages(sender,ackmessage);

            //sending acceptance to learners
            sendMessages();
        }

    }
}
