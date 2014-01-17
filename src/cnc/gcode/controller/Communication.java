/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import gnu.io.NRSerialPort;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
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
    
    public static ArrayList<String> getPortsNames()
    {
        return new ArrayList<String>(NRSerialPort.getAvailableSerialPorts()){
            {
                this.add("SIM");
            }
        };                
    }
    
    /*
     * Class:
     */
    private boolean simulation=false;
    private Timer resivetimer;
    private String lastserialstring="";
    private LinkedList<String> cmdhistroy= new LinkedList<>();
    private int resivecount=0;
    private ArrayList<IEvent> changed=new ArrayList<>();
    private ArrayList<IResivedLines> resived=new ArrayList<>();
    private ArrayList<ISend> send=new ArrayList<>();
    private NRSerialPort sp=null;
    private String status="Not Connected!";
    private InputStream is;
    private OutputStream os;
    private boolean startlinefound=false;    
    
    private Communication()
    {
        //Resive Task:
        resivetimer=new Timer();
        resivetimer.schedule(new TimerTask(){
            @Override
            public void run() {
                try {
                    recive();
                } catch (Exception ex) {
                    status = ex.toString();
                    if(sp!=null)    
                        sp.disconnect();
                    sp=null;
                    status="Communication Error! ("+ex+")";
                    ex.printStackTrace();
                    doUpdate();
                }
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

    private synchronized void recive() throws Exception {
        if(isConnect())
        {
            String input = "";
            if(simulation)
            {
               if(cmdhistroy.size()>resivecount) 
                   input="ok nr="+resivecount+" command: "+cmdhistroy.get(resivecount)+"\n";
            }
            else
            {
                while (true) 
                {
                    try {
                        int c = is.read();
                        if (c != -1) {
                            input = input + ((char) c);
                        } else {
                            break;
                        }
                    } catch (Exception ex) {
                        status = ex.toString();
                        if(sp!=null)    
                            sp.disconnect();
                        sp=null;
                        doUpdate();
                        return;
                    }
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
            
            if(inputs.size()>1)
            {
                int rs=0;
                for(String temp:inputs)
                {
                    //is it OK?
                    if(temp.length()>=2 && temp.substring(0, 2).equals("ok"))
                    {
                        resivecount++;
                        if(resivecount>cmdhistroy.size())
                        {
                            status = "More OK then send commands!";
                            if(sp!=null)    
                                sp.disconnect();
                            sp=null;
                            doUpdate();
                        }
                            
                    }
                    // "start" line after reset
                    if(temp.length()>=5 && temp.substring(0, 5).equals("start"))
                    {
                        startlinefound=true;
                        // reset counter
                        resivecount=0;
                        cmdhistroy.clear();
                    }
                    //resend?
                    if(temp.length()>=2 && temp.substring(0, 2).equals("rs"))
                    {
                        rs=Integer.parseInt(temp.substring(3));
                    }
                    if(temp.length()>=7 && temp.substring(0, 7).equals("Resend:"))
                    {
                        resivecount--; //Marlin ok will be comming anyway :-(
                        rs=Integer.parseInt(temp.substring(7));
                        if(rs<cmdhistroy.size()-1)
                        {
                            status = "Resend of old command!";
                            if(sp!=null)
                                sp.disconnect();
                            sp=null;
                            doUpdate();
                        }
                    }
                }

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

                if(rs>0)
                {
                    if(rs!=1)
                        resend(rs);
                    else
                        throw new MyException("Resend 1! Controller reset?!");
                }
                else
                {
                    //Notify waiting Threads:
                    notify();
                }
            }
            
        }
        wait(0,1);
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
        if(simulation)
            return true;
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
        if(simulation)
        {
            simulation=false;
        }
        else
        {
            sp.disconnect();
            sp=null;
        }
        status="Disconneded!";
        doUpdate();
        
        notifyAll();
    }

    public synchronized void connect(String port, int speed) {
        if(isConnect())
        {
            doUpdate();
            return;
        }

        lastserialstring="";
        cmdhistroy= new LinkedList<>();
        resivecount=0;

        try
        {
            if(port.equals("SIM"))
            {
                simulation=true;
            }
            else
            {
                startlinefound=false; // reset flag
                sp = new NRSerialPort(port, speed);
                if(sp.connect()==false)
                {
                    status="Cannot connect!";
                    return;
                }
                
                is = sp.getInputStream();
                os = sp.getOutputStream();
             
                // Wait for "start" line from serial input to dedect Marlin reset
                for (int i=0;i<20;i++)
                {
                    if (startlinefound==true) { break; }
                    wait(100);
                }

            }
            
            //Send M110 to reset checksum
            send("M110");

            //3 secound Timout for answer
            for (int i=0;i<30;i++)
            {
                wait(100);
                if (!isbussy()) break;
            }
            if(isbussy())
                //Printer not answered!
                throw new MyException("Printer did not respons! Try to replug USB cable.");
        }
        catch(Exception e)
        {
            if(sp!=null)    
                sp.disconnect();
            sp=null;
            status=e.toString();
            return;
        }
        status="Connected!";
        doUpdate();
    }

    public synchronized boolean isbussy()
    {
        return !isConnect() || cmdhistroy.size()>resivecount;
    }
    
    public synchronized void send(String command) {
        if(!isConnect())
        {
            doUpdate();
            return;
        }
        
        //No command
        if(command.trim().equals(""))
            return;
        
        command=command.replace('*', ' ');
        
        while(cmdhistroy.size()>resivecount)
        {
            try {
                wait();
            } catch (InterruptedException ex) { 
                return;
            }
        }

        if(!isConnect())
            return;

        cmdhistroy.add(command);        
        
        resend(cmdhistroy.size());

    }
    
    private void resend(int rs)
    {
        try {
           //Cecksum:
            if(rs>cmdhistroy.size())
            {
                status = "Resend bigger then send history!";
                if(sp!=null)    
                    sp.disconnect();
                sp=null;
                doUpdate();
            }
            String command=cmdhistroy.get(rs-1);
            command="N"+cmdhistroy.size()+" "+command+" *";
            byte cs = 0;
            byte[] b=command.getBytes();
            for(int i = 0;  b[i]!= '*' && i<b.length; i++)
               cs = (byte)(cs ^ b[i]);
            command+=""+cs;

            if(!simulation)
                os.write((command+"\n").getBytes());

            final String ccommand=cmdhistroy.get(rs-1);
            for(final ISend e:send)
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        e.send(ccommand);
                    }
                });        

        } catch (Exception ex) {
            status = ex.toString();
            if(sp!=null)    
                sp.disconnect();
            sp=null;
            doUpdate();
        }
    }
    
    public boolean isSimulation()
    {
        return simulation;
    }
    

    public synchronized String getStatus() {
        return status;
    }
}
