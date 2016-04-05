/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.io.PrintStream;
import java.util.Date;
import java.util.Random;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import org.rhwlab.starrynite.TimePointNucleus;

/**
 *
 * @author gevirl
 */
public class Nucleus {
    public Nucleus(TimePointNucleus data){
        this.time = data.getTime();
        this.name = data.getName();
        this.x = data.getX();
        this.y = data.getY();
        this.z = (long)data.getZ();
        this.radius = data.getRadius();
    }
    public Nucleus(String[] headings,String[] data){
        for (int i=0 ; i<headings.length ; ++i){
            if (headings[i].equalsIgnoreCase("Time")){
                time = Integer.valueOf(data[i]);
            } else if (headings[i].equalsIgnoreCase("Name")){
                name = data[i];
            } else if (headings[i].equalsIgnoreCase("X")){
                x = Long.valueOf(data[i]);
            } else if (headings[i].equalsIgnoreCase("Y")){
                y = Long.valueOf(data[i]);
            } else if (headings[i].equalsIgnoreCase("Z")){
                z = Long.valueOf(data[i]);
            } else if (headings[i].equalsIgnoreCase("Radius")){
                radius = Double.valueOf(data[i]);
            }
        }
    }
    public Nucleus (int time,long[] center,double radius){
        this(time,randomName(),center,radius);
    }
    public Nucleus (int time,String name,long[] center,double radius){
        this.time = time;
        this.name = name;
        this.x = center[0];
        this.y = center[1];
        this.z = center[2];
        this.radius = radius;
    } 
    static public String randomName(){
        if (rnd == null){
            rnd = new Random();
        }
        return String.format("Nuc_%d_%d",(new Date()).getTime(),rnd.nextInt());
    }
    static public void saveHeadings(PrintStream stream){
        stream.println("Time,Name,X,Y,Z,Radius,Child1,Child2");
    }
    public void saveNucleus(PrintStream stream){
//        stream.printf("%d,%s,%d,%d,%d,%f,%s,%s\n",time,getName(),x,y,z,radius,getChild1(),getChild2());
    }
    public int getTime(){
        return this.time;
    }
    public double getRadius(){
        return radius;
    }
    public long[] getCenter(){
        long[] center = new long[3];
        center[0] = x;
        center[1] = y;
        center[2] = z;
        return center;
    }
    public String getName(){
        if (name == null){
            return this.toString();
        }
        return name;
    }

    public void setSelected(boolean s){
        this.selected = s;
    }
    public boolean getSelected(){
        return this.selected;
    }
    public double distanceSqaured(long[] p){
        double d = 0.0;
        long[] c = this.getCenter();
        for (int i=0 ; i<p.length ; ++i){
            long delta = p[i]-c[i];
            d = d + delta*delta;
        }
        return d;
    }
    public JsonObjectBuilder asJson(){
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("Name", name);
        builder.add("Time", time);
        builder.add("X", x);
        builder.add("Y", y);
        builder.add("Z", z);
        builder.add("Radius", radius);
        if (cell != null){
            builder.add("Cell", cell.getName());
        }
        return builder;
    }
    int time;
    String name;
    long x;
    long y;
    long z;
    double radius;
    Cell cell;  // the cell to which this nucleus belongs - can be null
    
    boolean selected=false;
    static Random rnd;
}
