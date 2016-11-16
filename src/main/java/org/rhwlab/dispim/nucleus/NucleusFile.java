/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.io.File;
import java.util.List;;
import java.util.Set;

/**
 *
 * @author gevirl
 */
public interface NucleusFile extends javafx.beans.Observable{
    public void open()throws Exception ;
    public void save()throws Exception ;
    public void saveAs(File file) throws Exception ;
    public Set<Nucleus> getNuclei(int time);
    public File getFile();
    public Set<Nucleus> getRoots(int time);
    public List<Nucleus> linkedForward(Nucleus nuc);
    public Nucleus linkedBack(Nucleus nuc);
    public Nucleus sister(Nucleus nuc);
    public void addNucleus(Nucleus nuc);
    public Set<Integer> getAllTimes();
//    public Nucleus getNucleus(String name);
//    public void setSelected(int time,String name);
    public void setSelected(Nucleus nuc);
    public Nucleus getSelected();
    public Cell getCell(String name);
    public BHCTreeDirectory getTreeDirectory();
    public void addNuclei(BHCNucleusFile bhcToAdd,boolean curated);
    public void addSelectionOberver(javafx.beans.value.ChangeListener obs);
}
