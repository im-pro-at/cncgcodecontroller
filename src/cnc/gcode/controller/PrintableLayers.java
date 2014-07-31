/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import cnc.gcode.controller.CNCCommand.Type;
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
        
        private int index;

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
            double ts = Database.TOOLSIZE.getsaved();
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
    private HashMap<Double, LinkedList<IElement>> layers = new HashMap<>();
    private Double[] keys = new Double[0];
    private HashSet<Integer> sIndex = new HashSet<>();
    private int sMin =-1,sMax=-1;
    boolean isBlocked = false;

    public void processMoves(int index, CNCCommand.Move[] moves) {
        for (CNCCommand.Move move : moves) 
        {
            //Calc Layer
            double sz = move.getStart()[2];
            double ez = move.getEend()[2];
            if(Double.isNaN(sz) || Double.isNaN(ez))
            {
                return;
            }
            
            //Add layer if new
            if (!layers.containsKey(sz)) 
            {
                layers.put(sz, new LinkedList<IElement>());
            }
            if (!layers.containsKey(ez)) 
            {
                layers.put(ez, new LinkedList<IElement>());
            }
            if (sz == ez) 
            {
                //Line
                for(int i = 0;i < 2;i++)
                {
                    if(Double.isNaN(move.getStart()[i]) || Double.isNaN(move.getEend()[i]))
                    {
                        return;
                    }
                }
                layers.get(sz).add(new Line(index, Arrays.copyOfRange(move.getStart(), 0, 2), Arrays.copyOfRange(move.getEend(), 0, 2), move.getType()));
            } else {
                //Two Points
                if(!Double.isNaN(move.getStart()[0]) && !Double.isNaN(move.getStart()[1]))
                {
                    layers.get(sz).add(new Point(index, Arrays.copyOfRange(move.getStart(), 0, 2), move.getType()));
                }
                if(!Double.isNaN(move.getEend()[0]) && !Double.isNaN(move.getEend()[1]))
                {
                    layers.get(ez).add(new Point(index, Arrays.copyOfRange(move.getEend(), 0, 2), move.getType()));
                }
            }
        }
    }

    public void paint(Graphics2D g2, int index) {
        if (index < 0 || index >= keys.length) 
        {
            return;
        }

        for (IElement p : layers.get(keys[index])) 
        {
            p.paint(g2);
        }
    }

    public String[] getLayers() {
        keys = layers.keySet().toArray(new Double[0]);
        Arrays.sort(keys);
        String[] slayers = new String[keys.length];
        for (int i = 0; i < keys.length; i++) 
        {
            slayers[i] = Tools.dtostr(keys[i]);
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
        double size = Database.TOOLSIZE.getsaved() * 2;
        for (IElement e : layers.get(keys[index]))
        {
            if (e.isInRange(p, size)) 
            {
                list.add(e.getIndex());
            }
        }
                
        int[] r = new int[list.size()];
        int i = 0;
        for (int ri : list) 
        {
            r[i++] = ri;
        }
        return r;
    }
    
}
