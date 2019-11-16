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
    HashMap<Integer,String> siteIDMap = null;
    Site site = null;

    public Acceptor(Site siteInformation, HashMap<String, Site> siteMap,HashMap<Integer,String> siteIDMap){
        this.site = siteInformation;
        this.maxPrepare = 0;
        this.siteHashMap = siteMap;
        this.siteIDMap = siteIDMap;
        accNum = null;
        accValue = null;
    }

    public static Acceptor getInstance(Site siteInformation, HashMap<String, Site> siteMap,HashMap<Integer,String> siteIDMap){
        if (instance == null){
            instance = new Acceptor(siteInformation, siteMap,siteIDMap);
        }
        return instance;
    }
    public static Acceptor getInstance(){
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
        learnmessage.setMessageType(4);

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
                System.out.println("sending ack" + ack.getMessageType());
                String destinationAddress = siteHashMap.get(siteIDMap.get(sender)).getIpAddress();
                int port = siteHashMap.get(siteIDMap.get(sender)).getRandomPort();
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
        System.out.println("message from sender:" + sender);
        String proposalNumber[] = message.getProposalNumber().split("-");
        String proposed = proposalNumber[0] + proposalNumber[2];
        PrepareAck ackmessage = new PrepareAck();
        ackmessage.setaccNum(accNum);
        ackmessage.setAccValue(accValue);
        ackmessage.setFrom(site.getSiteNumber());

        if (Integer.parseInt(proposed) < maxPrepare) {
            ackmessage.setAck(false);
        }
        else
            ackmessage.setAck(true);

        ackmessage.setMessageType(3);
        sendAckMessages(sender,ackmessage);
        maxPrepare = Integer.parseInt(proposed);

    }

    public void processAcceptRequest(AcceptMessage message) {

        int sender = message.getFrom();
        String proposalNumber[] = message.getCompleteProposalNumber().split("-");
        String proposed = proposalNumber[0] + proposalNumber[2];
        PrepareAck ackmessage = new PrepareAck();
        System.out.println(proposalNumber +"  "+ maxPrepare);
        if (Integer.parseInt(proposed) < maxPrepare) {
            //sending max prepare in case of Nack
            ackmessage.setaccNum(String.valueOf(maxPrepare));
            ackmessage.setMessageType(5);
            ackmessage.setAck(false);
            sendAckMessages(sender,ackmessage);

        }

        if (Integer.parseInt(proposed) == maxPrepare) {
            try {
                accNum = String.valueOf(proposalNumber);
                accValue = String.valueOf(message.getProposedValue());
                maxPrepare = Integer.parseInt(proposed);
                ackmessage.setaccNum(accNum);
                ackmessage.setAccValue(accValue);
                ackmessage.setAck(true);
                ackmessage.setMessageType(6);
                //sending acceptance message to proposer
                sendAckMessages(sender, ackmessage);
            }
            catch (Exception e){
                System.out.println(e);
            }

            //sending acceptance to learners
            //sendMessages();
        }

    }
}
