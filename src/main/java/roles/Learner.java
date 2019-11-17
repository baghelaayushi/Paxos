package roles;

import helpers.Site;

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
    List<String> log = new ArrayList<String>();
    Site site = null;

    public Learner(Site siteInformation, HashMap<String, Site> siteMap,HashMap<Integer,String> siteIDMap){
        this.site = siteInformation;
        this.siteHashMap = siteMap;
        this.siteIDMap = siteIDMap;
        this.logMap = new HashMap<>();
        accNum = null;
        accValue = null;
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

    public void Listen(LearnMessage message){
        String accNum = message.getAccNum();
        String accVal = message.getAccValue();
        int logPosition = message.getLogPosition();
        int maxSites = siteHashMap.size()/2+1;

        if(logMap.containsKey(logPosition)){
             if(logMap.get(logPosition).containsKey(accNum + '-' + accVal)){
                int count = logMap.get(logPosition).get(accNum + '-' + accVal);
                if(count+1>=maxSites){
                    log.add(logPosition,accVal);
                    System.out.println("Committing at position " + logPosition + " " + log.get(logPosition));

                }
                HashMap<String,Integer> temp = logMap.get(logPosition);
                temp.replace(accNum + '-' + accVal,count+1);
                logMap.replace(logPosition,temp);
            }
            else {
                logMap.get(logPosition).put(accNum + '-' + accVal,1);
            }
        }
        else {
            HashMap<String,Integer> temp = new HashMap<>();
            temp.put(accNum + '-' + accVal,1);
            logMap.put(logPosition,temp);
        }

    }

}
