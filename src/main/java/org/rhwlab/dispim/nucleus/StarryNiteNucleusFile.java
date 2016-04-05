/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.io.File;
import org.rhwlab.starrynite.SeriesNuclei;
import org.rhwlab.starrynite.TimePointNuclei;
import org.rhwlab.starrynite.TimePointNucleus;

/**
 *
 * @author gevirl
 */
public class StarryNiteNucleusFile extends Ace3DNucleusFile {
    public StarryNiteNucleusFile(String fn){
        this.fileName = fn;
        
    }
    public void open()throws Exception {
        SeriesNuclei seriesNucs = new SeriesNuclei("");
        seriesNucs.readZipFile(new File(fileName));
        for (int t=1 ; t<seriesNucs.getMaxTime() ; ++t){
            TimePointNuclei nucs = seriesNucs.getNucleiAtTime(t);
            if (nucs != null){
                for (TimePointNucleus nuc : nucs.getNuclei()){
                    Nucleus nucleus = new Nucleus(nuc);
                    this.putNucleusIntoMaps(nucleus);
                }
            }
        }
    }
    String fileName;
}
