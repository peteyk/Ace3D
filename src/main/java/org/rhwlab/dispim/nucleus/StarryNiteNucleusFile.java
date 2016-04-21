/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.io.File;
import java.util.Set;
import org.rhwlab.starrynite.SeriesNuclei;
import org.rhwlab.starrynite.TimePointNuclei;
import org.rhwlab.starrynite.TimePointNucleus;

/**
 *
 * @author gevirl
 */
public class StarryNiteNucleusFile extends Ace3DNucleusFile {
    public StarryNiteNucleusFile(String fn)throws Exception {
        this.fileName = fn;
        open();
    }
    public void adjustCoordinates(int xMin,int yMin,int zMin){
        for (Set<Nucleus> nucSet : this.byTime.values()){
            for (Nucleus nuc : nucSet){
                nuc.x = nuc.x - xMin;
                nuc.y = nuc.y - yMin;
                nuc.z = nuc.z - zMin;
            }
        }
    }
    public void open()throws Exception {
        SeriesNuclei seriesNucs = new SeriesNuclei("");
        seriesNucs.readZipFile(new File(fileName));
        
        for (int t=1 ; t<seriesNucs.getMaxTime() ; ++t){
            TimePointNuclei nucs = seriesNucs.getNucleiAtTime(t);
            if (nucs != null){
                for (TimePointNucleus nuc : nucs.getNuclei()){
                    TimePointNucleus pred = nuc.getPredecessor();
                    if (pred == null){
                        Cell root = makeCell(nuc);
                        this.addRoot(root);
                    }
                }
            }
        }

        int asfhue=0;
    }
    public Cell makeCell(TimePointNucleus starting){
        Cell cell = new Cell(starting.getName());
        this.cellMap.put(cell.getName(), cell);
        TimePointNucleus tpn = starting;
        
        // add all the nuclei
        while (tpn != null){
            Nucleus nuc = new Nucleus(tpn);
            this.addNucleus(nuc);
            cell.addNucleus(nuc);
            TimePointNucleus[] succs = tpn.getSuccessors();
            switch(succs.length){
                case 0:
                    tpn = null;
                    break;
                case 1:
                    tpn = succs[0];
                    break;
                case 2:
                    Cell childCell1 = makeCell(succs[0]);
                    Cell childCell2 = makeCell(succs[1]);
                    cell.addChild(childCell1);
                    cell.addChild(childCell2);
                    tpn = null;
                    break;                     
            }
        }
        this.cellMap.put(cell.getName(), cell);
        return cell;
    }    
    String fileName;
}
