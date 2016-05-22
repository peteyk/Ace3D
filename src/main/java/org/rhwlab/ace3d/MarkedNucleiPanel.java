/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.awt.BorderLayout;
import java.util.Set;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.Nucleus;

/**
 *
 * @author gevirl
 */
public class MarkedNucleiPanel extends JPanel implements InvalidationListener {
    public MarkedNucleiPanel(){
        this.setLayout(new BorderLayout());
        this.add(new JLabel("Marked Nuclei at Next Time"),BorderLayout.NORTH);
        list = new JList();
        JScrollPane scroll = new JScrollPane(list);
        this.add(scroll,BorderLayout.CENTER);
    }

    @Override
    public void invalidated(Observable observable) {
        if (observable instanceof ImagedEmbryo){
            ImagedEmbryo embryo = (ImagedEmbryo)observable;
            Nucleus selected = embryo.selectedNucleus();
            if (selected != null){
                Set<Nucleus> marked = embryo.getMarkedNuclei(selected.getTime()+1);
                DefaultListModel model = new DefaultListModel();
                for (Nucleus nuc : marked){
                    model.addElement(nuc.getName());
                }
                list.setModel(model);
                this.repaint();
            }
        }
    }
    JList list;
}
