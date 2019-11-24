package roles;

import helpers.AcceptedRequest;
import messaging.helpers.*;
import helpers.Site;
import messaging.MessagingClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


//maxPre
//accNum
//accVal

public class Acceptor {
    static Acceptor instance = null;
    int maxPrepare = -1;
    HashMap<Integer, AcceptedRequest>  acceptedEntries = null;
    String accNum;
    String accValue;
    HashMap<String, Site> siteHashMap = null;
    HashMap<Integer,String> siteIDMap = null;
    Site site = null;

    public Acceptor(Site siteInformation, HashMap<String, Site> siteMap,HashMap<Integer,String> siteIDMap){
        this.site = siteInformation;
        this.maxPrepare = 0;
        this.siteHashMap = siteMap;
        this.siteIDMap = siteIDMap;
//        this.accEntry = new HashMap<>();
        this.acceptedEntries = new HashMap<>();
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


    private void sendCommitToLearner(int sender, int logPosition){
        Learner instance = Learner.getInstance();
        LearnMessage learnmessage = new LearnMessage();
        learnmessage.setAccNum(String.valueOf(acceptedEntries.get(logPosition).getAccNum()));
        learnmessage.setAccValue(String.valueOf(acceptedEntries.get(logPosition).getAccVal()));
        learnmessage.setMessageType(8);
        learnmessage.setFrom(site.getSiteNumber());
        learnmessage.setLogPosition(logPosition);

        for(Map.Entry<String, Site> client :siteHashMap.entrySet()){


            try {
                String destinationAddress = client.getValue().getIpAddress();
                int port = client.getValue().getRandomPort();

                if(client.getValue().getSiteNumber() == site.getSiteNumber()){
                    instance.learner(learnmessage);
                }
                else {
                    MessagingClient mClient = new MessagingClient(destinationAddress, port);
                    mClient.send(learnmessage);
                    mClient.close();
                }
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }

    }

    private void sendAckMessages(int sender, PrepareAck ack){
            try {
                System.out.println("Sender is" + siteHashMap.get(siteIDMap.get(sender)).getIpAddress());
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

    private void sendReconcileMessage(int sender, ReconcileMessage ack){
        try {
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
        Proposer proposer = Proposer.getInstance(null,null,null);
        int sender = message.getFrom();
        String proposalNumber[] = message.getProposalNumber().split("-");
        String proposed = proposalNumber[0] + proposalNumber[1];


        PrepareAck ackmessage = new PrepareAck();
        //if the log entry is empty for acceptor too
        //create a new entry in hashmap with max prepare as proposed and accnum and acc val as -1
        System.out.println("Accepting for Log Position " + message.getLogPosition());
        if(!acceptedEntries.containsKey(message.getLogPosition())) {

            acceptedEntries.put(message.getLogPosition(), new AcceptedRequest(Integer.parseInt(proposed)));
        }
        int prep = acceptedEntries.get(message.getLogPosition()).getMaxPrepare();
        if (Integer.parseInt(proposed) < prep) {
            ackmessage.setAck(false);
        }
        else
            ackmessage.setAck(true);

        ackmessage.setAccNum(String.valueOf(acceptedEntries.get(message.getLogPosition()).getAccNum()));
        ackmessage.setAccValue(acceptedEntries.get(message.getLogPosition()).getAccVal());
        ackmessage.setFrom(site.getSiteNumber());

        ackmessage.setMessageType(3);

        if(sender == site.getSiteNumber()){
            proposer.processProposalAcks(ackmessage,true);
        }
        else {
            sendAckMessages(sender, ackmessage);
        }
        maxPrepare = Integer.parseInt(proposed);

    }

    public void processAcceptRequest(AcceptMessage message) {

        int sender = message.getFrom();
        String proposalNumber[] = message.getCompleteProposalNumber().split("-");
        String proposed = proposalNumber[0] + proposalNumber[1];

        PrepareAck ackmessage = new PrepareAck();
        int prep = acceptedEntries.get(message.getLogPosition()).getMaxPrepare();
        if (Integer.parseInt(proposed) < prep) {
            //sending max prepare in case of Nack
            ackmessage.setaccNum(String.valueOf(maxPrepare));
            ackmessage.setMessageType(5);
            ackmessage.setAck(false);

            sendAckMessages(sender,ackmessage);

        }
        else{
            try {
                accNum = proposed;
                accValue = message.getProposedValue();
                maxPrepare = Integer.parseInt(proposed);

                AcceptedRequest acceptedRequest = acceptedEntries.get(message.getLogPosition());
                acceptedRequest.setMaxPrepare(maxPrepare);
                acceptedRequest.setAccNum(Integer.parseInt(accNum));
                acceptedRequest.setAccVal(accValue);
                acceptedEntries.replace(message.getLogPosition(), acceptedRequest);

                ackmessage.setaccNum(accNum);
                ackmessage.setAccValue(accValue);
                ackmessage.setAck(true);
                ackmessage.setMessageType(6);
                //sending acceptance message to proposer
                if(sender == site.getSiteNumber()){
                    //TODO for nack
                }else {
                    sendAckMessages(sender, ackmessage);
                }
            }
            catch (Exception e){
                System.out.println(e);
            }

            //sending acceptance to learners
            System.out.println("Sending message to learner");
            sendCommitToLearner(sender,message.getLogPosition());
        }

    }
}
