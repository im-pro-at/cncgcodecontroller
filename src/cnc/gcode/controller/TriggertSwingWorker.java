/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import javax.swing.SwingUtilities;

/**
 *
 * @author patrick
 */
public abstract class TriggertSwingWorker<P> {
    
    protected abstract P doJob() throws Exception;
    protected void process(P chunk){}
    
    private boolean cancelled=false;
    
    private Thread t= new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                P p;
                synchronized(TriggertSwingWorker.this)
                {
                    //Wait for first trigger
                    TriggertSwingWorker.this.wait();
                }
                while(true)
                {
                    try {
                        p=doJob();
                    } catch (InterruptedException ex) {
                        return;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        continue;
                    }
                    synchronized(TriggertSwingWorker.this)
                    {
                        if(cancelled)
                            return;
                        final P cp=p;
                    
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                process(cp);
                            }
                        });
                        
                        //Wait for trigger
                        TriggertSwingWorker.this.wait();
                        
                    }
                }
            } catch (InterruptedException ex) {
                //Interrupted!
            }
        }
    }){
        {
            //Start thread imediatly:
            start();
        }
    };    
    
    public final synchronized boolean isCancelled()
    {
        return cancelled;
    }
    
    public final synchronized void cancel()
    {
        if(!cancelled)
        {
            cancelled=true;
            if(!Thread.currentThread().equals(t))
                t.interrupt();
        }
    }
    
    public final synchronized void trigger()
    {
        notify();
    }
        
}
