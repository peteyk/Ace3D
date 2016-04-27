/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import ij.ImageJ;
import ij.plugin.PlugIn;
import ij.process.LUT;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import org.rhwlab.dispim.DataSetDesc;
import org.rhwlab.dispim.Hdf5ImageSource;
import org.rhwlab.dispim.ImageJHyperstackSource;
import org.rhwlab.dispim.ImageSource;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.Ace3DNucleusFile;
import org.rhwlab.dispim.TifDirectoryImageSource;
import org.rhwlab.dispim.nucleus.NucleusFile;
import org.rhwlab.dispim.nucleus.StarryNiteNucleusFile;

/**
 *
 * @author gevirl
 */
public class Ace3D_Frame extends JFrame implements PlugIn {
    public  Ace3D_Frame()  {
        nucFile = new Ace3DNucleusFile();
        panel = new SynchronizedMultipleSlicePanel(3);
        this.add(panel);
        buildMenu();
        this.pack();
        buildChooser();
        String homeDir = System.getProperty("user.home");
        File propFile = new File(homeDir,"Ace3D_Frame.properties");
        props.open(propFile.getPath());
        this.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing(WindowEvent e){
                try {
                    close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public void close() throws Exception {
        props.save();
        System.exit(0);
    }
    @Override
    public void run(String string) {
        this.setSize(1800,600);
        this.setVisible(true);
//        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }
    

    final void buildMenu(){
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        
        JMenuItem hyper = new JMenuItem("Import ImageJ Hyperstack");
        hyper.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                source = new ImageJHyperstackSource();
                source.open();
                initToSource();
                int fhsduis=0;
            }
        });
        fileMenu.add(hyper);
        
        JMenuItem open = new JMenuItem("Open Images from HDF5");
        open.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String hdf5File = props.getProperty("HDF5ImageFile");
                if (hdf5File != null){
                    ij.io.OpenDialog.setDefaultDirectory(new File(hdf5File).getParent());
                }
                source = new Hdf5ImageSource();
                if (source.open()){
                    props.setProperty("HDF5ImageFile", source.getFile());
                    initToSource();
                }
            }
        });
        fileMenu.add(open);
        
        JMenuItem openTif = new JMenuItem("Open Images from TIF Directory");
        openTif.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                source = new TifDirectoryImageSource("/net/waterston/vol9/diSPIM/20151118_nhr-25_XIL0141/CroppedReslicedBGSubtract488");
                source.open();
                initToSource();
            }
        });
        fileMenu.add(openTif);        
        fileMenu.addSeparator();
        
        JMenuItem nucOpen = new JMenuItem("Open Nuclei File");
        nucOpen.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    openNucFile();
                }catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
        fileMenu.add(nucOpen);
        
        JMenuItem snOpen = new JMenuItem("Open Starry Nite Nuclei File");
        snOpen.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    openStarryNiteNucFile();
                }catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
        fileMenu.add(snOpen);        
        fileMenu.addSeparator();

        JMenuItem nucSave = new JMenuItem("Save Nuclei File");
        nucSave.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (nucFile.getFile() != null && !(nucFile instanceof StarryNiteNucleusFile)){
                        nucFile.save();
                    }else {
                        saveAsNucFile();
                    }
                }catch (Exception exc){
                    exc.printStackTrace();
                }                
            }
        });
        fileMenu.add(nucSave);

        JMenuItem nucSaveAs = new JMenuItem("Save Nuclei File As");
        nucSaveAs.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    saveAsNucFile();
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
        fileMenu.add(nucSaveAs);  
        fileMenu.addSeparator();

        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    close();
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
        fileMenu.add(exit);
        
        dataset = new JMenu("DataSet");
        menuBar.add(dataset);
        
        JMenu imageMenu = new JMenu("Image");
        menuBar.add(imageMenu);
        contrast = new JMenu("Contrast");
        imageMenu.add(contrast); 
        lutMenu = new JMenu("LUT");
        imageMenu.add(lutMenu);
        colorMenu = new JMenu("Color");
        imageMenu.add(colorMenu);
        
        JMenu navigate = new JMenu("Navigate");
        JMenuItem toTime = new JMenuItem("To Time Point");
        toTime.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                moveToTime();
            }
        });
        navigate.add(toTime);
        menuBar.add(navigate);
        
        JMenu view = new JMenu("Annotations");
        menuBar.add(view);
        
        segmentedNuclei = new JCheckBoxMenuItem("Nuclei indicator");
        segmentedNuclei.setSelected(true);
        segmentedNuclei.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.repaint();
            }
        });
        view.add(segmentedNuclei);
        
        sisters = new JCheckBoxMenuItem("Sister indicator");
        sisters.setSelected(true);
        sisters.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.repaint();
            }
        });        
        view.add(sisters);

        locationIndicator = new JCheckBoxMenuItem("Location indicator");
        locationIndicator.setSelected(true);
        locationIndicator.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.repaint();
            }
        });        
        view.add(locationIndicator);
        
        selectedLabeled = new JCheckBoxMenuItem("Label the Selected Nucleus");
        selectedLabeled.setSelected(false);
        selectedLabeled.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.repaint();
            }
        });        
        view.add(selectedLabeled);
        
        nucleiLabeled = new JCheckBoxMenuItem("Label All the Nuclei");
        nucleiLabeled.setSelected(false);
        nucleiLabeled.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.repaint();
            }
        });        
        view.add(nucleiLabeled);
        


        this.setJMenuBar(menuBar);        
    }
   
    private void initToSource(){
                // set up the dataset properties map
        dataSetProperties.clear();
        Iterator<DataSetDesc> iter = source.getDataSets().iterator();
        while (iter.hasNext()){
            dataSetProperties.put(iter.next().getName(),new DataSetProperties());
        }                
        buildDataSetMenu();
        buildContrastMenu();
        buildColorMenu();                
        imagedEmbryo = new ImagedEmbryo(source);
        imagedEmbryo.setNucleusFile(nucFile);
        panel.setEmbryo(imagedEmbryo);        
    }
    private void moveToTime(){
        boolean valid = false;
        while (!valid) {
            String timeStr = JOptionPane.showInputDialog("Enter the time:");
            if (timeStr == null) return;
            try {
                int time = Integer.valueOf(timeStr);
                panel.changeTime(time);
                valid = true;
            } catch (Exception exc){
                JOptionPane.showMessageDialog(this,"Not a valid time entry");
            }
        }
    }
    private void openNucFile()throws Exception {
        if (nucChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){
            nucFile = new Ace3DNucleusFile(nucChooser.getSelectedFile());
            if (imagedEmbryo != null){
                imagedEmbryo.setNucleusFile(nucFile);
            }
        }
    }
    
    private void openStarryNiteNucFile()throws Exception {
        
//        nucFile = new StarryNiteNucleusFile("/nfs/waterston/pete/Segmentation/dispim_sample_data/matlab_output/CroppedReslicedBGSubtract488/Decon_emb1.zip");
        String starry = props.getProperty("StarryNite");
        if (starry != null){
            nucChooser.setSelectedFile(new File(starry));
        }
        if (nucChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){
            nucFile = new StarryNiteNucleusFile(nucChooser.getSelectedFile().getPath());
            props.setProperty("StarryNite",nucFile.getFile().getPath());
            if (imagedEmbryo != null){
                long[] coords = imagedEmbryo.getMinCoordinate();
                ((StarryNiteNucleusFile)nucFile).adjustCoordinates((int)coords[0],(int)coords[1],(int)coords[2]);
                imagedEmbryo.setNucleusFile(nucFile);
            }
        }        
    }
    
    private void saveAsNucFile()throws Exception {
        buildChooser();
        if (nucFile != null){
            nucChooser.setSelectedFile(nucFile.getFile().getParentFile());
        }
        if (nucChooser.showSaveDialog(panel) == JFileChooser.APPROVE_OPTION){
            File f = nucChooser.getSelectedFile();
            if (f.getPath().equals(nucFile.getFile().getPath())){
                if (nucFile instanceof StarryNiteNucleusFile){
                    JOptionPane.showMessageDialog(rootPane,"Cannot overwrite the StarryNite file");
                } else {
                    if (JOptionPane.showConfirmDialog(rootPane,"Replace the original nucleus file?")==JOptionPane.OK_OPTION){
                        nucFile.saveAs(f);
                    }
                }
            } else {
                nucFile.saveAs(nucChooser.getSelectedFile());
            }
        }
    }
    
    private void buildChooser(){
        if (nucChooser == null){
            if (source != null){
                File xml = new File(source.getFile());
                nucChooser = new JFileChooser(xml.getParentFile());
            } else {
                nucChooser = new JFileChooser();
            }
        }        
    }
    private void buildDataSetMenu(){
        dataset.removeAll();
        ButtonGroup buttonGroup = new ButtonGroup();
        Iterator<DataSetDesc> dataSetIter = source.getDataSets().iterator();
        
        datasetChoices = new JCheckBoxMenuItem[source.getDataSets().size()];
        int i=0;
        while(dataSetIter.hasNext()){
            datasetChoices[i] = new JCheckBoxMenuItem(dataSetIter.next().getName());
            datasetChoices[i].addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    panel.showCurrentImage();
                }
            });
            dataset.add(datasetChoices[i]);
//            buttonGroup.add(datasetChoices[i]);
            ++i;
        }
        datasetChoices[0].setState(true);
    }
    private void buildContrastMenu(){
        contrast.removeAll();
        Iterator<DataSetDesc> dataSetIter = source.getDataSets().iterator();
        contrastDialogs.clear();
        while(dataSetIter.hasNext()){
            String datasetName = dataSetIter.next().getName();
            ContrastDialog cd = new ContrastDialog(this,dataSetProperties.get(datasetName),String.format("Contrast for Dataset: %s",datasetName),0,Short.MAX_VALUE);
            contrastDialogs.put(datasetName,cd);
            JMenuItem channelContrast = new JMenuItem(datasetName);
            contrast.add(channelContrast);
            channelContrast.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    cd.setVisible(true);
                }
            });
        }
    }
    private void buildLutMenu(){
        lutMenu.removeAll();
        Iterator<DataSetDesc> dataSetIter = source.getDataSets().iterator();
        while(dataSetIter.hasNext()){
            ButtonGroup buttonGroup = new ButtonGroup();
            String dataset = dataSetIter.next().getName();
            JMenu dataSetMenu = new JMenu(dataset);
            lutMenu.add(dataSetMenu);
            Set<String> lutNames = lookUpTables.getLutNames();
            JCheckBoxMenuItem[] lutItems = new JCheckBoxMenuItem[lutNames.size()];
            int i=0;
            for (String lutName : lutNames){
                
                lutItems[i] = new JCheckBoxMenuItem(lutName);
                lutItems[i].setName(dataset);
                lutItems[i].addActionListener(new ActionListener(){
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        JCheckBoxMenuItem checked = (JCheckBoxMenuItem)e.getSource();
                        if (checked.getState()){

                            dataSetLuts.put(checked.getName(),lookUpTables.getLUT(checked.getText()));
                            panel.showCurrentImage();
                        }
                    }
                });
                dataSetMenu.add(lutItems[i]);
                buttonGroup.add(lutItems[i]);
                if (lutName.equalsIgnoreCase("Gray")){
                    lutItems[i].setSelected(true);
                }
                ++i;
            }
            dataSetLuts.put(dataset,lookUpTables.getLUT("Gray"));
        }
    }
    private void buildColorMenu(){
        colorMenu.removeAll();
        colorChoices = new JMenuItem[source.getDataSets().size()];
        int i=0;
        Iterator<DataSetDesc> dataSetIter = source.getDataSets().iterator();
        while(dataSetIter.hasNext()){
            String datasetName = dataSetIter.next().getName();
            colorChoices[i] = new JMenuItem(datasetName);
            colorChoices[i].addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    JMenuItem item = (JMenuItem)e.getSource();
                    DataSetProperties props = dataSetProperties.get(item.getText());
                    Color choosen = JColorChooser.showDialog(Ace3D_Frame.this,item.getText(),props.color);
                    if (choosen!=null){
                        props.color = choosen;
                        item.setForeground(choosen);
                    }
                }
            });
            colorMenu.add(colorChoices[i]);
            ++i;
        }        
    }
    public void refreshImage(){
        panel.repaint();
    }
    static public boolean labelNuclei(){
        return nucleiLabeled.getState();
    }
    static public boolean labelSelectedNucleus(){
        return selectedLabeled.getState();
    }
    static public boolean sistersIndicated(){
        return sisters.getState();
    }
    static public boolean nucleiIndicated(){
        return segmentedNuclei.getState();
    }
    static public boolean locationIndicated(){
        return locationIndicator.getState();
    }

    static public List<String> datasetsSelected(){
        ArrayList<String> ret = new ArrayList<>();
        for (JCheckBoxMenuItem item : datasetChoices){
            if (item.isSelected()){
                ret.add(item.getText());
            }
        }
        return ret;
    }
    static public DataSetProperties getProperties(String dataSet){
        return dataSetProperties.get(dataSet);
    }
    
    static public LUT getLUT(String dataSet){
        return dataSetLuts.get(dataSet);
    }
    Properties props = new Properties();
    JMenu dataset;
    JMenu contrast;
    JMenu lutMenu;
    JMenu colorMenu;
    ImageSource source;
    NucleusFile nucFile;
    ImagedEmbryo imagedEmbryo;
    SynchronizedMultipleSlicePanel panel;
    JFileChooser nucChooser;
    JFileChooser imageChooser;
    TreeMap<String,ContrastDialog> contrastDialogs = new TreeMap<>();
    LookUpTables lookUpTables = new LookUpTables();
    
    static JCheckBoxMenuItem segmentedNuclei;
    static JCheckBoxMenuItem sisters;
    static JCheckBoxMenuItem locationIndicator;
    static JCheckBoxMenuItem nucleiLabeled;
    static JCheckBoxMenuItem selectedLabeled;
    static JCheckBoxMenuItem[] datasetChoices;
    static JMenuItem[] colorChoices;
    static TreeMap<String,LUT> dataSetLuts = new TreeMap<>();
    
    static TreeMap<String,DataSetProperties> dataSetProperties = new TreeMap<>();
    
    static public void main(String[] args) {
        EventQueue.invokeLater(new Runnable(){
            @Override
            public void run() {
                new ImageJ();
                try {
                    Ace3D_Frame   frame = new Ace3D_Frame();
                    
                    frame.run(null);
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
    }     

}
