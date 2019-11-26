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
    static HashSet<Integer> valueLearned = new HashSet<Integer>();
    boolean acceptSent = false;
    static Boolean wonLastRound = false;
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


    private void sendMessages(int stage,int position){

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
                    PrepareMessage message = new PrepareMessage(proposalNumber, position, site.getSiteNumber());

                    if(client.getValue().getSiteNumber() == site.getSiteNumber()){
                        approvalFrom.add(site.getSiteNumber());
                        acceptorInstance.processPrepareRequest(message);
                        valueLearned.add(site.getSiteNumber());
                    }else {
                        MessagingClient mClient = new MessagingClient(destinationAddress, port);

                        mClient.send(message);
                        mClient.close();
                    }

                }

                // to send an accept message to acceptors
                if(stage == 2) {

                    AcceptMessage message = new AcceptMessage(position, latestProposalCombination, maxProposalNumber, currentValue, site.getSiteNumber());
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


    public void initiateProposal(String reservation, String method, int position){

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(new Tasks());
        currentValue = reservation;
        Learner learner = Learner.getInstance();

        approvalFrom = new HashSet<>();
        valueLearned = new HashSet<>();

        if(position ==0) {
            for (String s : learner.log) {
                if (s == null) {
                    break;
                }
                position++;
            }
        }

        acceptSent = false;

        if(method.equals("reserve"))
            System.out.println("Reservation submitted for " + reservation.split(" ")[1]+".");
        else if(method.equals("cancel"))
            System.out.println("Reservation cancelled for " + reservation.split(" ")[1]+".");


        if(!wonLastRound)
            sendMessages(1,position);
        else{
            System.err.println("% executing an optimized paxos");
            getProposalNumber();
            sendMessages(2, position);
        }

        try{
            future.get(3, TimeUnit.SECONDS);
        }catch (Exception e){

            future.cancel(true);
        }


        if(Learner.getLog()[position] == null)
            reAttempt(executor, future, position);
        if(Learner.getLog()[position] == null)
            reAttempt(executor, future, position);
        if(Learner.getLog()[position] == null)
            reAttempt(executor, future, position);

        currentValue = reservation;

        executor.shutdownNow();


    }

    private void reAttempt(ExecutorService executor, Future<String> future, int position) {
        try {
            future = executor.submit(new Tasks());
            future.get(3, TimeUnit.SECONDS);
            System.err.println(Learner.getLog()[position]);
            checkSetAndAttemptSend(position);
        }catch (Exception e1){
            future.cancel(true);
        }
    }

    private void checkSetAndAttemptSend(int position) throws Exception {

        if (!wonLastRound){
            if (approvalFrom.size() <= siteHashMap.size() / 2) {
                approvalFrom = new HashSet<>();
                sendMessages(1, position);
            } else {
                throw new Exception();
            }
        } else{
            if (valueLearned.size() <= siteHashMap.size() / 2) {
                valueLearned = new HashSet<>();
                sendMessages(2, position);
            } else {
                throw new Exception();
            }
        }



    }

    public void learnAcceptanceAcks(Message ack) {

        wonLastRound = false;
        PrepareAck message = (PrepareAck) ack;

        System.err.println("% value-accepted - received ack("+message.getAccNum()+","+message.getAccValue()+"" +
                ") from site " + message.getFrom());

        valueLearned.add(message.getFrom());

        if (valueLearned.size() > (siteHashMap.size() / 2)) {
//            System.out.println("I have won");
            wonLastRound = true;
        }
    }

    public void processProposalAcks(Message ack, boolean wasSupported){

        if(!wasSupported){
            //TODO:Proposal was denied, need to propose with a bigger proposal num
            PrepareAck message = (PrepareAck) ack;
            System.err.println("% Was rejected, reproposing with a higher value "
                    + ack.getOriginalValue()
                    + " max Prop should be atleast "+ message.getAccNum() +
                    "for position "+ message.getLogPosition());

            maxProposalNumber += Integer.parseInt(message.getAccNum());
            initiateProposal(ack.getOriginalValue(),"",1);
//
//           System.out.println("The reservation was rejected");

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
                    sendMessages(2,ack.getLogPosition());

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