/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import ij.ImageJ;
import ij.macro.Interpreter;
import ij.plugin.PlugIn;
import ij.process.LUT;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.rhwlab.BHC.BHCTree;
import org.rhwlab.ace3d.dialogs.BHCTreeCutDialog;
import org.rhwlab.ace3d.dialogs.PanelDisplay;
import org.rhwlab.dispim.DataSetDesc;
import org.rhwlab.dispim.ImageJHyperstackSource;
import org.rhwlab.dispim.ImageSource;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.TifDirectoryImageSource;
import org.rhwlab.dispim.TimePointImage;
import org.rhwlab.dispim.nucleus.BHCTreeDirectory;
import org.rhwlab.dispim.nucleus.LinkedNucleusFile;
import org.rhwlab.dispim.nucleus.Nucleus;
import org.rhwlab.dispim.nucleus.NucleusFile;

/**
 *
 * @author gevirl
 */
public class Ace3D_Frame extends JFrame implements PlugIn,ChangeListener  {
    public  Ace3D_Frame()  {
        
        imagedEmbryo = new ImagedEmbryo();
        TimePointImage.setEmbryo(imagedEmbryo);
        
        contrastDialog = new DataSetsDialog(this,0,Short.MAX_VALUE);
        contrastDialog.setVisible(true);
        
        panel = new SynchronizedMultipleSlicePanel(3);
        panel.setEmbryo(imagedEmbryo);
//        imagedEmbryo.addListener(panel);
        this.add(panel);
        
        navFrame = new Navigation_Frame(imagedEmbryo,panel);       
        imagedEmbryo.addListener(navFrame);
        
        selectedNucFrame = new SelectedNucleusFrame(this,imagedEmbryo);
//        selectedNucFrame.setVisible(true);        
        buildMenu();
        this.pack();
   
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
        imagedEmbryo.getNucleusFile().addListener(navFrame);
        imagedEmbryo.getNucleusFile().addSelectionOberver(selectedNucFrame);
        imagedEmbryo.getNucleusFile().addSelectionOberver(panel); 
        
        viPlot = new VolumeIntensityPlot(imagedEmbryo);
        viDialog = new PanelDisplay(viPlot);
    }

    public void close() throws Exception {
        props.save();
        System.exit(0);
    }
    @Override
    public void run(String string) {
        this.setSize(1800,600);
        this.setVisible(true);
        navFrame.run(null);
        this.selectedNucFrame.run(null);
    }
    

    final void buildMenu(){
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        
        JMenuItem virtStack = new JMenuItem("Open Lineaging TIFF Virtual Stack");
        virtStack.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String vsProp = props.getProperty("VirtualStack");
                if (vsProp != null){
                    nucChooser.setSelectedFile(new File(vsProp));
                } 
                if (nucChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){    
                    File sel = nucChooser.getSelectedFile();
                    String timeStr = setMinTime();
                    if (timeStr != null){
                        ImageJHyperstackSource source = new ImageJHyperstackSource(sel,Integer.valueOf(timeStr),"Lineaging",imagedEmbryo); 
                        panel.setTimeRange(Math.max(source.getMinTime(),panel.getMinTime())
                            ,Math.min(source.getMaxTime(),panel.getMaxTime()) );  
                        imagedEmbryo.notifyListeners();
                        props.setProperty("VirtualStack",sel.getPath());
                    }
                }                
            }
        });
        fileMenu.add(virtStack);
        
        JMenuItem segTifDir = new JMenuItem("Open Segmented TIF Directory");
        segTifDir.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String stProp = props.getProperty("SegTiffs");
                if (stProp != null){
                    nucChooser.setSelectedFile(new File(stProp));
                } 
                if (nucChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){    
                    File sel = nucChooser.getSelectedFile();
                    TifDirectoryImageSource source = new TifDirectoryImageSource(sel.getPath(),"Segmented",imagedEmbryo);
                        panel.setTimeRange(Math.max(source.getMinTime(),panel.getMinTime())
                            ,Math.min(source.getMaxTime(),panel.getMaxTime()) );
                    imagedEmbryo.notifyListeners();
                    props.setProperty("SegTiffs",sel.getPath());
                }                 
            }
        });
        fileMenu.add(segTifDir);
        
        JMenuItem bhcOpen = new JMenuItem("Open BHC Trees ");
        bhcOpen.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    openBHCDir(null);
                    imagedEmbryo.notifyListeners();
                    
                }catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
        fileMenu.add(bhcOpen); 
        fileMenu.addSeparator();
        
        JMenuItem session = new JMenuItem("Open Existing Session");
        fileMenu.add(session);
        session.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String prop = props.getProperty("Session");
                if (prop != null){
                    nucChooser.setSelectedFile(new File(prop));
                }
                if (nucChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){    
                    File sel = nucChooser.getSelectedFile();
                    try {
                        openSession(sel);
                    } catch (Exception exc){
                        exc.printStackTrace();
                    }
                    props.setProperty("Session",sel.getPath());
                }
            }
        });
        
        JMenuItem saveSession = new JMenuItem("Save Current Session");
        fileMenu.add(saveSession);
        saveSession.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    saveSession();

                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
        fileMenu.addSeparator();
/*        
        
        JMenuItem hyper = new JMenuItem("Import ImageJ Hyperstack");
        hyper.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String timeStr = setMinTime();
                if (timeStr != null){
                    source = new ImageJHyperstackSource();
                    source.open();
                    initToSource();
                    int firstTime = Integer.valueOf(timeStr);
                    source.setFirstTime(firstTime);
                    panel.setTimeRange(firstTime, source.getMaxTime());                    
                }
            }
        });
        fileMenu.add(hyper);

        JMenuItem superVoxel = new JMenuItem("Open TGMM SuperVoxel Binary Files");
        superVoxel.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String svFile = props.getProperty("TGMMSuperVoxelBinaryFile");
                if (svFile != null){
                    sourceChooser.setSelectedFile(new File(svFile));
                } 
                if (sourceChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){
                    props.setProperty("TGMMSuperVoxelBinaryFile",sourceChooser.getSelectedFile().getPath()); 
                    source = new TGMMSuperVoxelSource(sourceChooser.getSelectedFile().getPath());
                    source.open();
                    initToSource();
                }
                int fhsduis=0;
            }
        });
        fileMenu.add(superVoxel);
       
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
                source = new TifDirectoryImageSource("/net/waterston/vol2/home/gevirl/rnt-1/segmented");
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
*/        
       
 /*       
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
*/        
        
/*
        JMenuItem nucSave = new JMenuItem("Save Nuclei File");
        nucSave.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (nucFile.getFile() != null ){
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
 */       
        JMenuItem calcExp = new JMenuItem("Calculate Expression");
        calcExp.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                Ace3D_Frame.this.imagedEmbryo.calculateExpression();
            }
        });
        fileMenu.add(calcExp);
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
        
        JMenu segmenting = new JMenu("Segmenting");
        menuBar.add(segmenting);
        
        JMenuItem cutItem = new JMenuItem("Identify Nuclei");
        cutItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    cutTree();
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });       
        segmenting.add(cutItem); 

        segmenting.addSeparator();
/*        
        JMenuItem allTimePnts = new JMenuItem("Submit All Time Points");
        allTimePnts.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    submitAllTimePoints();
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
        segmenting.add(allTimePnts);
        
        JMenu selectedTimePoint = new JMenu("Selected Time Point");
        segmenting.add(selectedTimePoint);
 */       
        JMenuItem scatter = new JMenuItem("Intensity/Volume Plot");
        scatter.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    viDialog.setVisible(true);
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
        segmenting.add(scatter);

        JMenuItem lineplot = new JMenuItem("Nuclei Probability Plot");
        lineplot.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    
                    int time = getCurrentTime();
                    BHCTree tree = bhc.getTree(time);
                    SegmentationLinePlot plot = new SegmentationLinePlot();
                    plot.setTree(tree);
                    PanelDisplay dialog = new PanelDisplay(plot);
                    dialog.setVisible(true);    
                   
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });        
        segmenting.add(lineplot);
        segmenting.addSeparator();
        
        JMenuItem remove = new JMenuItem("Remove Nuclei - Current Time");
        remove.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
               Set<Nucleus> nucs = imagedEmbryo.getNuclei(getCurrentTime());
               for (Nucleus nuc : nucs){
                   imagedEmbryo.getNucleusFile().removeNucleus(nuc,false);
               }
               imagedEmbryo.notifyListeners();
            }
        });
        segmenting.add(remove);
        JMenuItem removeAll = new JMenuItem("Remove All Nuclei");
        removeAll.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                for (Integer t : imagedEmbryo.getNucleusFile().getAllTimes()){
                   Set<Nucleus> nucs = imagedEmbryo.getNuclei(t);
                   for (Nucleus nuc : nucs){
                       imagedEmbryo.getNucleusFile().removeNucleus(nuc,false);
                   }                    
                }
                imagedEmbryo.notifyListeners();
            }
            
        });
        segmenting.add(removeAll);        
/*        
        JMenuItem outlierItem = new JMenuItem("Resegment Outliers");
        outlierItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
//                    cutTreeOutlier();
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });        
        selectedTimePoint.add(outlierItem);
 */       

        
        JMenu linking = new JMenu("Linking");
        menuBar.add(linking);
        JMenuItem linkItem = new JMenuItem("Auto Link");
        linking.add(linkItem);
        linkItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    ((LinkedNucleusFile)imagedEmbryo.getNucleusFile()).autoLinkBetweenCuratedTimes(getCurrentTime());
                } catch (Exception exc){
                    exc.printStackTrace();
                }
/*                
                String timeStr = JOptionPane.showInputDialog("Enter the last time to link:");
                if (timeStr != null){
                    Integer endTime;
                    try {
                        endTime = Integer.valueOf(timeStr);

                    } catch (Exception exc){
                        JOptionPane.showMessageDialog(Ace3D_Frame.this, "Invalid entry");
                        return;
                    }
                    
                    for (int t=getCurrentTime() ; t<=endTime ; ++t){
                        try {
                            if (t == 21){
                                int uisahdfuis=0;
                            }
                            ((LinkedNucleusFile)imagedEmbryo.getNucleusFile()).autoLink(t);

 //                           ((Ace3DNucleusFile)nucFile).linkTimePointAdjustable(t,Ace3D_Frame.this.imagedEmbryo.getBHCTree(t+1));
//                            ((Ace3DNucleusFile)nucFile).linkTimePoint(t);
                        } catch (Exception exc){
                            exc.printStackTrace();
                        }
                    }
                    
                }
*/                
            }
        });
        JMenuItem unlink = new JMenuItem("Unlink Current Time");
        linking.add(unlink);
        unlink.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                Set<Nucleus> nucs = imagedEmbryo.getNucleusFile().getNuclei(getCurrentTime());
                NucleusFile nucFile = imagedEmbryo.getNucleusFile();
                int count = 1;
                for (Nucleus nuc : nucs){
                    nucFile.unlinkNucleus(nuc, count == nucs.size());  // notify listeners at last nucleus
                    ++count;
                }
            }
        });        
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
        
        divisionIndicator = new JCheckBoxMenuItem("Division indicator");
        divisionIndicator.setSelected(true);
        divisionIndicator.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.repaint();
            }
        });        
        view.add(divisionIndicator);
        
        sisters = new JCheckBoxMenuItem("Sister indicator");
        sisters.setSelected(false);
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
    private String setMinTime(){
        String ret = null;
        boolean valid = false;
        while (!valid) {
            String timeStr = JOptionPane.showInputDialog("Enter the first time value:");
            if (timeStr == null) return ret;  // null return means user cancelled input
            try {

                valid = true;
                ret = timeStr;
                
            } catch (Exception exc){
                JOptionPane.showMessageDialog(this,"Not a valid time entry");
            }
        }
        return ret;        
    }
    public int getCurrentTime(){
        return panel.getTime();
    }
/*    
    private void openNucFile()throws Exception {
        String f = props.getProperty("NucFile");
        if (f != null){
            nucChooser.setSelectedFile(new File(f));
        }         
        if (nucChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){
            nucFile = new NamedNucleusFile(nucChooser.getSelectedFile());
            nucFile.open();

            if (imagedEmbryo != null){
                imagedEmbryo.setNucleusFile(nucFile);
            }            
            nucFile.addListener(navFrame);
            nucFile.addSelectionOberver(selectedNucFrame);
            nucFile.addSelectionOberver(panel);            
            
            props.setProperty("NucFile",nucFile.getFile().getPath());            

        }
    }
*/    
    // starting from BHC directory, no nucleus file exists yet
    private void openBHCDir(File sel)throws Exception {
        if (sel == null){
            String bhcProp = props.getProperty("BHC");
            if (bhcProp != null){
                nucChooser.setSelectedFile(new File(bhcProp));
            } 
            if (nucChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){
                sel = nucChooser.getSelectedFile();

            }            
        }
        
        bhc  = new BHCTreeDirectory(sel);
        imagedEmbryo.getNucleusFile().setBHCTreeDirectory(bhc);

        props.setProperty("BHC",sel.getPath());            
        
    } 
    private void openSession(File xml)throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(xml);
        Element root = doc.getRootElement();   
        if (!root.getName().equals("Ace3DSession")){
            return;
        }
        
        Element bhcEle = root.getChild("BHCTreeDirectory");
        if (bhcEle != null){
            openBHCDir(new File(bhcEle.getAttributeValue("path")));
        }
        imagedEmbryo.fromXML(root.getChild("ImagedEmbryo"));
        for (ImageSource source : imagedEmbryo.getSources()){
            panel.setTimeRange(Math.max(source.getMinTime(),panel.getMinTime())
                ,Math.min(source.getMaxTime(),panel.getMaxTime()) );
        }
        imagedEmbryo.notifyListeners();  
        
        Element dsEle = root.getChild("DataSets");
        for (Element props : dsEle.getChildren("DataSetProperties")){
            String name = props.getAttributeValue("Name");
            DataSetProperties p = new DataSetProperties(props);
            dataSetProperties.put(name, p);
            contrastDialog.setProperties(name, p);
        }
        this.sessionXML = xml;
        
    }
    private void saveSession()throws Exception {
        
        if (sessionXML == null){
            JFileChooser sessionChooser;
            if (this.bhc == null){
                sessionChooser = new JFileChooser();
            } else {
                sessionChooser = new JFileChooser(bhc.getDirectory());
            }
            if (sessionChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION){
                sessionXML = sessionChooser.getSelectedFile();
            } else {
                return;
            }
        }
        
        Element root = new Element("Ace3DSession");
        if (bhc != null){
            root.addContent(bhc.toXML());
        }
       
        root.addContent(imagedEmbryo.toXML());
        
        Element dsProps = new Element("DataSets");
        for (String ds : dataSetProperties.keySet()){
            DataSetProperties props = dataSetProperties.get(ds);
            Element dsEle = props.toXML();
            dsEle.setAttribute("Name", ds);
            dsProps.addContent(dsEle);
        }
        root.addContent(dsProps);
        
        OutputStream stream = new FileOutputStream(sessionXML);       
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        out.output(root, stream);
        stream.close();
        props.setProperty("Session",sessionXML.getPath());
        
        // save the nuclei and cells
//        imagedEmbryo.getNucleusFile().save();

    }
    
    /*
    private void openStarryNiteNucFile()throws Exception {
        
//        nucFile = new StarryNiteNucleusFile("/nfs/waterston/pete/Segmentation/dispim_sample_data/matlab_output/CroppedReslicedBGSubtract488/Decon_emb1.zip");
        String starry = props.getProperty("StarryNite");
        if (starry != null){
            nucChooser.setSelectedFile(new File(starry));
        }
        if (nucChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){
            nucFile = new StarryNiteNucleusFile(nucChooser.getSelectedFile(),panel,selectedNucFrame);
            nucFile.addListener(navFrame);
            nucFile.open();
            props.setProperty("StarryNite",nucFile.getFile().getPath());
            if (imagedEmbryo != null){
                long[] coords = TimePointImage.getMinCoordinate();
                ((StarryNiteNucleusFile)nucFile).adjustCoordinates((int)coords[0],(int)coords[1],(int)coords[2]);
                imagedEmbryo.setNucleusFile(nucFile);
            }
        }        
    }
*/
    private void submitAllTimePoints()throws Exception {
        JFileChooser fileChooser = new JFileChooser();
        String segTiff = props.getProperty("SegmentedTIFF");
        if (segTiff != null){
            fileChooser.setSelectedFile(new File(segTiff));
        }
        if (fileChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){
            segTiff = fileChooser.getSelectedFile().getPath();
//            Nuclei_Identification.submitTimePoints(segTiff,1,2);
            props.setProperty("SegmentedTIFF",segTiff);
        }
    }
    private void cutTree()throws Exception {
        int time = this.getCurrentTime();

        BHCTreeDirectory bhcTree = imagedEmbryo.getNucleusFile().getTreeDirectory();
        BHCTree tree = bhcTree.getTree(this.getCurrentTime());
        if (treeCutDialog == null){
            treeCutDialog = new BHCTreeCutDialog(this,this.imagedEmbryo);
        }
        treeCutDialog.setBHCTree(tree);
        treeCutDialog.setVisible(true);
        if (treeCutDialog.isOK()){
            double nextThresh = treeCutDialog.getThresh();
 //           bhcNucFile.setThreshold(nextThresh);
        }
    }
/*
    private void cutTreeOutlier()throws Exception {
        int time = this.getCurrentTime();
        BHCNucleusDirectory bhc = ((Ace3DNucleusFile)nucFile).getBHC();
        BHCNucleusFile bhcNucFile = bhc.getFileforTime(time); 
        BHCTree tree = imagedEmbryo.getBHCTree(time);
        BHCNucleusFile replace = bhcNucFile.cutTreeOutlier(tree);
        if (replace != null){
            ((Ace3DNucleusFile)this.nucFile).replaceTime(replace);
        }
    }
    private void saveAsNucFile()throws Exception {
        buildChooser();
        if (nucFile != null && nucFile.getFile()!=null){
            nucChooser.setSelectedFile(nucFile.getFile().getParentFile());
        } else {
            java.util.Properties pros = System.getProperties();
            Map<String,String> map = System.getenv();
            nucChooser.setSelectedFile(new File(System.getProperty("user.home")));
        }
        if (nucChooser.showSaveDialog(panel) == JFileChooser.APPROVE_OPTION){
            File f = nucChooser.getSelectedFile();
            if (nucFile.getFile()!=null && f.getPath().equals(nucFile.getFile().getPath())){

                if (JOptionPane.showConfirmDialog(rootPane,"Replace the original nucleus file?")==JOptionPane.OK_OPTION){
                    nucFile.saveAs(f);
                }
                
            } else {
                nucFile.saveAs(nucChooser.getSelectedFile());
            }
        }
    }
 */   


/*
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
*/

    @Override
    public void stateChanged(ChangeEvent e) {
        panel.stateChanged(e);
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
    static public boolean divisionsIndicated(){
        return divisionIndicator.getState();
    }    
    static public boolean nucleiIndicated(){
        return segmentedNuclei.getState();
    }
    static public boolean locationIndicated(){
        return locationIndicator.getState();
    }

    static public List<String> datasetsSelected(){
        ArrayList<String> ret = new ArrayList<>();
        for (String key : dataSetProperties.keySet()){
            DataSetProperties props = dataSetProperties.get(key);
            if (props.selected){
                ret.add(key);
            }
        }
        return ret;
    }
    static public List<String> getAllDatsets(){
        ArrayList<String> ret = new ArrayList<>();
        for (String key : dataSetProperties.keySet()){
            ret.add(key);
        }
        return ret;        
    }
    static public DataSetProperties getProperties(String dataSet){
        return dataSetProperties.get(dataSet);
    }
    static public void setProperties(String dataset,DataSetProperties ps){
        dataSetProperties.put(dataset, ps);
    }
    
    static public LUT getLUT(String dataSet){
        return dataSetLuts.get(dataSet);
    }
    static public DataSetsDialog getDataSetsDialog(){
        return contrastDialog;
    }
    public ImagedEmbryo getEmbryo(){
        return this.imagedEmbryo;
    }
    File sessionXML;
    Properties props = new Properties();
    JMenu dataset;
    JMenu contrast;
    JMenu lutMenu;
    JMenu colorMenu;
    
//    ImageSource source;
//    NucleusFile nucFile;
    ImagedEmbryo imagedEmbryo;
    
    
    SynchronizedMultipleSlicePanel panel;
    SelectedNucleusFrame selectedNucFrame;
    JFileChooser nucChooser = new JFileChooser();
//    JFileChooser sourceChooser = new JFileChooser();
    static DataSetsDialog contrastDialog;
    Navigation_Frame navFrame;
    LookUpTables lookUpTables = new LookUpTables();
    BHCTreeCutDialog treeCutDialog;
    BHCTreeDirectory bhc;
    VolumeIntensityPlot viPlot;
    PanelDisplay viDialog;
    
    static JCheckBoxMenuItem segmentedNuclei;
    static JCheckBoxMenuItem sisters;
    static JCheckBoxMenuItem locationIndicator;
    static JCheckBoxMenuItem divisionIndicator;
    static JCheckBoxMenuItem nucleiLabeled;
    static JCheckBoxMenuItem selectedLabeled;
    static JMenuItem[] colorChoices;
    static TreeMap<String,LUT> dataSetLuts = new TreeMap<>();
    
    static TreeMap<String,DataSetProperties> dataSetProperties = new TreeMap<>();
//    static public double R=15.0;
    
    static public void main(String[] args) {
        EventQueue.invokeLater(new Runnable(){
            @Override
            public void run() {
                Interpreter.batchMode=true;
                ImageJ ij = new ImageJ(ImageJ.NO_SHOW);
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
