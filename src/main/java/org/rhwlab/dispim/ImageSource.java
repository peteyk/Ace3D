/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;


/**
 *
 * @author gevirl
 */
public interface ImageSource {
    public TimePointImage getImage(int time);
    public int getTimes();
    public String getFile();
}
