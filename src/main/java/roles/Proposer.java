package roles;

import javafx.concurrent.Task;
import messaging.helpers.*;
import helpers.Site;
import messaging.MessagingClient;
import sun.security.util.ManifestEntryVerifier;

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
    static String currentValue = null;
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

        if(stage == 1)
            proposalNumber = getProposalNumber();

        for(Map.Entry<String, Site> client :siteHashMap.entrySet()){


            try {
                String destinationAddress = client.getValue().getIpAddress();
                int port = client.getValue().getRandomPort();
                Acceptor acceptorInstance  = Acceptor.getInstance();
                Message m;

                //to send a propose message to acceptor
                if(stage == 1){
//                    System.out.println("Sending messages" + proposalNumber);

                    if(client.getValue().getSiteNumber() == site.getSiteNumber()){
//                        System.out.println("Adding self to acceptor");
                        approvalFrom.add(site.getSiteNumber());
                        PrepareMessage message = composeProposal(proposalNumber);
                        System.out.println("Proposing to self for " + message.getLogPosition());
                        acceptorInstance.processPrepareRequest(message);
                    }else {
                        MessagingClient mClient = new MessagingClient(destinationAddress, port);
                        mClient.send(composeProposal(proposalNumber));
                        mClient.close();
                    }


                }

                // to send an accept message to acceptors
                if(stage == 2) {

                    if(client.getValue().getSiteNumber() == site.getSiteNumber()){
                        acceptorInstance.processAcceptRequest(composeAccept());
                    }else {
                        MessagingClient mClient = new MessagingClient(destinationAddress, port);
                        mClient.send(composeAccept());
                        mClient.close();
                    }
                }
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }

    }


    private PrepareMessage composeProposal(String proposalNumber){

        PrepareMessage proposalMessage = new PrepareMessage();
        proposalMessage.setProposalNumber(proposalNumber);
        proposalMessage.setMessageType(1);
        proposalMessage.setLogPosition(Learner.log.size());
        proposalMessage.setFrom(site.getSiteNumber());
        return proposalMessage;

    }

    private AcceptMessage composeAccept(){
        AcceptMessage acceptMessage = new AcceptMessage();
        acceptMessage.setMessageType(2);
        acceptMessage.setLogPosition(Learner.log.size());
        acceptMessage.setCompleteProposalNumber(latestProposalCombination);
        acceptMessage.setProposalNumber(maxProposalNumber);
        acceptMessage.setProposedValue(currentValue);
        acceptMessage.setFrom(site.getSiteNumber());
        return acceptMessage;
    }


    public void initiateProposal(String reservation){

        String input[] = reservation.split(" ");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(new Tasks());
        currentValue = reservation;

        approvalFrom = new HashSet<>();

        System.out.println("Proposing for Log Position" + Learner.log.size());
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


        System.out.println("Received msg from " + ack.getFrom() + " for position " + ack.getLogPosition());
        if(!wasSupported){
            //TODO:Proposal was denied, need to propose with a bigger proposal num
           System.out.println("The reservation was rejected");
        }else{
            //TODO: Add to the set
            PrepareAck prepareMessage = (PrepareAck)ack;

            approvalFrom.add(prepareMessage.getFrom());

            String proposed = prepareMessage.getAccNum();

            if(Integer.parseInt(proposed) < maxRecvdAckNum){
                System.out.println("There seems to be a value existing for this log position, now prpoposing"+ prepareMessage.getAccValue());
                currentValue = prepareMessage.getAccValue();
            }

            if(approvalFrom.size() > siteHashMap.size()/2 && !acceptSent){

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