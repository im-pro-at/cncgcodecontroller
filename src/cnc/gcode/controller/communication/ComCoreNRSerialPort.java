/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller.communication;

import gnu.io.NRSerialPort;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

//Core for kommunikation

class ComCoreNRSerialPort extends AComCore {
    private final NRSerialPort sp;
    private final InputStream is;
    private final OutputStream os;
    private final Timer resivetimer;

    private String lastserialstring="";
    
    public ComCoreNRSerialPort(IResivedLines resivedlines, IDisconnect disconnect, String port, int speed) throws Exception {
        super(resivedlines, disconnect);
        
        sp = new NRSerialPort(port, speed);
        
        if (sp.connect() == false) {
            throw new Exception("Cannot connect to selected port!");
        }
        is = sp.getInputStream();
        os = sp.getOutputStream();
        
        resivetimer = new Timer() {
            {
                this.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        check();
                    }
                }, 1, 1);
            }
        };
    }

    private void check() {
        try {
            String input = "";

            while (true) {
                int c = is.read();
                if (c != -1) {
                    input = input + ((char) c);
                } else {
                    break;
                }
            }

            ArrayList<String> inputs= new ArrayList<>();
            inputs.add("");
            for(char c:input.toCharArray())
            {
                if ((c=='\n')||(c=='\r'))
                    inputs.add("");
                else
                    inputs.set(inputs.size()-1, inputs.get(inputs.size()-1)+c);
            }

            inputs.set(0,lastserialstring+inputs.get(0));
            lastserialstring= inputs.get(inputs.size()-1);  
            
            if(inputs.size()>1){
                final String[] lines= new String[inputs.size()-1];
                for(int i=0; i < inputs.size()-1;i++)
                    lines[i]=inputs.get(i);
                internal_receivedEvent(lines);
            }
        } catch (Exception ex) {
            internal_disconnectedEvent("Communication Error! (" + ex + ")");
            ex.printStackTrace();
            internal_discharge();
        }
    }

    @Override
    public void send(String line) {
        if(!line.endsWith("\n"))
            line+="\n";
        try {
            os.write((line).getBytes());
        } catch (Exception ex) {
            internal_disconnectedEvent("Communication Error! (" + ex + ")");
            internal_discharge();
        }
    }

    @Override
    public boolean isSimulation() {
        return false;
    }
        


    @Override
    public void internal_discharge() {
        resivetimer.cancel();
        try {
            if(sp.isConnected())
                sp.disconnect();
        } catch (Exception ex) {
            Logger.getLogger(ComCoreNRSerialPort.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static ArrayList<String> getPortsNames(){
        return new ArrayList<String>(NRSerialPort.getAvailableSerialPorts());                
    }

    public static ArrayList<Integer> getPortsSpeeds(){
        return new ArrayList<Integer>(Arrays.asList(new Integer[]{2400, 4800, 9600, 14400, 19200, 28800, 38400, 57600, 76800, 115200, 230400}));
    }
}
