/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import java.util.EventListener;

/**
 *
 * @author Patrick
 */
public interface JPPaintableListener extends EventListener {
    public void paintComponent(JPPaintableEvent evt); 
}
