/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import java.awt.Graphics;
import java.util.EventObject;


/**
 *
 * @author Patrick
 */
public class JPPaintableEvent extends EventObject {

    Graphics g;
    
    public JPPaintableEvent(JPPaintable source, Graphics g) {
        super(source);
        this.g = g;
    }
    
    public Graphics getGaraphics(){
        return g;
    }
}
