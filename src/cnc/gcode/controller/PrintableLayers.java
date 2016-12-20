/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import cnc.gcode.controller.CNCCommand.Type;
import de.unikassel.ann.util.ColorHelper;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

/**
 *
 * @author patrick
 */
public class PrintableLayers {

    private abstract class IElement {
        
        private final int index;

        public IElement(int index) 
        {
            this.index = index;
        }

        public abstract void paint(Graphics2D g2);

        public abstract boolean isInRange(Point2D p, double size);

        public Integer getIndex() 
        {
            return index;
        }
    }
    
    private class Point extends IElement 
    {

        double[] point;
        Type type;

        public Point(int index, double[] point, Type type) 
        {
            super(index);
            this.point  = point;
            this.type   = type;
        }

        @Override
        public void paint(Graphics2D g2) 
        {
            double ts = DatabaseV2.TOOLSIZE.getsaved();
            type.setupGraphicsOptions(g2, selected(getIndex()));
            g2.fill(new Ellipse2D.Double(point[0] - ts / 2,
                                        point[1] - ts / 2,
                                        ts,
                                        ts));
        }

        @Override
        public boolean isInRange(Point2D p, double size) {
            return Geometrics.pointInRectangle(p , new Rectangle2D.Double(point[0],
                                                                          point[1],
                                                                          0.0,
                                                                          0.0), size);
        }

    }

    private class Line extends IElement {

        Line2D.Double line;
        Type type;

        public Line(int index, double[] start, double[] end, Type type) 
        {
            super(index);
            line = new Line2D.Double(start[0], start[1], end[0], end[1]);
            this.type = type;
        }

        @Override
        public void paint(Graphics2D g2) 
        {
            type.setupGraphicsOptions(g2, selected(getIndex()));
            g2.draw(line);
        }

        @Override
        public boolean isInRange(Point2D p, double size) 
        {
            return line.ptSegDist(p)<size/2;
        }

    }

    private class XYZ extends IElement {
        Line2D.Double line;
        double z;

        public XYZ(int index, CNCCommand.Move m) 
        {
            super(index);
            if(m.isXyz()){
                z=(m.getStart()[2]+m.getEnd()[2])/2;
            }
            else
            {
                z=m.getA()/m.getDistanceXY();
            }
            line = new Line2D.Double(m.getStart()[0], m.getStart()[1], m.getEnd()[0],  m.getEnd()[1]);
        }

        @Override
        public void paint(Graphics2D g2) 
        {
            g2.setColor(Color.red);
            CNCCommand.Type.setupGraphicsOptions(g2, selected(getIndex()),ColorHelper.numberToColorPercentage(Tools.adjustDouble( (z-zmin)/(zmax-zmin)  , 0, 1)));
            g2.draw(line);
        }

        @Override
        public boolean isInRange(Point2D p, double size) 
        {
            return line.ptSegDist(p)<size/2;
        }

    }
    
    
    private HashMap<Double, LinkedList<IElement>> layers = new HashMap<>();
    private Double[] keys = new Double[0];
    private HashSet<Integer> sIndex = new HashSet<>();
    private int sMin =-1,sMax=-1;
    boolean isBlocked = false;
    private double zmin=Double.MAX_VALUE;
    private double zmax=-Double.MAX_VALUE;

    public void processMoves(int index, CNCCommand.Move[] moves) {
        for (CNCCommand.Move move : moves) 
        {
            //Calc Layer
            double sz = move.getStart()[2];
            double ez = move.getEnd()[2];
            if(Double.isNaN(sz) || Double.isNaN(ez))
            {
                return;
            }

            if(!Double.isNaN(move.getDistance()) && (move.isXyz() || (!Double.isNaN(move.getA()) && move.getA()!=0))){
                if(!layers.containsKey(Double.NaN)){
                    layers.put(Double.NaN, new LinkedList<>());
                }
                if(move.isXyz())
                {
                    //XYZ Move!
                    zmin=Math.min(Math.min(sz, ez),zmin);
                    zmax=Math.max(Math.max(sz, ez),zmax);
                    layers.get(Double.NaN).add(new XYZ(index, move));
                }
                else
                {
                    //A Move!
                    zmin=Math.min(move.getA()/move.getDistanceXY() ,zmin);
                    zmax=Math.max(move.getA()/move.getDistanceXY() ,zmax);
                    layers.get(Double.NaN).add(new XYZ(index, move));                    
                }
            }
            else{
                //Add layer if new
                if (!layers.containsKey(sz)) 
                {
                    layers.put(sz, new LinkedList<>());
                }
                if (!layers.containsKey(ez)) 
                {
                    layers.put(ez, new LinkedList<>());
                }
                if (sz == ez) 
                {
                    //Line
                    for(int i = 0;i < 2;i++)
                    {
                        if(Double.isNaN(move.getStart()[i]) || Double.isNaN(move.getEnd()[i]))
                        {
                            return;
                        }
                    }
                    layers.get(sz).add(new Line(index, Arrays.copyOfRange(move.getStart(), 0, 2), Arrays.copyOfRange(move.getEnd(), 0, 2), move.getType()));
                } else {
                    //Two Points
                    if(!Double.isNaN(move.getStart()[0]) && !Double.isNaN(move.getStart()[1]))
                    {
                        layers.get(sz).add(new Point(index, Arrays.copyOfRange(move.getStart(), 0, 2), move.getType()));
                    }
                    if(!Double.isNaN(move.getEnd()[0]) && !Double.isNaN(move.getEnd()[1]))
                    {
                        layers.get(ez).add(new Point(index, Arrays.copyOfRange(move.getEnd(), 0, 2), move.getType()));
                    }
                }
            }
        }
    }

    public void paint(Graphics2D g2, int index) {
        if (index < 0 || index >= keys.length) 
        {
            return;
        }

        layers.get(keys[index]).stream().forEach((p) -> {
            p.paint(g2);
        });
    }

    public String[] getLayers(double offset) {
        keys = layers.keySet().toArray(new Double[0]);
        Arrays.sort(keys);
        String[] slayers = new String[keys.length];
        for (int i = 0; i < keys.length; i++) 
        {
            if(Double.isNaN(keys[i])){
               slayers[i] = "XYZ/A Moves"; 
            }
            else{
                slayers[i] = Tools.dtostr(keys[i]+offset);
            }
        }
        return slayers;
    }

    public void blocknextIndexSet(){
        isBlocked = true;
    }
    
    public void setSelectedIndexs(int[] selectedIndices) {
        if(isBlocked)
        {
            isBlocked = false;
            return;
        }
        HashSet<Integer> hashmap = new HashSet<>(selectedIndices.length);
        for (int i : selectedIndices) 
        {
            hashmap.add(i);
        }
        this.sIndex = hashmap;
        
    }
    
    public void setSelectedRange(int start, int stop)
    {
        if(isBlocked)
        {
            isBlocked = false;
            return;
        }
        this.sIndex = null;
        this.sMin   = start;
        this.sMax   = stop;
    }

    private boolean selected(int i)
    {
        final HashSet<Integer> hs = this.sIndex;
        if(hs != null)
        {
            return hs.contains(i);
        }
        return i <= sMax && i >= sMin;
    }
    
    public int[] getIndexes(Point2D p, int index) {
        if (index < 0 || index >= keys.length) 
        {
            return new int[0];
        }
        LinkedList<Integer> list = new LinkedList<>();
        double size = DatabaseV2.TOOLSIZE.getsaved() * 2;
        layers.get(keys[index]).stream().filter((e) -> (e.isInRange(p, size))).forEach((e) -> {
            list.add(e.getIndex());
        });
                
        int[] r = new int[list.size()];
        int i = 0;
        for (int ri : list) 
        {
            r[i++] = ri;
        }
        return r;
    }
    
    
    public boolean paintlegend(Graphics2D g2, int index, int jpw, int jph){
        if (index < 0 || index >= keys.length || !Double.isNaN(keys[index])) 
        {
            return false;
        }
        
        jpw=jpw-100;
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
        g2.setFont(font);
        int zh      = (int)(font.getStringBounds(Tools.dtostr(100.0), g2.getFontRenderContext()).getHeight()) + 10;                    
        int elements= jph / zh;
        int dy      = (jph-elements * zh) / 2;
        for(int i = 0;i < elements && elements >= 2;i++)
        {
            double z = zmax - i * ((zmax-zmin) / (elements - 1));
            double relative = (z - zmin) / (zmax-zmin);
            relative = Tools.adjustDouble(relative, 0, 1);

            Color c = ColorHelper.numberToColorPercentage(relative);
            g2.setColor(c);
            g2.fillRect(jpw + 5,
                        dy + zh * i,
                        90,
                        zh - 4);
            g2.setColor(((299 * c.getRed() + 587 * c.getGreen() + 114 * c.getBlue())> 128000) ? Color.black:Color.white);
            g2.drawString(Tools.dtostr(z), jpw + 10, dy + zh * i + zh - 10);
            g2.setColor(Color.black);
            g2.drawRect(jpw + 5,
                        dy + zh * i,
                        90,
                        zh - 4);
        }    
        g2.setClip(0,0,jpw,jph);
        
        return true;
    }
}
