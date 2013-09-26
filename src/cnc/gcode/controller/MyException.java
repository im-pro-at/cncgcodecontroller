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

    Object o;
    
    public MyException(String message) {
        super(message);
    }

    public MyException(String message, Object o) {
        super(message);
        this.o = o;
    }

    public Object getO() {
        return o;
    }

    
    @Override
    public String toString() {
        return getLocalizedMessage();
    }
    
}
