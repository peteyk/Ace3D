/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import java.util.Collection;
import java.util.List;


/**
 *
 * @author gevirl
 */
public interface ImageSource {
    public TimePointImage getImage(String datatset,int time);
    public int getTimes();
    public String getFile();
    public Collection<DataSetDesc> getDataSets();

}
