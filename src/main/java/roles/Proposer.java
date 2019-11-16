package roles;

import javafx.concurrent.Task;
import messaging.helpers.AcceptMessage;
import messaging.helpers.Message;
import messaging.helpers.PrepareMessage;
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
        int logPosition = log.size();
        //Compose the Number
        System.out.println("log position is:" + logPosition);
        String proposalNumber = maxProposalNumber +"-"+logPosition+"-"+site.getSiteNumber();
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
                    System.out.println("Sending messages" + proposalNumber);

                    if(client.getValue().getSiteNumber() == site.getSiteNumber()){
                        System.out.println("Adding self to acceptor");
                        approvalFrom.add(site.getSiteNumber());
                    }else {
                        MessagingClient mClient = new MessagingClient(destinationAddress, port);
                        mClient.send(composeProposal(proposalNumber));
                        mClient.close();
                    }


                }

                // to send an accept message to acceptors
                if(stage == 2) {
                    if(client.getValue().getSiteNumber() == site.getSiteNumber()){
                        System.out.println("TODO:ACK-ING self");
//                        approvalFrom.add(site.getSiteNumber());
                    }else {
                        MessagingClient mClient = new MessagingClient(destinationAddress, port);
                        System.out.print("sending accept messages");
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
        acceptMessage.setCompleteProposalNumber(latestProposalCombination);
        acceptMessage.setProposalNumber(maxProposalNumber);
        acceptMessage.setProposedValue(currentValue);
        return acceptMessage;
    }

    public void initiateProposal(String reservation){

        String input[] = reservation.split(" ");
        String clientName = input[1];
        String flightNumbers[] = input[2].split(",");
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Future<String> future = executor.submit(new Tasks());

        approvalFrom = new HashSet<>();
        sendMessages(1);

        try{
            System.out.println("Waiting");
            future.get(3, TimeUnit.SECONDS);
            System.out.println("Finished");
        }catch (Exception e){

            future.cancel(true);
        }

        try {
            System.out.println("Attempt 1 " + approvalFrom.size() + " " + siteHashMap.size() / 2);
            future = executor.submit(new Tasks());
            future.get(3, TimeUnit.SECONDS);
            checkSetAndAttemptSend();
        }catch (Exception e1){
           future.cancel(true);
        }

        try {
            System.out.println("Attempt 2 " + approvalFrom.size() + " " + siteHashMap.size() / 2);
            future = executor.submit(new Tasks());
            future.get(3, TimeUnit.SECONDS);
            checkSetAndAttemptSend();
        }catch (Exception e1){
            future.cancel(true);
        }

        try {
            System.out.println("Attempt 3 " + approvalFrom.size() + " " + siteHashMap.size() / 2);
            future = executor.submit(new Tasks());
            future.get(3, TimeUnit.SECONDS);
            checkSetAndAttemptSend();
        }catch (Exception e1){
            future.cancel(true);
        }



        currentValue = reservation;
        //TODO : Figure out how multiple requests will be handled here

        executor.shutdownNow();


    }

    private void checkSetAndAttemptSend() throws Exception {
        if (approvalFrom.size() <= siteHashMap.size() / 2) {
            approvalFrom = new HashSet<>();
            System.out.println("Sending another round of messages");
            sendMessages(1);
        } else {
            throw new Exception();
        }
    }

    public void processProposalAcks(Message ack, boolean wasSupported){

        if(!wasSupported){
            //TODO:Proposal was denied

        }else{
            //TODO: Add to the set
            approvalFrom.add(ack.getFrom());
            if(approvalFrom.size() > siteHashMap.size()/2 && !acceptSent){

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