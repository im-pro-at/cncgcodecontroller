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
import jssc.SerialPort;
import jssc.SerialPortList;

//Core for communication

class ComCoreJSSC extends AComCore {
    private final SerialPort serialPort;
    private final Timer receiveTimer;

    private String lastSerialString = "";
    
    public ComCoreJSSC(IReceivedLines received_lines,
                                IDisconnect disconnect,
                                String port,
                                int speed) throws Exception 
    {
        super(received_lines, disconnect);
        serialPort = new SerialPort(port);
        if (!serialPort.openPort())        
        {
            throw new Exception("Cannot connect to selected port!");
        }
        if (!serialPort.setParams(speed,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE))
        {
            throw new Exception("Cannot set parameters for selected port!");
        }
        
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
            String input=serialPort.readString();
            if(input==null)
                input="";
            
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
            serialPort.writeString(line);
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
            if(serialPort.isOpened())
            {
                serialPort.closePort();
            }
        } catch (Exception ex) {
            Logger.getLogger(ComCoreJSSC.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static ArrayList<String> getPortsNames(){
        return new ArrayList<String>(Arrays.asList(SerialPortList.getPortNames())); 
    }
    public static ArrayList<Integer> getPortsSpeeds(){
        return new ArrayList<Integer>(Arrays.asList(new Integer[]{110,300,600,1200,4800,9600,14400,19200,38400,57600,115200,128000,250000,256000}));
    }
}
