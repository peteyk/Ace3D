/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 *
 * @author gevirl
 */
public interface NucleusFile {
    public void open()throws Exception ;
    public void save()throws Exception ;
    public void saveAs(File file) throws Exception ;
    public Set<Nucleus> getNuclei(int time);
    public File getFile();
    public List<Nucleus> linkedForward(Nucleus nuc);
    public Nucleus sister(Nucleus nuc);
}
