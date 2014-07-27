/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller.communication;

import javax.swing.SwingUtilities;

/**
 *
 * @author patrick
 */
public class ComCoreSIM extends AComCore{
    
    public ComCoreSIM(IReceivedLines resivedlines, IDisconnect disconnect) {
        super(resivedlines, disconnect);
    }

    @Override
    public boolean isSimulation() {
        return true;
    }

    @Override
    public void send(final String line) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                internal_receivedEvent(new String[]{"ok command: " + line});
            }
        });
    }

    @Override
    public void internal_discharge() {
        //nothing to discharge!
    }
    
}
