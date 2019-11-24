package roles;

import messaging.helpers.*;
import helpers.Site;
import messaging.MessagingClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class Proposer {

    static Proposer instance = null;

    Site site = null;
    int maxProposalNumber = -1;
    String latestProposalCombination = "";
    HashMap<String, Site> siteHashMap = null;
    String currentValue = null;
    HashSet<Integer> approvalFrom = new HashSet<Integer>();
    boolean acceptSent = false;

    int maxRecvdAckNum = -1;

    public Proposer(Site siteInformation, List<String> log, HashMap<String, Site> siteMap){

        //TODO:System could have crashed, check that.
        this.site = siteInformation;
        this.siteHashMap = siteMap;
    }
    public static Proposer getInstance(Site siteInformation, List<String> log, HashMap<String, Site> siteMap){
        if (instance == null){
            instance = new Proposer(siteInformation, log, siteMap);
        }
        return instance;
    }

    private String getProposalNumber(){

        maxProposalNumber++;
        String proposalNumber = maxProposalNumber +"-"+site.getSiteNumber();
        latestProposalCombination = proposalNumber;
        return proposalNumber;
    }

    private void sendMessages(int stage){

        String proposalNumber = null;

        if(stage == 1){
            proposalNumber = getProposalNumber();
            System.err.println("% sending prepare("+proposalNumber+") to all sites");
        }

        if(stage == 2){
            System.err.println("% sending accept("+maxProposalNumber+","+currentValue+") to all sites");
        }

        for(Map.Entry<String, Site> client :siteHashMap.entrySet()){


            try {
                String destinationAddress = client.getValue().getIpAddress();
                int port = client.getValue().getRandomPort();
                Acceptor acceptorInstance  = Acceptor.getInstance();

                //to send a propose message to acceptor
                if(stage == 1){
                    PrepareMessage message = new PrepareMessage(proposalNumber, Learner.log.size(), site.getSiteNumber());
                    if(client.getValue().getSiteNumber() == site.getSiteNumber()){
                        approvalFrom.add(site.getSiteNumber());
                        acceptorInstance.processPrepareRequest(message);
                    }else {
                        MessagingClient mClient = new MessagingClient(destinationAddress, port);

                        mClient.send(message);
                        mClient.close();
                    }

                }

                // to send an accept message to acceptors
                if(stage == 2) {

                    AcceptMessage message = new AcceptMessage(Learner.log.size(), latestProposalCombination, maxProposalNumber, currentValue, site.getSiteNumber());
                    if(client.getValue().getSiteNumber() == site.getSiteNumber()){
                        acceptorInstance.processAcceptRequest(message);
                    }else {
                        MessagingClient mClient = new MessagingClient(destinationAddress, port);
                        mClient.send(message);
                        mClient.close();
                    }
                }
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }

    }


    public void initiateProposal(String reservation, String method){

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(new Tasks());
        currentValue = reservation;

        approvalFrom = new HashSet<>();
        acceptSent = false;

        if(method.equals("reserve"))
            System.out.println("Reservation submitted for " + reservation.split(" ")[1]+".");
        else
            System.out.println("Reservation cancelled for " + reservation.split(" ")[1]+".");


        sendMessages(1);

        try{
            future.get(3, TimeUnit.SECONDS);
        }catch (Exception e){

            future.cancel(true);
        }

        try {
            future = executor.submit(new Tasks());
            future.get(3, TimeUnit.SECONDS);
            checkSetAndAttemptSend();
        }catch (Exception e1){
           future.cancel(true);
        }

        try {
            future = executor.submit(new Tasks());
            future.get(3, TimeUnit.SECONDS);
            checkSetAndAttemptSend();
        }catch (Exception e1){
            future.cancel(true);
        }

        try {
            future = executor.submit(new Tasks());
            future.get(3, TimeUnit.SECONDS);
            checkSetAndAttemptSend();
        }catch (Exception e1){
            future.cancel(true);
        }

        currentValue = reservation;

        executor.shutdownNow();


    }

    private void checkSetAndAttemptSend() throws Exception {
        if (approvalFrom.size() <= siteHashMap.size() / 2) {
            approvalFrom = new HashSet<>();
            sendMessages(1);
        } else {
            throw new Exception();
        }
    }

    public void processProposalAcks(Message ack, boolean wasSupported){


//        System.out.println("Received msg from " + ack.getFrom() + " for position " + ack.getLogPosition());
        if(!wasSupported){
            //TODO:Proposal was denied, need to propose with a bigger proposal num
           System.out.println("The reservation was rejected");
        }else{
            //TODO: Add to the set
            PrepareAck prepareMessage = (PrepareAck)ack;

            approvalFrom.add(prepareMessage.getFrom());

            String proposed = prepareMessage.getAccNum();

            System.err.println("% received promise("+prepareMessage.getAccNum()+","+prepareMessage.getAccValue()+"" +
                    ") from site " + prepareMessage.getFrom());

            if(prepareMessage.getAccValue() != null){

                if(maxRecvdAckNum != -1){
                    if(Integer.parseInt(proposed) > maxRecvdAckNum){
                        maxRecvdAckNum = Integer.parseInt(proposed);
                        currentValue = prepareMessage.getAccValue();
                    }
                }else{
                    maxRecvdAckNum = Integer.parseInt(prepareMessage.getAccNum());
                    currentValue = prepareMessage.getAccValue();
                }


            }


            if(approvalFrom.size() > siteHashMap.size()/2 && !acceptSent){

                System.err.println("% received accept messages from a majority");
                if(prepareMessage.getFrom() != site.getSiteNumber())
                    sendMessages(2);

                acceptSent = true;

            }
        }
    }

}

class Tasks implements Callable<String> {
    @Override
    public String call() throws Exception {
        Thread.sleep(3000); // Just to demo a long running task of 4 seconds.
        return "Ready!";
    }
}