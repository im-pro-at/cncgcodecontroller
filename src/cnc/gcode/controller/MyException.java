/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

/**
 *
 * @author patrick
 */
public class MyException extends Exception{

    public MyException(String message) {
        super(message);
    }

    
    @Override
    public String toString() {
        return getLocalizedMessage();
    }
    
}
