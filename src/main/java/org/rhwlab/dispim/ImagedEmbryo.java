/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import java.io.File;
import java.util.ArrayList;
import org.rhwlab.dispim.nucleus.Nucleus;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.region.localneighborhood.EllipsoidCursor;
import net.imglib2.algorithm.region.localneighborhood.EllipsoidNeighborhood;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransformRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Translation3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.jdom2.Element;
import org.rhwlab.ace3d.Ace3D_Frame;
import org.rhwlab.ace3d.SynchronizedMultipleSlicePanel;
import org.rhwlab.dispim.nucleus.LinkedNucleusFile;
import org.rhwlab.dispim.nucleus.NucleusFile;

/**
 *
 * @author gevirl
 * encapsulates all the images and metadata for a dispim imaging experiment
 */
public class ImagedEmbryo implements Observable {
    public ImagedEmbryo(){
        sources = new ArrayList<>();
        nucFile = new LinkedNucleusFile();
    }
    public void fromXML(Element root){
        
        Element sourcesEle = root.getChild("Sources");
        for (Element src : sourcesEle.getChildren()){
            String name = src.getName();
            if (name.equals("ImageJHyperStack")){
                new ImageJHyperstackSource(src,this);
            }
            else if (name.equals("TifDirectorySource")){
                new TifDirectoryImageSource(src,this);
            }
        }   
        Element nucFileEle = root.getChild("Nuclei");
        nucFile.fromXML(nucFileEle);
    }
/*
    public void save()throws Exception {
        saveAsXML(xml);
    }
    public void saveAsXML(File xml)throws Exception {
        if (xml == null){
            JFileChooser chooser = new JFileChooser();
            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION){
                xml = chooser.getSelectedFile();
                
            }
        }
        OutputStream stream = new FileOutputStream(xml);       
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        out.output(this.toXML(), stream);
        stream.close();         
    }
    */
    public Element toXML(){
        Element ret = new Element("ImagedEmbryo");
        
        Element sourcesEle = new Element("Sources");
        ret.addContent(sourcesEle);
        for (ImageSource src : sources){
            sourcesEle.addContent(src.toXML());
        }
        
        ret.addContent(nucFile.toXML());
        
        File bhcDir  = nucFile.getTreeDirectory().getDirectory();
        File f = nucFile.getFile();
        if (f != null){
            Element nucFileEle = new Element("NucleusFile");
            nucFileEle.setAttribute("file", f.getPath());

            ret.addContent(nucFileEle);
        }
        return ret;
    }
    public void addSource(ImageSource src){
        sources.add(src);
 //       notifyListeners();
    }
    public CompositeTimePointImage getImage(int time){
        return new CompositeTimePointImage(time);
    }

    public int getTimes(){
        int ret = Integer.MAX_VALUE;
        for (ImageSource source : sources){
            int t = source.getTimes();
            if (t < ret) ret = t;
        }
        return ret;
    }
    public int getMinTime(){
        int ret = Integer.MIN_VALUE;
        for (ImageSource source : sources){
            int t = source.getMinTime();
            if (t > ret) ret = t;
        }
        return ret;        
    }
    public int getMaxTime(){
        int ret = Integer.MAX_VALUE;
        for (ImageSource source : sources) {
            int t = source.getMaxTime();
            if (t < ret) {
                ret = t;
            }
        }
        return ret;
    }
    public void setNucleusFile(NucleusFile file){
        nucFile = file;

    }
    public Nucleus selectedNucleus(){
        if (nucFile != null){
            return this.nucFile.getSelected();
        }
        return null;
    }
    public void setSelectedNucleus(Nucleus toSelect){
        this.nucFile.setSelected(toSelect);

    }
    public void setMarked(Nucleus toMark,boolean value){
        toMark.setMarked(value);
        notifyListeners();
    }
    public List<Nucleus> nextNuclei(Nucleus source){
        return nucFile.linkedForward(source);
    }
    public Nucleus previousNucleus(Nucleus source){
        return nucFile.linkedBack(source);
    }

    public void clearLabeled(int time){
        Set<Nucleus> nucs = nucFile.getNuclei(time);
        for (Nucleus nuc : nucs){
            nuc.setLabeled(false);
        }        
    }    
    public NucleusFile getNucleusFile(){
        return nucFile;
    }

    public Set<Nucleus> getNuclei(int time){
        if (nucFile == null){
            return new TreeSet<>();
        }
        return nucFile.getNuclei(time);
    }
    public void addNucleus(Nucleus nuc){
        nucFile.addNucleus(nuc);
    }
    public void calculateExpression(){
        List<String> datasets = Ace3D_Frame.datasetsSelected();
        if (!datasets.isEmpty()){
            Set<Integer> times = nucFile.getAllTimes();
            for (Integer time : times){
                this.calculateExpression(datasets.get(0), time);
            }
        }
    }
    // calculate the expression for all the nuclei at a given time using the given dataset images
    public void calculateExpression(String dataset,int time){
        Set<Nucleus> nuclei = nucFile.getNuclei(time);
        TimePointImage tpi = sourceForDataset(dataset).getImage(dataset,time);
        for (Nucleus nuc : nuclei){
            double[][] e = nuc.getEigenVectors();
            try {
            double exp = calculateExpression(nuc,tpi,e);
            nuc.setExpression(exp);
            } catch (Exception exc){
                System.err.printf("%s nucleus:%s time:%d\n",exc.getMessage(),nuc.getName(),nuc.getTime());
            }
        }
        notifyListeners();
    }

    public double calculateExpression(Nucleus nuc ,TimePointImage tpi,double[][] e)throws Exception {
        double[] center = nuc.getCenter();
        
        long[] radii = nuc.getRadii();
        long[] min = {-2*radii[0]-1,-2*radii[1]-1,-2*radii[2]-1};
        long[] max = {2*radii[0]+1,2*radii[1]+1,2*radii[2]+1};
        
        RandomAccessibleInterval img = tpi.getImage(); 
        RealRandomAccessible interpolated = Views.interpolate(img, new NLinearInterpolatorFactory() );

        Translation3D translation = new Translation3D(-center[0],-center[1],-center[2]);
        RealTransformRandomAccessible trans = RealViews.transform(interpolated, translation);
        
        AffineTransform3D rotation = new AffineTransform3D();
        rotation.set(e[0][0], e[0][1], e[0][2], 0, e[1][0], e[1][1], e[1][2], 0, e[2][0], e[2][1], e[2][2], 0);
        RealTransformRandomAccessible rotate = RealViews.transform(trans, rotation);
        
        IntervalView originView = Views.interval(rotate,min,max);
        
        double[] position = new double[3];
        
        long[] origin = {0,0,0};

        EllipsoidNeighborhood ellipse = new EllipsoidNeighborhood(originView,origin,radii);
        EllipsoidCursor  cursor = ellipse.cursor();

        double exp = 0.0;
        int count = 0;
        while (cursor.hasNext()){
            
            cursor.fwd();
            UnsignedShortType pix = (UnsignedShortType)cursor.get();
            exp = exp + pix.getInteger();
/*            
            cursor.localize(position);
            exp = exp + pix.getInteger()*nuc.prob(position);
*/
            ++count;
 //           System.out.printf("(%f,%f,%f) %d\n", position[0],position[1],position[2],pix.getInteger());;
            
        }
//        return 1000.0*exp/count;
        return exp/count;
    }
    public void setExpression(Nucleus nuc,double exp){
        nuc.setExpression(exp);
        notifyListeners();
    }
    public void notifyListeners(){
        for (InvalidationListener listener : listeners){
            listener.invalidated(this);
        }
    }

    @Override
    public void addListener(InvalidationListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        listeners.remove(listener);
    }
    public Set<Nucleus> getMarkedNuclei(int time){
        TreeSet<Nucleus> ret = new TreeSet<>();
        if (nucFile != null){
            Set<Nucleus> all = nucFile.getNuclei(time);
            for (Nucleus nuc : all){
                if (nuc.getMarked()){
                    ret.add(nuc);
                }
            }
        }
        return ret;
    }
    public void setPanel(SynchronizedMultipleSlicePanel panel){
        this.panel = panel;
        this.addListener(panel);
    }
    public TimePointImage getTimePointImage(String dataset,int time){
        
        return sourceForDataset(dataset).getImage(dataset, time);
    }
    public ImageSource sourceForDataset(String dataset){
        for (ImageSource source : sources){
            for (DataSetDesc desc : source.getDataSets()){
                if (dataset.equals(desc.getName())){
                    return source;
                }
            }
        }
        return null;
    }
    public void renameSelectedCell(String newName){
//        ((Ace3DNucleusFile)nucFile).renameCell(newName);
    }
    // join the selected nucleus to its sister
    public void joinSelectedNucleus()throws Exception {
/*       
        Nucleus nuc = nucFile.getSelected();
        if (nuc == null) return;
        int time = nuc.getTime();
        
        // make a Nucleus from the selected nucleus' parent node
        BHCNucleusData bhcNuc = (BHCNucleusData)nuc.getNucleusData();
        BHCTree tree = getBHCTree(time);
        NodeBase node = (NodeBase)tree.findNode(Integer.valueOf(bhcNuc.getSourceNode()));
        NodeBase parent = (NodeBase)node.getParent();
        BHCNucleusData joinedNuc = new BHCNucleusData(nuc.getTime(),parent.formElementXML(Integer.valueOf(bhcNuc.getID())));
        
        // find the Nucleus that corresponds to the sister node
        Nucleus sisterNuc = null;
        int sisterLabel = ((NodeBase)node.getSister()).getLabel();
        for (Nucleus possibleSister : nucFile.getNuclei(time)){
            int possibleLabel = Integer.valueOf(((BHCNucleusData)possibleSister.getNucleusData()).getSourceNode());
            if (possibleLabel == sisterLabel){
                sisterNuc = possibleSister;
                break;
            }
        }
        // unlink the nuclei
        Ace3DNucleusFile nf = ((Ace3DNucleusFile)nucFile);
        nf.unlinkTime(time);
        nf.unlinkTime(time-1);
            
        nf.removeNucleus(nuc, false);
        nf.removeNucleus(sisterNuc, false);
        nf.addNucleus(new Nucleus(joinedNuc),false);

//        nf.linkTimePoint(time-1);
//        nf.linkTimePoint(time);
        nf.notifyListeners();        
*/
    }
    // split the selected nucleus into daughters 
    public void splitSelectedNucleus(){
        
    }
    /*
    public BHCTree getBHCTree(int time)throws Exception {
        BHCTree tree = this.bhcTreeMap.get(time);
        if (tree == null) {
            BHCNucleusDirectory bhc = ((Ace3DNucleusFile) nucFile).getBHC();
            BHCNucleusFile bhcNucFile = bhc.getFileforTime(time);
            File bhcFile = bhcNucFile.getBHCTreeFile();
            tree = new BHCTree(bhcFile.getPath());
            bhcTreeMap.put(time, tree);
        }
        return tree
    } 
*/



    public List<ImageSource> getSources(){
        return sources;
    }



    public TreeMap<Integer,Set<Nucleus>> getRootNuclei(){
        TreeMap<Integer,Set<Nucleus>> ret = new TreeMap<>();
        for (Integer t : nucFile.getAllTimes()){
            Set<Nucleus> roots = nucFile.getRoots(t);
            if (!roots.isEmpty()){
                ret.put(t,roots);
            }
        }
        return ret;
    }
    NucleusFile nucFile;
    List<ImageSource>  sources;
    
    SynchronizedMultipleSlicePanel panel;
    ArrayList<InvalidationListener> listeners = new ArrayList<>();
 
//    Nucleus selectedNuc;
}
