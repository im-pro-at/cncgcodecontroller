/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import gnu.io.NRSerialPort;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.SwingUtilities;

/**
 *
 * @author patrick
 */
public class Communication {
    /*
     * Singelton
     */
    private static final Communication singelton=new Communication();
    
    public static Communication getInstance(){
        return singelton;
    }
    
    public interface IResivedLines
    {
        void resived(String[] lines);
    }
    public interface ISend
    {
        void send(String cmd);
    }
    
    /*
     * Class:
     */
    private Timer resivetimer;
    String lastserialstring="";
    long linecount=0;
    long sendlinecount=0;
    private ArrayList<IEvent> changed=new ArrayList<>();
    private ArrayList<IResivedLines> resived=new ArrayList<>();
    private ArrayList<ISend> send=new ArrayList<>();
    private NRSerialPort sp=null;
    private String status="Not Connected!";
    private InputStream is;
    private OutputStream os;
    
    
    private Communication()
    {
        //Resive Task:
        resivetimer=new Timer();
        resivetimer.schedule(new TimerTask(){
            @Override
            public void run() {
                recive();
            }
        }, 10, 10 );
    }
 
    private synchronized void doUpdate()
    {
        for(final IEvent e:changed)
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    e.fired();
                }
            });
    }

    private synchronized void recive() {
        if(isConnect())
        {
            try {
                if(is.available()==0)
                    return;
            } catch (IOException ex) {
                    status = ex.toString();
                    sp.disconnect();
                    sp=null;
                    doUpdate();
                    return;
            }
            
            String input = "";
            while (true) {
                try {
                    int c = is.read();
                    if (c != -1) {
                        input = input + ((char) c);
                    } else {
                        break;
                    }
                } catch (Exception ex) {
                    status = ex.toString();
                    sp.disconnect();
                    sp=null;
                    doUpdate();
                    return;
                }
            }
            
            ArrayList<String> inputs= new ArrayList<>();
            inputs.add("");
            for(char c:input.toCharArray())
            {
                if(c=='\n')
                    inputs.add("");
                else
                    inputs.set(inputs.size()-1, inputs.get(inputs.size()-1)+c);
            }

            inputs.set(0,lastserialstring+inputs.get(0));
            lastserialstring= inputs.get(inputs.size()-1);            
            
            if(inputs.size()>1)
            {
                linecount+=inputs.size()-1;
                notify();
                
                final String[] lines= new String[inputs.size()-1];
                for(int i=0; i < inputs.size()-1;i++)
                    lines[i]=inputs.get(i);
            
                for(final IResivedLines e:resived)
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            e.resived(lines);
                        }
                    });
            }
            
        }
    }
    
    
    public synchronized void addChangedEvent(IEvent e)
    {
        changed.add(e);
    }
    public synchronized void addResiveEvent(IResivedLines e)
    {
        resived.add(e);
    }
    public synchronized void addSendEvent(ISend e)
    {
        send.add(e);
    }
    
    public synchronized boolean isConnect()
    {
        if(sp==null)
            return false;
        return sp.isConnected();
    }
    
    public synchronized void disconnect()
    {
        if(!isConnect())
        {
            doUpdate();
            return;
        }
        sp.disconnect();
        sp=null;
        status="Disconneded!";
        doUpdate();
    }

    public synchronized void connect(String port, int speed) {
        if(isConnect())
        {
            doUpdate();
            return;
        }

        lastserialstring="";
        linecount=0;
        sendlinecount=0;

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
        doUpdate();
    }

    public synchronized boolean isbussy()
    {
        return sendlinecount>linecount;
    }
    
    public synchronized void send(final String command) {
        
        try {
            while(sendlinecount>linecount)
                wait();
            sendlinecount=linecount+1; //Block till answer!
        
            os.write((command+"\n").getBytes());
            
            for(final ISend e:send)
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        e.send(command);
                    }
                });
            
        } catch (Exception ex) {
                status = ex.toString();
                sp.disconnect();
                sp=null;
                doUpdate();
        }
    }

    public synchronized String getStatus() {
        return status;
    }
}
