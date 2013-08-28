/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

/**
 *
 * @author patrick
 */
public class SendListElement {
    public enum EType {

        IN, OUT
    }

    EType type;
    String s;
        
    public SendListElement(String s, EType type) {
        this.s=s;
        this.type=type;
    }

    public String getText()
    {
            return s;
    }
    
    @Override
    public String toString() {
        return (this.type== EType.IN?"-->":"<--") +" "+ this.s;
    }    
}
