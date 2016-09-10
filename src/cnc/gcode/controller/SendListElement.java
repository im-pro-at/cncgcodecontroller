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

        IN, OUT, INFO
    }

    EType type;
    String s;
        
    public SendListElement(String s, EType type) {
        this.s      = s;
        this.type   = type;
    }

    public String getText()
    {
            return s;
    }
    
    @Override
    public String toString() {
        switch (type){
            case IN:
                return "--> "+ this.s;
            case OUT:
                return "<-- "+ this.s;
            default:
            case INFO:
                return "INFO: "+ this.s;
        }
    }    
}
