/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller.communication;

/**
 *
 * @author patrick
 */
class ComCoreDummy extends AComCore{

    public ComCoreDummy() {
        super(null, null);
    }

    @Override
    public boolean isSimulation() {
        return false;
    }

    @Override
    public boolean isConnected() {
        return false;
    }
    
    @Override
    public void send(String line) {
        throw new UnsupportedOperationException("Dummy cannot send!");
    }

    @Override
    public void internal_discharge() {
    }
    
}
