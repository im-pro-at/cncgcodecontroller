/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author patrick
 */
public class AutoLevelSystem {

    public static class Point
    {
        private Point2D.Double p;
        private double value;
        
        public Point(double x, double y)
        {
            p= new Point2D.Double(x, y);
            value=Double.NaN;
        }
        
        public synchronized boolean isLeveld()
        {
            return !Double.isNaN(value);
        }
        
        public synchronized void setValue(double v) 
        {
            value=v;
        }

        public synchronized double getValue() {
            return value;
        }

        public Point2D.Double getPoint() {
            return new Point2D.Double(p.x, p.y);
        }

        @Override
        public synchronized String toString() {
            return "Position: ["+Tools.dtostr(p.x)+","+Tools.dtostr(p.y)+"] \nValue: "+(isLeveld()?Tools.dtostr(value) :"is not measured!"); 
        }


    }
    
    Point[][] points;
    Rectangle2D.Double pos;
    
    public AutoLevelSystem() {
        points= new Point[0][0];
        pos= new Rectangle2D.Double(0, 0, 0, 0);
    }
    
    public AutoLevelSystem(double sx,double sy,double ex,double ey)
    {
        this();
        
        //calc Points
        double dx=ex-sx;
        double dy=ey-sy;

        if(dx<0 || dy<0 )
            return;
        
        int countx=(int)Math.ceil(dx/Database.ALDISTANACE.getsaved());
        int county=(int)Math.ceil(dy/Database.ALDISTANACE.getsaved());
        
        double distancex=dx/countx;
        if(countx==0) distancex=0;
        double distancey=dy/county;
        if(county==0) distancey=0;
        
        points= new Point[countx+1][county+1];
        pos=new Rectangle2D.Double(sx,sy, distancex, distancey);
        
        for(int i=0;i<countx+1;i++)
            for (int j=0;j<county+1;j++)
            {
                points[i][j]=new Point(sx+i*distancex, sy+j*distancey);
            }
    }
    
    public Point[] getPoints()
    {
        LinkedList<Point> r= new LinkedList<>();
        for(Point[] pp:points)
            for(Point p:pp)
                r.add(p);
        return r.toArray(new Point[0]);
    }
    
    public boolean isLeveled()
    {
        //no Points no Leveling ;-)
        if(points.length==0 || points[0].length==0)
            return false;

        boolean isleveled=true;
        for(Point[] pp:points)
            for(Point p:pp)
                if(!p.isLeveld())
                    isleveled=false;
        return isleveled;
    }
    
    private double linearInterpolation(double x0, double x1, double f0, double f1, double x)
    {
        return f0+(f1-f0)/(x1-x0)*(x-x0); //http://de.wikipedia.org/wiki/Interpolation_(Mathematik)#Lineare_Interpolation
    }
    
    
    public double getdZ(Point2D p)
    {
        if(!isLeveled() || points.length==0 || points[0].length==0)
            return 0.0;
        
        //cals nearest
        int p0X=(int)Math.round((p.getX()-pos.x)/pos.width); 
        if(p0X<0) p0X=0;
        if(p0X>=points.length) p0X=points.length-1;
        
        int p0Y=(int)Math.round((p.getY()-pos.y)/pos.height); 
        if(p0Y<0) p0Y=0;
        if(p0Y>=points[p0X].length) p0Y=points[p0X].length-1;
        
        if(p0X==-1 || p0Y==-1)
            return 0.0;

        //dircet hit!
        if(Math.abs(points[p0X][p0Y].getPoint().getX()-p.getX()) <0.00001 && Math.abs(points[p0X][p0Y].getPoint().getY()-p.getY()) <0.00001)
            return points[p0X][p0Y].getValue();
                
        //nearest is now center -> calc quatrant
        boolean lr=p.getX()>points[p0X][p0Y].getPoint().getX(); //l=false r=true
        boolean lu=p.getY()>points[p0X][p0Y].getPoint().getY(); //l=false u=true

        //calc neighbor coorinats
        int p1X=p0X+(lr?1:-1);
        int p1Y=p0Y+(lu?1:-1);
        
        //Test if there exists neighbor
        boolean neighborX_exists= p1X<points.length && p1X>=0;
        boolean neighborY_exists= p1Y<points[p0X].length && p1Y>=0;
        
        if(!neighborX_exists && !neighborY_exists) //no neighbor
            return points[p0X][p0Y].getValue();
        
        if(neighborX_exists && ( !neighborY_exists || Math.abs( p.getY()-points[p0X][p0Y].getPoint().getY() )<0.00001) )
            return linearInterpolation(points[p0X][p0Y].getPoint().getX(), points[p1X][p0Y].getPoint().getX(), points[p0X][p0Y].getValue(), points[p1X][p0Y].getValue(), p.getX());

        if( neighborY_exists && ( !neighborX_exists || Math.abs( p.getX()-points[p0X][p0Y].getPoint().getX() )<0.00001 ) )
            return linearInterpolation(points[p0X][p0Y].getPoint().getY(), points[p0X][p1Y].getPoint().getY(), points[p0X][p0Y].getValue(), points[p0X][p1Y].getValue(), p.getY());

        //http://en.wikipedia.org/wiki/Bilinear_interpolation#Algorithm
        double r1=linearInterpolation(points[p0X][p0Y].getPoint().getX(), points[p1X][p0Y].getPoint().getX(), points[p0X][p0Y].getValue(), points[p1X][p0Y].getValue(), p.getX());
        double r2=linearInterpolation(points[p0X][p1Y].getPoint().getX(), points[p1X][p1Y].getPoint().getX(), points[p0X][p1Y].getValue(), points[p1X][p1Y].getValue(), p.getX());
        return    linearInterpolation(points[p0X][p0Y].getPoint().getY(), points[p0X][p1Y].getPoint().getY(), r1, r2, p.getY());
        
    }
    
    
    //Static part
    private static AutoLevelSystem al=null;
    
    public static void publish(AutoLevelSystem al)
    {
        AutoLevelSystem.al=al;
    }
    
    public static double correctz(double x, double y, double z)
    {
        double d=al.getdZ(new Point2D.Double(x, y))-Database.ALZERO.getsaved();
        
        if(Double.isNaN(d))
        {
            //this schould never happens!
            (new MyException("Autolefeling Problems!")).printStackTrace();
            d=maxz()-Database.ALZERO.getsaved(); 
        }
        
        return z+d;
    }
    
    private static double maxz() {
        double max=-Double.MAX_VALUE;
        for(Point[] xy:al.points)
            for(Point p:xy)
                if(max<p.value)
                    max=p.value;
        return max;
    }
    
    
    public static boolean leveled()
    {
        if(al==null || !al.isLeveled())
            return false;
        return true;
    }
    
}
