/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

/**
 *
 * @author patrick
 */
public interface IGUIEvent {

    public void setGUIEvent(IEvent event);

    public void updateGUI(boolean serial, boolean isworking, boolean isleveled);
    
}
