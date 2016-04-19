/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.starrynite;
/**
 *
 * @author gevirl
 */

// this wraps a Starry Night Nucleus object and encapsulates
// the concept of a single nucleus at a single time point
public class TimePointNucleus {

    /**
     * constructor used when nuclei are read in from files
     * using the new file format
     * @param sa String [] with parsed entries from the line in
     * the file
     */
    public TimePointNucleus(String [] sa,TimePointNuclei nuclei) {
        brothers = nuclei;
        index = Integer.parseInt(sa[INDEX]);
        x = Integer.parseInt(sa[X]);
        y = Integer.parseInt(sa[Y]);
        z = Float.parseFloat(sa[Z]);
        identity = sa[IDENTITY];
        size = Integer.parseInt(sa[SIZE]);
        weight = Integer.parseInt(sa[WT]);
        // try..catch works around series without red data
        int i = 0;
        try {
            for (i = RWT; i < RWCORR4 + 1; i++) { 
                if (sa.length > i) {
                    if (sa[i].length() > 0) {
                        switch(i) {
                            case RWT:
                                rweight = Integer.parseInt(sa[RWT]);
                                break;
                            case RSUM:
                                rsum = Integer.parseInt(sa[RSUM]);
                                break;
                            case RCOUNT:
                                rcount = Integer.parseInt(sa[RCOUNT]);
                                break;
                            case ASSIGNEDID:
                                assignedID = sa[ASSIGNEDID];
                                break;
                            case RWRAW:
                                rwraw = Integer.parseInt(sa[RWRAW]);
                                break;
                            case RWCORR1:
                                rwcorr1 = Integer.parseInt(sa[RWCORR1]);
                                break;
                            case RWCORR2:
                                rwcorr2 = Integer.parseInt(sa[RWCORR2]);
                                break;
                            case RWCORR3:
                                rwcorr3 = Integer.parseInt(sa[RWCORR3]);
                                break;
                            case RWCORR4:
                                rwcorr4 = Integer.parseInt(sa[RWCORR4]);
                                break;
                        }
                    }
                }
            }
        } catch(Exception e) {
            //System.out.println("Nucleus constructor exception: " + i);
            
        }
        status = -1;
        int xstat = Integer.parseInt(sa[STATUS]);
        if (xstat > 0) status = xstat;

        if (sa[PRED].equals(NILL) 
            || Integer.parseInt(sa[PRED]) == -1
           ) predecessor = NILLI;
        else predecessor = Integer.parseInt(sa[PRED]);
        if (sa[SUCC1].equals(NILL)) successor1 = NILLI;
        else successor1 = Integer.parseInt(sa[SUCC1]);
        if (sa[SUCC2] == null) successor2 = NILLI;
        else successor2 = Integer.parseInt(sa[SUCC2]);
        
        //assignedID = "XXXX";
    }
    
    // returns the successors of this TimePointNucleus
    public TimePointNucleus[] getSuccessors(){
        TimePointNucleus[] ret = null;
        if (successor1 == -1 || successor1==0){
            ret = new TimePointNucleus[0];
        } else  if(successor2 ==-1) {
            ret = new TimePointNucleus[1];
            ret[0] = this.getSuccessor(successor1);
        } else {
            ret = new TimePointNucleus[2];
            ret[0] = this.getSuccessor(successor1);
            ret[1] = this.getSuccessor(successor2);
        }
        return ret;
    }  
    private TimePointNucleus getSuccessor(int succ){
        TimePointNuclei nextTimeNucs = brothers.getSeries().getNucleiAtTime(this.getTime()+1);
        return nextTimeNucs.getNucleus(succ);
    }
    public TimePointNucleus getPredecessor(){
        if (this.predecessor==-1){
            return null;
        }
        TimePointNuclei priorTimeNucs = brothers.getSeries().getNucleiAtTime(this.getTime()-1);
        return priorTimeNucs.getNucleus(this.predecessor);
    }
    
    public int getTime(){
        return brothers.time;  
    }
    public String getName(){
        return identity;
    }
    public int getExpression(String type) {
        return getCorrectedRed(type);
    }
    public int getCorrectedRed(String type) {
        if (type.equals("zblot")){

 		double midPlane = 15;
		double zcorSlope = 0.0331;
                int blot = -rwcorr3+rwraw;  // "blot" is  the blot corrected expression
		
                // add 3% for each higher plane number above the mid plane
                // subtract for plane numbers less than mid plane
                double deltazb = Math.abs(blot) * (z - midPlane)* zcorSlope; 
		double zb = blot + deltazb;
		
		int zblot = (int)Math.round(zb);  // this is a Z corrected blot expression
                return zblot;  // the return is the actual zblot expression, not a correction
        } else {
            int choice = getRedChoiceNumber(type);
            return computeRed(choice);
        }
    }  
    private int getRedChoiceNumber(String type) {
        int i = 0;
        for (i=0; i < REDCHOICE.length; i++) {
            if (type.equals(REDCHOICE[i])) break;
        }
        
        return i;
    }  
    private int computeRed(int k) {
        int red = rwraw;
        switch(k) {
        case 1:
            red -= rwcorr1; //global
            break;
        case 2:
            red -= rwcorr2; //local
            break;
        case 3: 
            red -= rwcorr3; //blot
            break;
        case 4:
            red -= rwcorr4; //cross
            break;
        }
        return red;
    } 
    
    public int getMeanExpression() {
        return rweight;
    }
    public int getVolume() {
        return rcount;
    }
    public int getTotalExpression() {
        return rsum;
    }
    public void dump(int time,java.io.PrintWriter writer){
        writer.print(identity);
        writer.print(",");
        writer.print(time);
        writer.print(",");
        writer.print(x);
        writer.print(",");
        writer.print(y);
        writer.print(",");
        writer.print(z);
        writer.print(",");
        writer.print(size);
        writer.print(",");
        writer.print(weight);
        writer.print(",");
        writer.print(rweight);
        writer.print(",");
        writer.print(rsum);
        writer.print(",");
        writer.print(rcount);
        writer.print(",");
        writer.print(rwraw);
        writer.print(",");
        writer.print(rwcorr1);
        writer.print(",");
        writer.print(rwcorr2);
        writer.print(",");
        writer.print(rwcorr3);
        writer.print(",");
        writer.print(rwcorr4);
        writer.print(","); 
        writer.println(getExpression("zblot"));
    }
    public String getSeriesID(){
        return brothers.series.seriesID;
    }
    public int getX(){
        return x;
    }
    public int getY(){
        return y;
    }
    public float getZ(){
        return z;
    }
    public int getRadius(){
        return (int)(size/2.0);
    }
    TimePointNuclei brothers;
    public String identity;
    int index;
    int status;
    int predecessor;
    int successor1;
    int successor2;
    int x;
    int y;
    float z;
    int size;    // radius of nulceus 
    int weight;
    int rweight;    // same as rwraw (LG)
    int rsum;       // sum of all pixels weighed by their intensity in this nucleus at this z(LG)
    int rcount;     // number of pixels in this nucleus at this z (LG)
    String assignedID;
    String hashKey;
    char id_tag;
    int  rwraw;     // = 1000*rsum/rcount
    int  rwcorr1; // a global background correction
    int  rwcorr2; // a local background correction
    int  rwcorr3; // local background correction with cookies
    int  rwcorr4; // crosstalk correction
    
    // location of things in the new file format
    final static int
    INDEX = 0
   ,X = 5
   ,Y = 6
   ,Z = 7
   ,IDENTITY = 9
   ,SIZE = 8
   ,WT = 10
   ,STATUS = 1
   ,PRED = 2
   ,SUCC1 = 3
   ,SUCC2 = 4
   ,RWT = 11
   ,RSUM = 12
   ,RCOUNT = 13
   ,ASSIGNEDID = 14
   ,RWRAW = 15
   ,RWCORR1 = 16
   ,RWCORR2 = 17
   ,RWCORR3 = 18
   ,RWCORR4 = 19
   ;    
   //*/
    final private static String
         NILL = "nill"
        ;
    
    final  static int 
         NILLI = -1
        ;  
    static final String [] REDCHOICE = {
        "none", "global", "local", "blot", "cross"
    };    
}