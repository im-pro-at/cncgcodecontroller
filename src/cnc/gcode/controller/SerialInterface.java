/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import gnu.io.NRSerialPort;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author patrick
 */
public class SerialInterface {
    
    private Tools.IEvent changed;
    private NRSerialPort sp=null;
    private String status="Not Connected!";
    private InputStream is;
    private OutputStream os;
    
//    void test() throws IOException
//    {
//        Set<String> test= NRSerialPort.getAvailableSerialPorts();
//        
//        for(String x: test)
//        {
//            if(JOptionPane.showConfirmDialog(null,x) == JOptionPane.YES_OPTION)
//            {
//                NRSerialPort sp= new NRSerialPort(x, 115200);
//                sp.connect();
//                InputStream is= sp.getInputStream();
//                OutputStream os= sp.getOutputStream();
//                while(true)
//                {   
//                    int c=is.read();
//                    if(c!=-1)
//                    {
//                        System.out.print((char)c);
//                    }
//                    
//                    if(System.in.available()!=0)
//                    {
//                        c= System.in.read();
//                        if(c!=-1)
//                        {
//                           os.write((char)c);
//                        }
//                    }
//                }
//                
//                //sp.closePort();
//                        
//                
//            }
//        }
//
//    }
    
    public SerialInterface(Tools.IEvent changed)
    {
        this.changed=changed;
    }
 
    public boolean isConnect()
    {
        if(sp==null)
            return false;
        return sp.isConnected();
    }
    
    public void disconnect()
    {
        if(!isConnect())
        {
            changed.fired();
            return;
        }
        sp.disconnect();
        sp=null;
        status="Disconneded!";
    }

    public void connect(String port, int speed) {
        if(isConnect())
        {
            changed.fired();
            return;
        }
        try
        {
            sp = new NRSerialPort(port, speed);
            if(sp.connect()==false)
            {
                status="Cannot connect!";
                return;
            }
            is = sp.getInputStream();
            os = sp.getOutputStream();
        }
        catch(Exception e)
        {
            sp.disconnect();
            sp=null;
            status=e.toString();
        }
        status="Connected!";
    }

    public void send(String command) {
        try {
            os.write(command.getBytes());
        } catch (Exception ex) {
                status = ex.toString();
                sp.disconnect();
                sp=null;
                changed.fired();
        }
    }

    public String get() {
        String s = "";
        while (true) {
            try {
                int c = is.read();
                if (c != -1) {
                    s = s + ((char) c);
                } else {
                    return s;
                }
            } catch (Exception ex) {
                status = ex.toString();
                sp.disconnect();
                sp=null;
                changed.fired();
                return s;
            }
        }
    }

    public String getStatus() {
        return status;
    }
}
