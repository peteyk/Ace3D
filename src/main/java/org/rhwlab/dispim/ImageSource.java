/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import java.util.Collection;
import java.util.List;
import org.jdom2.Element;


/**
 *
 * @author gevirl
 */
public interface ImageSource {
    public boolean open();
    public TimePointImage getImage(String datatset,int time);
    public int getTimes();
    public int getMinTime();
    public int getMaxTime();
    public String getFile();
    public Collection<DataSetDesc> getDataSets();
    public void setFirstTime(int minTime);
    public Element toXML();

}
