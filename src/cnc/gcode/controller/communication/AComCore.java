/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller.communication;

import java.util.ArrayList;
/**
 *
 * @author patrick
 */
abstract class AComCore {
    private boolean connected = true;
    private final IReceivedLines resivedlines;
    private final IDisconnect disconnect;

    public AComCore(IReceivedLines resivedlines, IDisconnect disconnect) {
        this.resivedlines   = resivedlines;
        this.disconnect     = disconnect;
    }

    public abstract boolean isSimulation();

    public abstract void send(String line);

    
    //Use only internal!!
    protected void internal_receivedEvent(String[] lines) {
        resivedlines.received(lines);
    }
    protected void internal_disconnectedEvent(final String status) {
        connected   = false;
        disconnect.disconnect(status);
    }
    public abstract void internal_discharge();
    
    
    public boolean isConnected(){
        return connected;
    }

    //Call at end of life to free comport (will not generate disconnect event!)
    public void discharge(){
        connected = false;
        internal_discharge();
    }    
    
    
    public static ArrayList<String> getPortsNames(){
        return new ArrayList<String>(){
            {
                this.addAll(ComCoreNRSerialPort.getPortsNames());
                this.add("SIM");
            }
        };
    }
    public static ArrayList<Integer> getPortsSpeeds(){
        return ComCoreNRSerialPort.getPortsSpeeds();
    }
    
    public static AComCore openPort(IReceivedLines resivedlines, IDisconnect disconnect, String name, int speed) throws Exception{
        if(name.equals("SIM"))
            return new ComCoreSIM(resivedlines, disconnect);
        else
            return new ComCoreNRSerialPort(resivedlines, disconnect, name, speed);
    }

    
}
