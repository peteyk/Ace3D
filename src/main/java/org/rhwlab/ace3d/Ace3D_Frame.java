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
import org.rhwlab.BHC.BHCTree;
import org.rhwlab.ace3d.dialogs.BHCTreeCutDialog;
import org.rhwlab.dispim.DataSetDesc;
import org.rhwlab.dispim.Hdf5ImageSource;
import org.rhwlab.dispim.ImageJHyperstackSource;
import org.rhwlab.dispim.ImageSource;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.Ace3DNucleusFile;
import org.rhwlab.dispim.TifDirectoryImageSource;
import org.rhwlab.dispim.TimePointImage;
import org.rhwlab.dispim.nucleus.BHC_NucleusDirectory;
import org.rhwlab.dispim.nucleus.NucleusFile;
import org.rhwlab.dispim.nucleus.StarryNiteNucleusFile;
import org.rhwlab.dispim.nucleus.TGMM_NucleusDirectory;
import org.rhwlab.dispim.nucleus.TGMM_NucleusFile;
import org.rhwlab.imagesource.TGMMSuperVoxelSource;

/**
 *
 * @author gevirl
 */
public class Ace3D_Frame extends JFrame implements PlugIn , ChangeListener {
    public  Ace3D_Frame()  {
        
        imagedEmbryo = new ImagedEmbryo();
        TimePointImage.setEmbryo(imagedEmbryo);
        
        panel = new SynchronizedMultipleSlicePanel(3);
        imagedEmbryo.addListener(panel);
        this.add(panel);
        
        navFrame = new Navigation_Frame(imagedEmbryo,panel);
        navFrame.run(null);
        
        selectedNucFrame = new SelectedNucleusFrame(this,imagedEmbryo);
        selectedNucFrame.setVisible(true);        
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
    }
    

    final void buildMenu(){
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        
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
/*
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
*/        
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
        
        JMenuItem tgmmOpen = new JMenuItem("Open BHC Nuclei ");
        tgmmOpen.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    openBHCNucFile();
                    
                }catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
        fileMenu.add(tgmmOpen);        
        
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
        
        JMenuItem calcExp = new JMenuItem("Calculate Expression");
        calcExp.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (Ace3D_Frame.this.source != null && Ace3D_Frame.this.nucFile != null){
                    Ace3D_Frame.this.imagedEmbryo.calculateExpression();
                }
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

        
        JMenu nuclesuID = new JMenu("NucleusId");
        menuBar.add(nuclesuID);
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
        nuclesuID.add(allTimePnts);
        JMenu selectedTimePoint = new JMenu("Selected Time Point");
        nuclesuID.add(selectedTimePoint);
        JMenuItem microClusterItem = new JMenuItem("Form Micro Clusters");
        selectedTimePoint.add(microClusterItem);
        JMenuItem bhcItem = new JMenuItem("Run Gaussian Model");
        selectedTimePoint.add(bhcItem);
        JMenuItem cutItem = new JMenuItem("Cut Tree");
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
        
        selectedTimePoint.add(cutItem);
        
        
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
   
    private void initToSource(){
        
                // set up the dataset properties map
//        dataSetProperties.clear();
        Iterator<DataSetDesc> iter = source.getDataSets().iterator();
        while (iter.hasNext()){
            String dataset = iter.next().getName();
            dataSetProperties.put(dataset,new DataSetProperties());
        }   
        imagedEmbryo.addSource(source);
        
        iter = source.getDataSets().iterator();
        while (iter.hasNext()){
            String dataset = iter.next().getName();
            TimePointImage.getSingleImage(dataset,source.getMinTime());
        } 
        
        panel.setEmbryo(imagedEmbryo);
        if (nucFile != null){
            imagedEmbryo.setNucleusFile(nucFile);
            if (nucFile instanceof StarryNiteNucleusFile){
                long[] coords = TimePointImage.getMinCoordinate();
                ((StarryNiteNucleusFile)nucFile).adjustCoordinates((int)coords[0],(int)coords[1],(int)coords[2]);
            }
        }
        if (contrastDialog != null){
            contrastDialog.setVisible(false);
        }
        contrastDialog = new DataSetsDialog(this,imagedEmbryo,0,Short.MAX_VALUE);
        contrastDialog.setVisible(true);
                
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
    private void openNucFile()throws Exception {
        String f = props.getProperty("NucFile");
        if (f != null){
            nucChooser.setSelectedFile(new File(f));
        }         
        if (nucChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){
            nucFile = new Ace3DNucleusFile(nucChooser.getSelectedFile(),panel,selectedNucFrame);
            nucFile.addListener(navFrame);
            nucFile.open();
            props.setProperty("NucFile",nucFile.getFile().getPath());            
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
    private void openBHCNucFile()throws Exception {
        String tgmm = props.getProperty("BHC");
        if (tgmm != null){
            nucChooser.setSelectedFile(new File(tgmm));
        } 
        if (nucChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){
            nucFile = new BHC_NucleusDirectory(nucChooser.getSelectedFile(),panel,selectedNucFrame);
            if (imagedEmbryo != null){
                imagedEmbryo.setNucleusFile(nucFile);
            }             
            nucFile.addListener(navFrame);
            nucFile.open();
            props.setProperty("BHC",nucFile.getFile().getPath());
           
        }
        
    }
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
        TGMM_NucleusDirectory tgmmDirectory = (TGMM_NucleusDirectory)nucFile;
        TGMM_NucleusFile tgmmFile = tgmmDirectory.getFileforTime(time);
        File bhcFile = tgmmFile.getBHCTreeFile();
        BHCTree tree = new BHCTree(bhcFile.getPath());
        if (treeCutDialog == null){
            treeCutDialog = new BHCTreeCutDialog(this,this.nucFile);
        }
        treeCutDialog.setBHCTree(tree,tgmmFile.getThreshold());
        treeCutDialog.setVisible(true);
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
    public ImagedEmbryo getEmbryo(){
        return this.imagedEmbryo;
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
    SelectedNucleusFrame selectedNucFrame;
    JFileChooser nucChooser;
    JFileChooser sourceChooser = new JFileChooser();
    DataSetsDialog contrastDialog;
    Navigation_Frame navFrame;
    LookUpTables lookUpTables = new LookUpTables();
    BHCTreeCutDialog treeCutDialog;
    
    static JCheckBoxMenuItem segmentedNuclei;
    static JCheckBoxMenuItem sisters;
    static JCheckBoxMenuItem locationIndicator;
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
                new ImageJ();
                Interpreter.batchMode=false;
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
