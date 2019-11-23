package roles;

import com.google.gson.*;
import helpers.Site;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import messaging.helpers.*;

import java.util.HashMap;
import java.util.List;

public class Learner {
    static Learner learner = null;
    static String accNum;
    static String accValue;
    HashMap<String, Site> siteHashMap = null;
    HashMap<Integer,String> siteIDMap = null;
    HashMap<Integer,HashMap<String,Integer>> logMap  = null;
    static String[] log = new String[Integer.MAX_VALUE];
    List<Boolean> logCheck = new ArrayList<>();
    Site site = null;

    public Learner(Site siteInformation, HashMap<String, Site> siteMap,HashMap<Integer,String> siteIDMap){
        this.site = siteInformation;
        this.siteHashMap = siteMap;
        this.siteIDMap = siteIDMap;
        this.logMap = new HashMap<>();
        accNum = null;
        accValue = null;
        for (int i =0 ; i < 10; i++){
            logCheck.add(false);
        }
    }

    public static Learner getInstance(Site siteInformation, HashMap<String, Site> siteMap,HashMap<Integer,String> siteIDMap){
        if (learner == null){
            learner = new Learner(siteInformation, siteMap,siteIDMap);
        }
        return learner;
    }
    public static Learner getInstance(){
        return learner;
    }

    static void saveState(){

        try(FileWriter fw = new FileWriter("saved_log.json")){
            Gson gson = new Gson();
            JsonArray arr = new JsonArray();
            for(String s:log){
                arr.add(s);
            }
            fw.append(gson.toJson(arr));
        }
        catch (IOException e) {
            e.printStackTrace();
        }


    }
    static void getState(){
        try {
            //convert the json string back to object
            BufferedReader backup = new BufferedReader(new FileReader("saved_log.json"));
            JsonParser parser = new JsonParser();
            JsonArray parsed = parser.parse(backup).getAsJsonArray();
            Gson gson = new Gson();
            log = new String[Integer.MAX_VALUE];
            int  i =0;
            for(JsonElement ob: parsed){
                String s = ob.getAsString();
                log[i] = s;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void learner(LearnMessage message){

        String accNum = message.getAccNum();
        String accVal = message.getAccValue();

        int requestedLogPosition = message.getLogPosition();

        int siteQuorum = siteHashMap.size()/2+1;


        //Have we received anything for this log position?
        if(logMap.containsKey(requestedLogPosition)){

            //Have we received this same copy of accNum and accVal?
             if(logMap.get(requestedLogPosition).containsKey(accNum + '-' + accVal)){

                int count = logMap.get(requestedLogPosition).get(accNum + '-' + accVal);

                if(count+1>=siteQuorum && !logCheck.get(requestedLogPosition)){

                    System.out.println("commiting at position" + requestedLogPosition);
                    log[requestedLogPosition] = accVal;
                    saveState();
                    logCheck.add(requestedLogPosition,true);
                }

                HashMap<String,Integer> temp = logMap.get(requestedLogPosition);
                temp.replace(accNum + '-' + accVal,count+1);
                logMap.replace(requestedLogPosition,temp);
            }
            else {
                logMap.get(requestedLogPosition).put(accNum + '-' + accVal,1);
            }

        }
        else {
            HashMap<String,Integer> temp = new HashMap<>();
            temp.put(accNum + '-' + accVal,1);
            logMap.put(requestedLogPosition,temp);
        }

    }

    public static void viewLog(){
        for(String s:log)
            System.out.println(s);
    }

    public static  String[] getLog(){
        return log;
    }




}
