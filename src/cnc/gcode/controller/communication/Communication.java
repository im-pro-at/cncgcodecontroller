/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller.communication;

import cnc.gcode.controller.Database;
import cnc.gcode.controller.IEvent;
import cnc.gcode.controller.MyException;
import cnc.gcode.controller.Tools;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 *
 * @author patrick
 */
public enum Communication {
    MARLIN{
        private LinkedList<String> cmdhistroy= new LinkedList<>();
        private int resivecount=0;

        @Override
        protected void internal_connect() throws MyException,InterruptedException{
            //try 3 times to get answare:
            for (int t=0;t<3;t++){
                internal_reset();

                //Send M110 to reset checksum
                internal_send("M110");
                doSendEvent("M110");

                //5 secound Timout for answer
                for (int i=0;i<50;i++){
                    Communication.class.wait(100);       //give data the possibility  to enter internal_receive!
                    if(!core.isConnected())
                        throw new MyException("Lost connection! ("+status+")");
                    if (resivecount>0) 
                        break;
                }

                if(resivecount>0)
                    break;
                Communication.doChangedEvent("Printer not answered to M110! Retry ...");
            }

            //Printer not answered!
            if(resivecount==0)
                throw new MyException("Printer did not respons! Try to replug USB cable.");

            //SET absolute
            internal_send("G90");
            doSendEvent("G90");
            
            //2 secound Timout for answer
            synchronized(Communication.class){
                for (int i=0;i<20;i++){
                    Communication.class.wait(100);       //give data the possibility to enter internal_receive!
                    if(!core.isConnected())
                        throw new MyException("Lost connection!");
                    if (!internal_isbusy()) 
                        break;
                }
            }
            if(internal_isbusy())
                throw new MyException("Printer did not answered to G90!");
        }
        
        @Override
        protected void internal_reset(){
            cmdhistroy= new LinkedList<>();
            resivecount=0;
        }
       
        @Override
        protected void internal_send(String command) throws MyException{
            cmdhistroy.add(command);        
            resend(cmdhistroy.size());
        }

        @Override
        protected void internal_receive(String line) throws MyException{
            int rs=0;
            if(line.length()>=2 && line.substring(0, 2).equals("ok"))
            {
                resivecount++;
            
                if(resivecount>cmdhistroy.size())
                    throw new MyException("More OK then send commands!");
 
                Communication.class.notify();
            }
            // "start" line after reset
            if(line.length()>=5 && line.substring(0, 5).equals("start"))
            {
                if(!initThread.isAlive())
                    throw new MyException("Controller reset detected!");
            }
            //resend?
            if(line.length()>=2 && line.substring(0, 2).equals("rs"))
            {
                rs=Integer.parseInt(line.substring(3));
            }
            if(line.length()>=7 && line.substring(0, 7).equals("Resend:"))
            {
                resivecount--; //Marlin ok will be comming anyway :-(
                try{
                    rs=Integer.parseInt(line.substring(7).trim());
                }
                catch(Exception ex){
                    throw new MyException("Resend String Error! ("+ex.getMessage()+")");
                }
                if(rs<cmdhistroy.size()-1)
                    throw new MyException("Resend of old command!");
            }

            if(rs>0)
            {
                if(rs!=1)
                    resend(rs);
                else
                    throw new MyException("Resend 1! Controller reset?!");
            }

        }
        @Override
        protected boolean internal_isbusy(){
            return cmdhistroy.size()>resivecount;
        }
        @Override
        protected boolean internal_isConnected(){
            return resivecount!=0;            
        }
        @Override
        protected Double internal_ZEndStopHit(String line){
            if(line.contains("endstops") && line.contains("hit") && line.contains("Z:")){
                try {
                    return Tools.strtod(line.substring(line.indexOf("Z:")+2));
                } catch (ParseException ex) {
                    Logger.getLogger(Communication.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return null;
        }
        
        private void resend(int rs) throws MyException {
            //Cecksum:
            if(rs>cmdhistroy.size())
                throw new MyException("Resend bigger then send history!");

            String command=cmdhistroy.get(rs-1);
            command="N"+cmdhistroy.size()+" "+command+" *";
            byte cs = 0;
            byte[] b=command.getBytes();
            for(int i = 0;  b[i]!= '*' && i<b.length; i++)
               cs = (byte)(cs ^ b[i]);
            command+=""+cs;

            doSendEvent("   "+rs+"=>["+command+"]");
            core.send(command);

        }
        
        
    },
    GENERIC{
        private int sendcount=0;
        private int resivecount=0;

        @Override
        protected void internal_connect() throws MyException,InterruptedException{
            internal_reset();

            //SET absolute
            internal_send("G90");
            doSendEvent("G90");
            
            //2 secound Timout for answer
            synchronized(Communication.class){
                for (int i=0;i<20;i++){
                    Communication.class.wait(100);       //give data the possibility to enter internal_receive!
                    if(!core.isConnected())
                        throw new MyException("Lost connection!");
                    if (!internal_isbusy()) 
                        break;
                }
            }
            if(internal_isbusy())
                throw new MyException("Printer did not answered to G90!");
        }
        
        @Override
        protected void internal_reset(){
            sendcount=0;
            resivecount=0;
        }
       
        @Override
        protected void internal_send(String command) throws MyException{
            core.send(command);
            sendcount++;
        }

        @Override
        protected void internal_receive(String line) throws MyException{
            int rs=0;
            if(line.length()>=2 && line.substring(0, 2).equals("ok"))
            {
                resivecount++;
            
                if(resivecount>sendcount)
                    throw new MyException("More OK then send commands!");
 
                Communication.class.notify();
            }
            // "start" line after reset
            if(line.length()>=5 && line.substring(0, 5).equals("start"))
            {
                if(!initThread.isAlive())
                    throw new MyException("Controller reset detected!");
            }
        }
        @Override
        protected boolean internal_isbusy(){
            return sendcount>resivecount;
        }
        @Override
        protected boolean internal_isConnected(){
            return resivecount!=0;            
        }
        @Override
        protected Double internal_ZEndStopHit(String line){
            if(line.contains("endstops") && line.contains("hit") && line.contains("Z:")){
                try {
                    return Tools.strtod(line.substring(line.indexOf("Z:")+2));
                } catch (ParseException ex) {
                    Logger.getLogger(Communication.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return null;
        }
        
    },
    ;
  
    //internal Function (have to be overrided
    abstract void internal_connect() throws MyException,InterruptedException;      //No Exception means connected   
    abstract void internal_reset();           //Resets Variables
    abstract void internal_send(String command) throws MyException;            //Is Called if somthing will be send
    abstract void internal_receive(String line) throws MyException;         //Is Called if something is resived
    abstract Double internal_ZEndStopHit(String line);      //is called on each line to detect endstopHit of Zacess
    abstract boolean internal_isbusy();          //Return if busy
    abstract boolean internal_isConnected();     //Returns if Connected
    
    
    private static Thread initThread=new Thread();
    private static AComCore core= new ComCoreDummy();
    private static String status="Not Connected!";
    private static ArrayList<IEvent> changed=new ArrayList<>();
    private static ArrayList<IResivedLines> resived=new ArrayList<>();
    private static ArrayList<IEndstopHit> hitendstop=new ArrayList<>();
    private static ArrayList<ISend> send=new ArrayList<>();

    public static void addChangedEvent(IEvent e){
        changed.add(e);
    }
    
    public static void addResiveEvent(IResivedLines e){
        resived.add(e);
    }
    public static void addZEndstopHitEvent(IEndstopHit e){
        hitendstop.add(e);
    }
    public static void addSendEvent(ISend e){
        send.add(e);
    }
    
    public static synchronized void connect(final String port, final int speed) {
        //allready connected
        if(isConnected()){
            doChangedEvent(status);
            return;
        }
            
        //allready running init
        if(core.isSimulation() && initThread.isAlive())
            return; 
        
        //Prepair connection
        resetall();
        
        doChangedEvent("Connecting ... ");
        
        //Do not block GUI:
        initThread=new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized(Communication.class){
                    
                    //Try to open Port
                    try {
                        core=AComCore.openPort(new IResivedLines() {
                            @Override 
                            public void resived(String[] lines) {
                                Communication.receive(lines);
                            }
                        }, new IDisconnect() {
                            @Override
                            public void disconnect(String status) {
                                synchronized(Communication.class){
                                    resetall();
                                    doChangedEvent(status);
                                }
                            }
                        }, port, speed);
                    } catch (Exception ex) {
                        resetall();
                        doChangedEvent(ex.getMessage());
                        return;
                    }
                    try {
                        //Run connect
                        I().internal_connect();
                        
                    } catch (InterruptedException | MyException ex) {
                        Logger.getLogger(Communication.class.getName()).log(Level.SEVERE, null, ex);
                        resetall();
                        doChangedEvent(ex.getMessage()==null?status:ex.getMessage());
                        return;
                    }
                    
                    doChangedEvent("Connected!");
                    Communication.class.notify();
                }
            }
        });
        initThread.start();
        
        //Wait for initThread to be started
        while(initThread.getState()==Thread.State.NEW) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                Logger.getLogger(Communication.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
    
    public static synchronized void disconnect(){
        resetall();
        doChangedEvent("Disconneded!");
    }
    
    public static synchronized void send(String command) throws ComInterruptException {
        if(!isConnected()){
            disconnect();
            throw new ComInterruptException("Not connected !"); 
        }
        //No command
        if(command.trim().equals(""))
            return;
        
        while((I().internal_isbusy() || initThread.isAlive()) && isConnected())
        {
            try {
                Communication.class.wait();
            } catch (InterruptedException ex) { 
                throw new ComInterruptException("Interrupt!"); 
            }
        }
        if(!isConnected())
            throw new ComInterruptException("Not connected !"); 
        
        doSendEvent(command);
        
        try {
            I().internal_send(command);
        } catch (MyException ex) {
            Logger.getLogger(Communication.class.getName()).log(Level.SEVERE, null, ex);
            resetall();
            doChangedEvent(ex.getMessage());
            throw new ComInterruptException(ex.getMessage());
        }
    }
    
    public static synchronized boolean isConnected(){
        return core.isConnected() && I().internal_isConnected() && !initThread.isAlive();
    }
    
    public static synchronized boolean isbussy(){
        return !isConnected() || I().internal_isbusy();
    }

    public static synchronized boolean isSimulation(){
        return core.isSimulation();
    }
    
    public static synchronized String getStatus() {
        return status;
    }
    
    public static ArrayList<String> getPortsNames(){
        return AComCore.getPortsNames();
    }
    
    public static ArrayList<Integer> getPortsSpeeds() {
        return AComCore.getPortsSpeeds();
    }
                
    //Internal Function to call
    private static Communication I(){
        return valueOf(Database.COMTYPE.get());
    }

    private static synchronized void receive(String[] lines) {
        doResiveEvent(lines);
        
        for(String line:lines){
            try {
                I().internal_receive(line);
            } catch (MyException ex) {
                Logger.getLogger(Communication.class.getName()).log(Level.SEVERE, null, ex);
                resetall();
                doChangedEvent(ex.getMessage());
                return;
            }
            
            Double val=I().internal_ZEndStopHit(line);
            if(val!=null)
                doEnstopHitEvent(val);
        }
    }
    
    private static void resetall(){
        core.discharge();
        core= new ComCoreDummy();
        for(Communication c:Communication.values())
            c.internal_reset();
        initThread.interrupt();
        Communication.class.notifyAll();
    }
    
    private static void doChangedEvent(String status)
    {
        Communication.status=status;
        for(final IEvent e:changed)
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    e.fired();
                }
            });
    }
    
    private static void doResiveEvent(final String[] lines)
    {
        for(final IResivedLines e:resived)
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    e.resived(lines);
                }
            });
    }
    private static void doEnstopHitEvent(final double value)
    {
        for(final IEndstopHit e:hitendstop)
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    e.endStopHit(value);
                }
            });
    }
    private static void doSendEvent(final String cmd)
    {
        for(final ISend e:send)
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    e.send(cmd);
                }
            });
    }
    
    @Override
    public String toString() {
        return name();
    }
    
}
