/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller.communication;

import cnc.gcode.controller.DatabaseV2;
import cnc.gcode.controller.IEvent;
import cnc.gcode.controller.MyException;
import cnc.gcode.controller.ObjectProxy;
import cnc.gcode.controller.Tools;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author patrick
 */
public enum Communication {
    MARLIN{
        private LinkedList<String> cmdHistroy = new LinkedList<>();
        private int receiveCount = 0;

        @Override
        protected void internal_connect() throws MyException,InterruptedException{
            //try 3 times to get answare:
            for (int t = 0;t < 3;t++)
            {
                internal_reset();

                //Send M110 to reset checksum
                internal_send("M110");
                doSendEvent("M110");

                //5 secound Timout for answer
                for (int i = 0;i < 50;i++)
                {
                    Communication.class.wait(100);       //give data the possibility  to enter internal_receive!
                    if(!core.isConnected())
                    {
                        throw new MyException("Lost connection! (" + status + ")");
                    }
                    if (receiveCount > 0) 
                    {
                        break;
                    }
                }

                if(receiveCount > 0)
                {
                    break;
                }
                Communication.doChangedEvent("Printer not answering to M110! Retrying ...");
            }

            //Printer not answered!
            if(receiveCount == 0)
            {
                throw new MyException("Printer did not respond! Try to replug USB cable.");
            }

            //SET absolute
            internal_send("G90");
            doSendEvent("G90");
            
            //2 secound Timout for answer
            synchronized(Communication.class)
            {
                for (int i = 0;i < 20;i++)
                {
                    Communication.class.wait(100);       //give data the possibility to enter internal_receive!
                    if(!core.isConnected())
                    {
                        throw new MyException("Lost connection!");
                    }
                    if (!internal_isbusy()) 
                    {
                        break;
                    }
                }
            }
            if(internal_isbusy())
            {
                throw new MyException("Printer did not answer to G90!");
            }
        }
        
        @Override
        protected void internal_reset()
        {
            cmdHistroy = new LinkedList<>();
            receiveCount = 0;
        }
       
        @Override
        protected void internal_send(String command) throws MyException{
            cmdHistroy.add(command);        
            resend(cmdHistroy.size());
        }

        @Override
        protected void internal_receive(String line) throws MyException{
            int rs = 0;
            if(line.length() >= 2 && line.substring(0, 2).equals("ok"))
            {
                receiveCount++;
            
                if(receiveCount > cmdHistroy.size())
                {
                    throw new MyException("More OK than send commands!");
                }
 
                Communication.class.notify();
            }
            // "start" line after reset
            if(line.length() >= 5 && line.substring(0, 5).equals("start"))
            {
                if(!initThread.isAlive())
                {
                    throw new MyException("Controler reset detected!");
                }
            }
            //resend?
            if(line.length() >= 2 && line.substring(0, 2).equals("rs"))
            {
                rs = Integer.parseInt(line.substring(3));
            }
            if(line.length() >= 7 && line.substring(0, 7).equals("Resend:"))
            {
                receiveCount--; //Marlin ok will be comming anyway :-(
                try{
                    rs = Integer.parseInt(line.substring(7).trim());
                }
                catch(Exception ex){
                    throw new MyException("Resend string error! (" + ex.getMessage() + ")");
                }
                if(rs < cmdHistroy.size() - 1)
                {
                    throw new MyException("Resending old command!");
                }
            }

            if(rs > 0)
            {
                if(rs != 1)
                {
                    resend(rs);
                }
                else
                {
                    throw new MyException("Resend 1! Controller reset?!");
                }
            }

        }
        @Override
        protected boolean internal_isbusy(){
            return cmdHistroy.size() > receiveCount;
        }
        @Override
        protected boolean internal_isConnected(){
            return receiveCount != 0;            
        }
        @Override
        protected Double internal_ZEndStopHit(String line){
            if(line.contains("endstops") && line.contains("hit") && line.contains("Z:"))
            {
                try {
                    return Tools.strtod(line.substring(line.indexOf("Z:") + 2));
                } catch (ParseException ex) {
                    Logger.getLogger(Communication.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return null;
        }
        
        private void resend(int rs) throws MyException {
            //Cecksum:
            if(rs > cmdHistroy.size())
            {
                throw new MyException("Resend bigger than send history!");
            }

            String command = cmdHistroy.get(rs-1);
            command = "N" + cmdHistroy.size() + " " + command + " *";
            byte cs = 0;
            byte[] b = command.getBytes();
            for(int i = 0;  b[i]!= '*' && i < b.length; i++)
            {
                cs = (byte)(cs ^ b[i]);
            }
            command += "" + cs;

            doSendEvent("   " + rs + "=>[" + command + "]");
            core.send(command);

        }
        
        
    },
    GENERIC{
        private int sendcount   = 0;
        private int resivecount = 0;

        @Override
        protected void internal_connect() throws MyException,InterruptedException{
            internal_reset();

            //SET absolute
            internal_send("G90");
            doSendEvent("G90");
            
            //2 secound Timout for answer
            synchronized(Communication.class){
                for (int i = 0;i < 20;i++)
                {
                    Communication.class.wait(100);       //give data the possibility to enter internal_receive!
                    if(!core.isConnected())
                    {
                        throw new MyException("Lost connection!");
                    }
                    if (!internal_isbusy()) 
                    {
                        break;
                    }
                }
            }
            if(internal_isbusy())
            {
                throw new MyException("Printer did not answer to G90!");
            }
        }
        
        @Override
        protected void internal_reset(){
            sendcount   = 0;
            resivecount = 0;
        }
       
        @Override
        protected void internal_send(String command) throws MyException{
            core.send(command);
            sendcount++;
        }

        @Override
        protected void internal_receive(String line) throws MyException{
            int rs = 0;
            if(line.length() >= 2 && line.substring(0, 2).equals("ok"))
            {
                resivecount++;
            
                if(resivecount > sendcount)
                {
                    throw new MyException("More OK than send commands!");
                }
 
                Communication.class.notify();
            }
            // "start" line after reset
            if(line.length() >= 5 && line.substring(0, 5).equals("start"))
            {
                if(!initThread.isAlive())
                {
                    throw new MyException("Controler reset detected!");
                }
            }
        }
        @Override
        protected boolean internal_isbusy(){
            return sendcount > resivecount;
        }
        @Override
        protected boolean internal_isConnected(){
            return resivecount != 0;            
        }
        @Override
        protected Double internal_ZEndStopHit(String line){
            if(line.contains("endstops") && line.contains("hit") && line.contains("Z:")){
                try {
                    return Tools.strtod(line.substring(line.indexOf("Z:") + 2));
                } catch (ParseException ex) {
                    Logger.getLogger(Communication.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return null;
        }
        
    },
    GRBL{
        private int sendcount   = 0;
        private int resivecount = 0;
        private boolean whatforreset =false;

        @Override
        protected void internal_connect() throws MyException,InterruptedException{
            internal_reset();

            Communication.class.wait(2000);
            
            whatforreset=true;
            internal_send(Character.toString((char)24));
            doSendEvent("Send ctrl-C [RESET]");

            //Wait for GRBL to reset (1 secound)
            Communication.doChangedEvent("Wait for GRBL Reset ...");   
            
            //2 secound Timout for answer
            synchronized(Communication.class){
                for (int i = 0;i < 20;i++)
                {
                    Communication.class.wait(100);       //give data the possibility to enter internal_receive!
                    if(!core.isConnected())
                    {
                        throw new MyException("Lost connection!");
                    }
                    if (!whatforreset) 
                    {
                        break;
                    }
                }
            }
            if(whatforreset)
            {
                throw new MyException("Printer did not Reset!");
            }
        }
        
        @Override
        protected void internal_reset(){
            sendcount   = 0;
            resivecount = 0;
        }
       
        @Override
        protected void internal_send(String command) throws MyException{
            core.send(command);
            sendcount++;
        }

        @Override
        protected void internal_receive(final String line) throws MyException{
            int rs = 0;
            if(line.length() >= 2 && line.substring(0, 2).equals("ok"))
            {
                resivecount++;
            
                if(resivecount > sendcount)
                {
                    throw new MyException("More OK than send commands!");
                }
 
                Communication.class.notify();
            }
            
            final ObjectProxy<Boolean> status= new ObjectProxy<>();
            
            if(line.length()>=5 && line.substring(0,5).equals("error")){
                
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            status.set(JOptionPane.showConfirmDialog(null,"GRBL message: "+line+"\n\r Continue?","Error",JOptionPane.OK_CANCEL_OPTION,JOptionPane.ERROR_MESSAGE)==JOptionPane.OK_OPTION);
                        }
                    });
                } catch (Exception ex) {
                    throw new MyException(ex.getMessage());
                }
                
                if(status.get()==true){
                    resivecount++;

                    if(resivecount > sendcount)
                    {
                        throw new MyException("More OK than send commands!");
                    }

                    Communication.class.notify();                    
                }
                else{
                    //Kill conection
                    if(!initThread.isAlive())
                    {
                        throw new MyException("Error!");
                    }
                }
            }
            
            if(line.length()>=5 && line.substring(0,5).equals("ALARM")){
                if(!initThread.isAlive())
                {
                    throw new MyException(line);
                }
            }
            
            // "start" line after reset
            if(line.length() >= 4 && line.substring(0, 4).equals("Grbl"))
            {
                if(whatforreset){
                    whatforreset=false;
                }
                else if(!initThread.isAlive())
                {
                    throw new MyException("Controler reset detected!");
                }
            }
        }
        @Override
        protected boolean internal_isbusy(){
            return sendcount > resivecount;
        }
        @Override
        protected boolean internal_isConnected(){
            return resivecount != 0;            
        }
        @Override
        protected Double internal_ZEndStopHit(String line){
            if(line.contains("endstops") && line.contains("hit") && line.contains("Z:")){
                try {
                    return Tools.strtod(line.substring(line.indexOf("Z:") + 2));
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
    abstract void internal_receive(String line) throws MyException;         //Is Called if something is received
    abstract Double internal_ZEndStopHit(String line);      //is called on each line to detect endstopHit of Zacess
    abstract boolean internal_isbusy();          //Return if busy
    abstract boolean internal_isConnected();     //Returns if Connected
    
    
    private static Thread initThread    = new Thread();
    private static AComCore core        = new ComCoreDummy();
    private static String status        = "Not Connected!";
    private static ArrayList<IEvent> changed        = new ArrayList<>();
    private static ArrayList<IReceivedLines> received = new ArrayList<>();
    private static ArrayList<IEndstopHit> hitEndStop= new ArrayList<>();
    private static ArrayList<ISend> send            = new ArrayList<>();

    public static void addChangedEvent(IEvent e){
        changed.add(e);
    }
    
    public static void addReceiveEvent(IReceivedLines e){
        received.add(e);
    }
    public static void addZEndstopHitEvent(IEndstopHit e){
        hitEndStop.add(e);
    }
    public static void addSendEvent(ISend e){
        send.add(e);
    }
    
    public static synchronized void connect(final String port, final int speed) {
        //allready connected
        if(isConnected())
        {
            doChangedEvent(status);
            return;
        }
            
        //allready running init
        if(core.isSimulation() && initThread.isAlive())
        {
            return;
        } 
        
        //Prepair connection
        resetAll();
        
        doChangedEvent("Connecting ... ");
        
        //Do not block GUI:
        initThread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized(Communication.class){
                    
                    //Try to open Port
                    try {
                        core = AComCore.openPort(new IReceivedLines() {
                            @Override 
                            public void received(String[] lines) {
                                Communication.receive(lines);
                            }
                        }, new IDisconnect() {
                            @Override
                            public void disconnect(String status) {
                                synchronized(Communication.class){
                                    resetAll();
                                    doChangedEvent(status);
                                }
                            }
                        }, port, speed);
                    } catch (Exception ex) {
                        resetAll();
                        doChangedEvent(ex.getMessage());
                        return;
                    }
                    try {
                        //Run connect
                        I().internal_connect();
                        
                    } catch (InterruptedException | MyException ex) {
                        Logger.getLogger(Communication.class.getName()).log(Level.SEVERE, null, ex);
                        resetAll();
                        doChangedEvent(ex.getMessage() == null ? status:ex.getMessage());
                        return;
                    }
                    
                    doChangedEvent("Connected!");
                    Communication.class.notify();
                }
            }
        });
        initThread.start();
        
        //Wait for initThread to be started
        while(initThread.getState() == Thread.State.NEW) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                Logger.getLogger(Communication.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
    
    public static synchronized void disconnect(){
        resetAll();
        doChangedEvent("Disconnected!");
    }
    
    public static synchronized void send(String command) throws ComInterruptException {
        if(!isConnected())
        {
            disconnect();
            throw new ComInterruptException("Not connected !"); 
        }
        //No command
        if(command.trim().equals(""))
        {
            return;
        }
        
        while((I().internal_isbusy() || initThread.isAlive()) && isConnected())
        {
            try {
                Communication.class.wait();
            } catch (InterruptedException ex) { 
                throw new ComInterruptException("Interrupt!"); 
            }
        }
        if(!isConnected())
        {
            throw new ComInterruptException("Not connected !");
        } 
        
        doSendEvent(command);
        
        try {
            I().internal_send(command);
        } catch (MyException ex) {
            Logger.getLogger(Communication.class.getName()).log(Level.SEVERE, null, ex);
            resetAll();
            doChangedEvent(ex.getMessage());
            throw new ComInterruptException(ex.getMessage());
        }
    }
    
    public static synchronized boolean isConnected(){
        return core.isConnected() && I().internal_isConnected() && !initThread.isAlive();
    }
    
    public static synchronized boolean isBussy(){
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
        return valueOf(DatabaseV2.COMTYPE.get());
    }

    private static synchronized void receive(String[] lines) {
        //remove emty lines
        ArrayList<String> l= new ArrayList<>();
        for(String s:lines)
            if(!s.equals(""))
                l.add(s);
        lines=l.toArray(new String[0]);
        
        doResiveEvent(lines);
        
        for(String line:lines){
            try {
                I().internal_receive(line);
            } catch (MyException ex) {
                Logger.getLogger(Communication.class.getName()).log(Level.SEVERE, null, ex);
                resetAll();
                doChangedEvent(ex.getMessage());
                return;
            }
            
            Double val = I().internal_ZEndStopHit(line);
            if(val != null)
            {
                doEnstopHitEvent(val);
            }
        }
    }
    
    private static void resetAll(){
        core.discharge();
        core = new ComCoreDummy();
        for(Communication c:Communication.values())
        {
            c.internal_reset();
        }
        initThread.interrupt();
        Communication.class.notifyAll();
    }
    
    private static void doChangedEvent(String status)
    {
        Communication.status=status;
        for(final IEvent e:changed)
        {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    e.fired();
                }
            });
        }
    }
    
    private static void doResiveEvent(final String[] lines)
    {
        for(final IReceivedLines e:received)
        {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    e.received(lines);
                }
            });
        }
    }
    private static void doEnstopHitEvent(final double value)
    {
        for(final IEndstopHit e:hitEndStop)
        {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    e.endStopHit(value);
                }
            });
        }
    }
    private static void doSendEvent(final String cmd)
    {
        for(final ISend e:send)
        {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    e.send(cmd);
                }
            });
        }
    }
    
    @Override
    public String toString() {
        return name();
    }
    
}
