/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller.communication;

/**
 *
 * @author patrick
 */
public class ComInterruptException extends Exception{

    Object o;
    
    public ComInterruptException(String message) {
        super(message);
    }

    @Override
    public String toString() {
        return getLocalizedMessage();
    }
    
}