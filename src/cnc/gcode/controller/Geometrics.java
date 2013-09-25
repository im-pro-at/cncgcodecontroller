/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author Patrick
 */
public final class Geometrics {
    
    public static final double NONRATIO=0;
    
    public enum EArea { UPPER_SIDE, LOWER_SIDE, RIGHT_SIDE, LEFT_SIDE, UL_CORNER, UR_CORNER, LL_CORNER, LR_CORNER, CENTER, NON  }
    
    /**
     * Creates a Rectangle out of two Points
     * @param p1
     * @param p2
     * @return 
     */
    public static Rectangle pointToRectangel(Point p1, Point p2){
        Rectangle rect= new Rectangle();
        if(p1.x<p2.x){
            rect.x=p1.x;
            rect.width=p2.x-p1.x;
        }
        else{
            rect.x=p2.x;
            rect.width=p1.x-p2.x;            
        }
        if(p1.y<p2.y){
            rect.y=p1.y;
            rect.height=p2.y-p1.y;
        }
        else{
            rect.y=p2.y;
            rect.height=p1.y-p2.y;            
        }
        return rect;
    }
    public static Rectangle2D pointToRectangel(Point2D p1, Point2D p2){
        double x,y,width,height;
        if(p1.getX()<p2.getX()){
            x=p1.getX();
            width=p2.getX()-p1.getX();
        }
        else{
            x=p2.getX();
            width=p1.getX()-p2.getX();            
        }
        if(p1.getY()<p2.getY()){
            y=p1.getY();
            height=p2.getY()-p1.getY();
        }
        else{
            y=p2.getY();
            height=p1.getY()-p2.getY();            
        }
        return new Rectangle2D.Double(x, y, width, height);
    }
    
    
    
    /**
     * Calces where the Point is located on the rectangle 
     * @param point
     * @param rectangle
     * @return 
     * -1== out of range
     * 0 == in the window
     * 1 2 3 4 = Corners
     * 5 6 7 8 = side 
     *  ---> x
     * |   1 _____ 2
     * |    |  5  |
     * V   8|     |6
     * y    |  0  |
     *      '-----'
     *     4   7   3
     * 0=CENTER
     * 1=UL_CORNER
     * 2=UR_CORNER
     * 3=LR_CORNER
     * 4=LL_CORNER
     * 5=UPPER_SIDE
     * 6=RIGHT_SIDE
     * 7=LOWER_SIDE
     * 8=LEFT_SIDE
     * 
     */
    public static EArea pointOnRectangle(Point point, Rectangle rectangle, int margin){
        EArea area= EArea.NON;
        
        int x= point.x;
        int y= point.y;
        int r_x=rectangle.x;
        int r_y=rectangle.y;
        int r_w=rectangle.width;
        int r_h=rectangle.height;
        
        if(x>r_x && y > r_y && x<(r_x+r_w) && y<(r_y+r_h))
            area= EArea.CENTER;
        if(x>=(r_x-margin) &&  x<=(r_x+r_w+margin)){
            if(Math.abs(y-r_y)<=margin)
                area= EArea.UPPER_SIDE;
            if(Math.abs(y-r_y-r_h)<=margin)
                area= EArea.LOWER_SIDE;
        }
        if(y>(r_y-margin) &&  y<(r_y+r_h+margin)){
            if(Math.abs(x-r_x)<=margin)
                area= EArea.LEFT_SIDE;
            if(Math.abs(x-r_x-r_w)<=margin)
                area= EArea.RIGHT_SIDE;
        }
        if(Math.abs(x-r_x)<=margin && Math.abs(y-r_y)<=margin)
            area= EArea.UL_CORNER;
        if(Math.abs(x-r_x-r_w)<=margin && Math.abs(y-r_y)<=margin)
            area= EArea.UR_CORNER;
        if(Math.abs(x-r_x-r_w)<=margin && Math.abs(y-r_y-r_h)<=margin)
            area= EArea.LR_CORNER;
        if(Math.abs(x-r_x)<=margin && Math.abs(y-r_y-r_h)<=margin)
            area= EArea.LL_CORNER;
        
        return area;
    }
    
    /** Returns true if the Point is over the Rectangle within a given margin*/
    public static boolean pointInRectangle(Point point, Rectangle rectangle, int margin){
        int x= point.x;
        int y= point.y;
        int r_x=rectangle.x;
        int r_y=rectangle.y;
        int r_w=rectangle.width;
        int r_h=rectangle.height;
        if((x+margin)>=r_x && (y+margin) >= r_y && (x-margin)<=(r_x+r_w) && (y-margin)<=(r_y+r_h))
            return true;
        return false;
    }

    public static boolean pointInRectangle(Point2D point, Rectangle2D rectangle, double margin){
        double x= point.getX();
        double y= point.getY();
        double r_x=rectangle.getX();
        double r_y=rectangle.getY();
        double r_w=rectangle.getWidth();
        double r_h=rectangle.getHeight();
        if((x+margin)>=r_x && (y+margin) >= r_y && (x-margin)<=(r_x+r_w) && (y-margin)<=(r_y+r_h))
            return true;
        return false;
    }

    
    /** Returns true if the two Rectangle are overlapping*/
    public static boolean rectangleInRectangle(Rectangle r1, Rectangle r2){
        if(r1.x<(r2.x+r2.width) && (r1.x + r1.width)> r2.x && r1.y<(r2.y+r2.height) && (r1.y + r1.height)> r2.y)
            return true;
        return false;
    }
    
    
    public static double getRatio(double w, double h){
        return (double)w/h;
    }
    public static double getRatio(int w, int h){
        return (double)w/h;
    }
    
    /**
     * Returns a Rectangle placed centered in (0;w,0:h) with the maximum possible size and the given ratio 
     */
    public static Rectangle placeRectangle(int w, int h, double ratio){                
                int w_neu=w;
                int h_neu=h;
                
                if(ratio==NONRATIO)
                    ratio=getRatio(w,h);
                
                if(ratio < getRatio(w,h))
                    w_neu=(int)(ratio*h);
                else
                    h_neu=(int)(w/ratio);                  
                
                return new Rectangle((w-w_neu)/2,(h-h_neu)/2,w_neu, h_neu);
    }
    
    /**
     * Calces the scale for (w2,h2) to fit into (w1,h1) 
     */
    public static double getScale(double w1, double h1, double w2, double h2){
        if(getRatio(w1,h1) < getRatio(w2,h2))
            return (double)w1/w2;            
        else
            return (double)h1/h2;            
    }

    public static double getScale(int w1, int h1, int w2, int h2){
        return getScale((double)w1,(double)h1,(double)w2,(double)h2);
    }

    
    /**
     * Manipulate rectangle (old) (defined in Operation) with delta. Allowed area is (0,max.x;0,max.y)
     * returns new rectangle
     */
    @SuppressWarnings("fallthrough")
    public static Rectangle manipulateRectangle(EArea operation, Rectangle oldR, Point delta, Point max, double ratio){
        Rectangle r =new Rectangle(oldR);

        //Spacel Case Center:
        if(operation==EArea.CENTER){
            r.x+=delta.x;
            r.y+=delta.y;
            if(r.x<0)
                r.x=0;
            if(r.y<0)
                r.y=0;
            if(r.x+r.width>=max.x)
               r.x=max.x-r.width;                                      
            if(r.y+r.height>=max.y)
               r.y=max.y-r.height;    
            return r;
        }
        
        
        int maxWidth;
        int maxHeight;
        boolean editX=false;
        boolean editY=false;
        
        switch(operation){
            case RIGHT_SIDE:    // 
            case LR_CORNER:     // --->                
            case UR_CORNER:     //                
                r.width+=delta.x; 
            case UPPER_SIDE:                      
            default:
                maxWidth=max.x-r.x;
                break;
                
            case LEFT_SIDE:     //                 
            case LL_CORNER:     // <---
            case UL_CORNER:     //               
                r.width-=delta.x;                
            case LOWER_SIDE: 
                maxWidth=oldR.x+oldR.width;
                editX=true;
                break;
        }
        switch(operation){
            case LOWER_SIDE:   // |                   
            case LL_CORNER:    // |
            case LR_CORNER:    // V                 
                r.height+=delta.y; 
            case RIGHT_SIDE:
            default:
                maxHeight=max.y-r.y;
                break;
                
            case UPPER_SIDE:   // ^                  
            case UL_CORNER:    // |               
            case UR_CORNER:    // |                 
                r.height-=delta.y;   
            case LEFT_SIDE:                      
                maxHeight=oldR.y+oldR.height;
                editY=true;
                break;        
        }
               
        //Calc possible range for width and height
        Rectangle range=placeRectangle(maxWidth, maxHeight, ratio);
            
        //Calc ratio
        if(ratio!=NONRATIO){
            switch(operation){
                case LEFT_SIDE:
                case UL_CORNER:
                case RIGHT_SIDE:
                case LR_CORNER:
                    r.height=(int)Math.floor(r.width/ratio);
                default:
                    r.width=(int)Math.floor(r.height*ratio);
            }
        }
        
        //Overflow
        if(r.width > range.width){
            r.width=range.width;
            if(ratio!=NONRATIO)
                r.height=range.height;
        }
        if(r.height > range.height){
            r.height=range.height;
            if(ratio!=NONRATIO)
                r.width=range.width;        
        }

        //Underflow 
        if(r.width<=0)
            r.width=1;
        if(r.height<=0)
            r.height=1;
        

        if(editX)
            r.x=oldR.x + oldR.width-r.width;
        
        if(editY)
            r.y=oldR.y + oldR.height-r.height;
        
        return r;
    }
    
    /**
     * Returns the proper Cursor for the given area! 
     * @param area
     * @return 
     */
    public static Cursor getCursor(EArea area){
        switch(area){
            case CENTER:                      
              return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
            case RIGHT_SIDE:                      
              return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
            case LEFT_SIDE:                      
              return Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
            case UPPER_SIDE:                      
              return Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
            case LOWER_SIDE:                      
              return Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
            case LL_CORNER:                      
              return Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
            case LR_CORNER:                      
              return Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
            case UL_CORNER:                      
              return Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
            case UR_CORNER:                      
              return Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
            default:
              return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        }
    }
    
    /**
     * Bounds the point in the Rectangle!
     * @param t_pos
     * @param rect
     * @return 
     */
    public static Point limitPointInRect(Point t_pos, Rectangle rect){
        Point pos=t_pos.getLocation();
        if(pos.x<rect.x)
            pos.x=rect.x;
        if(pos.y<rect.y)
            pos.y=rect.y;
        if(pos.x>rect.x+rect.width)
            pos.x=rect.x+rect.width;
        if(pos.y>rect.y+rect.height)
            pos.y=rect.y+rect.height;
        return pos;
    }
    
    /**
     * Draws a Rectangle with a dashed Line
     * @param g
     * @param r
     * @param c1
     * @param c2 
     */
    public static void drawDashedLine(Graphics g, Rectangle r, Color c1, Color c2){
        g.setColor(c1);
        g.drawRect(r.x, r.y, r.width, r.height);
        if(g instanceof Graphics2D){
            ((Graphics2D)g).setStroke(new BasicStroke(1,BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,10.0f,new float[]{ 5f }, 0f));
            g.setColor(c2);
            g.drawRect(r.x, r.y, r.width, r.height);
            ((Graphics2D)g).setStroke(new BasicStroke());
        }        
    }
    
    /**Returns a Rectangle witch contains all rectangles */
    public static Rectangle compRectangles(Rectangle[] rects){
        Rectangle ret= new Rectangle(-1, -1);
        for(Rectangle rect: rects)
            ret.add(rect);
        return ret;
    }


    /**Converts a Dimension to a Point x=width y=height*/
    public static Point dimensiontoPoint(Dimension d){
        return new Point(d.width, d.height);
    }
    
    /** Scale the element Rectangle*/
    public static Rectangle scaleRectangleInRectangle(Rectangle element, Rectangle r_old, Rectangle r_new){
        if (r_new==null)
            return element;
        
        Rectangle inner=new Rectangle(element.x-r_old.x, element.y-r_old.y, element.width, element.height);
        
        double s_x=(double)r_new.width/r_old.width;
        double s_y=(double)r_new.height/r_old.height;
        
        int width=(int)(inner.width*s_x);
        int height=(int)(inner.height*s_y);
        if (width<=0)
            width=1;
        if (height<=0)
            height=1;
        
        inner= new Rectangle((int)(inner.x*s_x),(int)(inner.y*s_y),width,height);

        return new Rectangle(inner.x+r_new.x, inner.y+r_new.y, inner.width, inner.height);
    }
    
    public static Rectangle scaleRectangleandPos(Rectangle r, Point center, double scale){
        return new Rectangle((int)(r.x*scale)+center.x, (int)((r.y)*scale)+center.y,(int)(r.width*scale), (int)(r.height*scale));
    }
    
    
    public static boolean doubleequals(double a, double b, double epsilon)
    {
        double f= a-b;
        if(f <= ( 0 - epsilon ) )
            return false;
        if(f >= ( 0 + epsilon ) )
            return false;
        return true;
    }
    
    public static boolean doubleequals(double a, double b)
    {
        double epsilon = 0.0000001;
        return doubleequals(a, b, epsilon);
    }
    
    public static double getdistance(double x1, double y1, double x2, double y2)
    {
        return Math.hypot(x1-x2, y1-y2);
    }

    private Geometrics(){
        throw new AssertionError();
    }
}
