/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller.communication;

import gnu.io.NRSerialPort;
import gnu.io.SerialPort;
import java.io.Console;
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
    private final NRSerial serialPort;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final Timer receiveTimer;

    private String lastSerialString = "";
    
    public ComCoreNRSerialPort(IReceivedLines received_lines,
                                IDisconnect disconnect,
                                String port,
                                int speed) throws Exception 
    {
        super(received_lines, disconnect);
        
        serialPort = new NRSerial(port, speed);
        if (serialPort.connect() == false) 
        {
            throw new Exception("Cannot connect to selected port!");
        }
                
        inputStream     = serialPort.getInputStream();
        outputStream    = serialPort.getOutputStream();
        
        receiveTimer = new Timer() {
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
                int c = inputStream.read();
                if (c != -1) 
                {
                    input = input + ((char) c);
                } else 
                {
                    break;
                }
            }

            ArrayList<String> inputs = new ArrayList<>();
            inputs.add("");
            for(char c:input.toCharArray())
            {
                if ((c == '\n')||(c == '\r'))
                {
                    inputs.add("");
                }
                else
                {
                    inputs.set(inputs.size() - 1, inputs.get(inputs.size() - 1) + c);
                }
            }

            inputs.set(0,lastSerialString+inputs.get(0));
            lastSerialString = inputs.get(inputs.size()-1);  
            
            if(inputs.size() > 1)
            {
                final String[] lines = new String[inputs.size() - 1];
                for(int i = 0; i < inputs.size() - 1;i++)
                {
                    lines[i] = inputs.get(i);
                }
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
        {
            line += "\n";
        }
        try {
            outputStream.write((line).getBytes());
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
        receiveTimer.cancel();
        try {
            if(serialPort.isConnected())
            {
                serialPort.disconnect();
            }
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
