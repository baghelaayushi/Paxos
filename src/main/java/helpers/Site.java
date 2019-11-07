package helpers;

public class Site {
    private String ipAddress;
    private int uDPStartAddress;
    private int uDPEndAddress;
    private int SiteNumber;

    public Site(String ipAddress, String uDPStartAddress, String uDPEndAddress, int SiteNumber){
        this.ipAddress = ipAddress;
        this.uDPStartAddress = Integer.parseInt(uDPStartAddress);
        this.uDPEndAddress = Integer.parseInt(uDPEndAddress);
        this.SiteNumber = SiteNumber;
    }

    public String getIpAddress(){
        return this.ipAddress;
    }

    public int getRandomPort(){
        return uDPStartAddress;
//        return (int) (Math.random() * (uDPEndAddress - uDPStartAddress)) + uDPStartAddress;
    }

    public int getSiteNumber(){
        return this.SiteNumber;
    }
}
