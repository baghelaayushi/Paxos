package roles;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import helpers.ValuePos;

import messaging.helpers.*;
import helpers.Site;
import messaging.MessagingClient;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class Proposer {

    static Proposer instance = null;

    Site site = null;
    String latestProposalCombination = "";
    HashMap<String, Site> siteHashMap = null;
    String currentValue = null;
    static HashSet<Integer> approvalFrom = new HashSet<Integer>();

    static HashMap<ValuePos, List<Integer>> approvalMap = new HashMap<>();

    static HashSet<Integer> valueLearned = new HashSet<Integer>();
    boolean acceptSent = false;
    static Boolean wonLastRound = false;
    static int maxRecvdAckNum = -1;

    int[] proposalNumbers = new int[1000];
    String[] proposalCombinations = new String[1000];
    String[] proposalValues = new String[1000];


    public Proposer(Site siteInformation, List<String> log, HashMap<String, Site> siteMap){

        //TODO:System could have crashed, check that.
        this.site = siteInformation;
        this.siteHashMap = siteMap;
        proposalNumbers = new int[1000];
    }
    public static Proposer getInstance(Site siteInformation, List<String> log, HashMap<String, Site> siteMap){
        if (instance == null){
            instance = new Proposer(siteInformation, log, siteMap);
        }
        return instance;
    }

    private String getProposalNumber(int logPosition){

//        maxProposalNumber++;
        proposalNumbers[logPosition]++;
        saveState();
        String proposalNumber = proposalNumbers[logPosition] +"-"+site.getSiteNumber();
        latestProposalCombination = proposalNumber;
        proposalCombinations[logPosition] = proposalNumber;
        return proposalNumber;
    }

    public static void saveState(){
        try(FileWriter fw = new FileWriter("saved_maxProposal.json")){
            Gson gson = new Gson();
            JsonArray arr = new JsonArray();
            for(int s:instance.proposalNumbers){
                arr.add(s);
            }
            fw.append(gson.toJson(arr));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void getState(){
        try {
            //convert the json string back to object
            File f = new File("saved_maxProposal.json");
            if(!f.exists())
                return;

            BufferedReader backup = new BufferedReader(new FileReader("saved_maxProposal.json"));
            JsonParser parser = new JsonParser();
            JsonArray parsed = parser.parse(backup).getAsJsonArray();
            Gson gson = new Gson();
            if(!parsed.isJsonNull()) {
                int i = 0;
                for (JsonElement ob : parsed) {
                    //System.out.println(ob);
                    if(ob != null && ob.toString().equals("null"))
                        instance.proposalNumbers[i] = Integer.parseInt(ob.getAsString());
                    i++;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void sendMessages(int stage,int position){

        String proposalNumber = null;

        if(stage == 1){
            proposalNumber = getProposalNumber(position);
            System.err.println("% sending prepare("+proposalNumber+") to all sites for log position " + position);
        }

        if(stage == 2){
            System.err.println("% 2- sending accept("+proposalNumbers[position]+","+proposalValues[position]+") to all sites");
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
                        MessagingClient mClient = new MessagingClient(destinationAddress, site.getRandomPort());

                        mClient.send(message, port);
                        mClient.close();
                    }

                }

                // to send an accept message to acceptors
                if(stage == 2) {

                    AcceptMessage message = new AcceptMessage(position, proposalCombinations[position], proposalValues[position], site.getSiteNumber());
                    if(client.getValue().getSiteNumber() == site.getSiteNumber()){
                        acceptorInstance.processAcceptRequest(message);
                    }else {
                        MessagingClient mClient = new MessagingClient(destinationAddress, site.getRandomPort());
                        mClient.send(message, port);
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

        HashMap<Integer,Integer> flights = Learner.getInstance().getFlights();
        String requiredFlights[] = reservation.split(" ")[2].split(",");
        for(String s: requiredFlights) {
            if (flights.get(Integer.parseInt(s)) <= 0){
                System.err.println("Can't place reservation");
            return;
        }
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(new Tasks());
//        currentValue = reservation;
        Learner learner = Learner.getInstance();

        approvalFrom = new HashSet<>();
        valueLearned = new HashSet<>();

        maxRecvdAckNum = -1;

        if(position ==0) {
            for (String s : learner.log) {
                if (s == null) {
                    break;
                }
                position++;
            }
        }

        acceptSent = false;

        proposalValues[position] = reservation;

        approvalMap.put(new ValuePos(reservation, position), new ArrayList<>());

        if(method.equals("reserve"))
            System.out.println("Reservation submitted for " + reservation.split(" ")[1]+".");
        else if(method.equals("cancel"))
            System.out.println("Reservation cancelled for " + reservation.split(" ")[1]+".");


        if(!wonLastRound) {
            sendMessages(1, position);
        }
        else{
            System.err.println("% executing an optimized paxos");
            getProposalNumber(position);
            sendMessages(2, position);
        }

        try{
            future.get(99, TimeUnit.MILLISECONDS);
        }catch (Exception e){

            future.cancel(true);
        }

        System.err.println("% Is there still a null?");
        if(Learner.getLog()[position] == null)
            reAttempt(executor, future, position);
        if(Learner.getLog()[position] == null)
            reAttempt(executor, future, position);
        if(Learner.getLog()[position] == null)
            reAttempt(executor, future, position);

        executor.shutdownNow();


    }

    private void reAttempt(ExecutorService executor, Future<String> future, int position) {
        try {
            future = executor.submit(new Tasks());
            future.get(99, TimeUnit.MILLISECONDS);
            System.err.println("% Reattempting");
            System.err.println(Learner.getLog()[position]);
            checkSetAndAttemptSend(position);
        }catch (Exception e1){
            future.cancel(true);
        }
    }

    private void checkSetAndAttemptSend(int position) throws Exception {

        if (!wonLastRound){
            System.err.println("%checkAndSet");
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

            proposalNumbers[message.getLogPosition()] += Integer.parseInt(message.getAccNum());
            saveState();
            initiateProposal(ack.getOriginalValue(),"",1);


        }else{
            //TODO: Add to the set
            PrepareAck prepareMessage = (PrepareAck)ack;


            approvalFrom.add(prepareMessage.getFrom());


            String proposed = prepareMessage.getAccNum();

            System.err.println("% received promise("+prepareMessage.getAccNum()+","+prepareMessage.getAccValue()+"" +
                    ") from site " + prepareMessage.getFrom());

            if(prepareMessage.getAccValue() != null){

                System.err.println("Received message is not null" + prepareMessage.getAccNum()+","+prepareMessage.getAccValue()+" maxRecv" +
                        maxRecvdAckNum);
                //Is this my first proposal?
                if(maxRecvdAckNum != -1){
                    if(Integer.parseInt(proposed) >= maxRecvdAckNum){
                        maxRecvdAckNum = Integer.parseInt(proposed);
                        System.err.println("Updating from "+
                                proposalValues[prepareMessage.getLogPosition()]+
                                "to" +
                                prepareMessage.getAccValue());
                        proposalValues[prepareMessage.getLogPosition()] = prepareMessage.getAccValue();

                        //Reset approvals

                    }
                }else{
                    //Yes it is
                    maxRecvdAckNum = Integer.parseInt(prepareMessage.getAccNum());
                    System.err.println("Updating from "+
                            proposalValues[prepareMessage.getLogPosition()]+
                            "to" +
                            prepareMessage.getAccValue());
                    proposalValues[prepareMessage.getLogPosition()] = prepareMessage.getAccValue();

                }
            }


            if(approvalFrom.size() > siteHashMap.size()/2 && !acceptSent){

                System.err.println("% received accept messages from a majority with" + proposalValues[ack.getLogPosition()]);
//                if(prepareMessage.getFrom() != site.getSiteNumber())
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