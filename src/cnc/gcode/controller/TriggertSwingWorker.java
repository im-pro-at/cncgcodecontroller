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
    
    private boolean cancelled   = false;
    private boolean triggered   = false;
    
    private Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                P p;
                while(true)
                {
                    synchronized(TriggertSwingWorker.this)
                    {
                        //Wait for trigger
                        if(!triggered)
                        {
                            TriggertSwingWorker.this.wait();
                        }
                        triggered = false;
                    }
                    try {
                        p = doJob();
                    } 
                    catch (InterruptedException ex) 
                    {
                        return;
                    } 
                    catch (Exception ex) 
                    {
                        ex.printStackTrace();
                        continue;
                    }
                    synchronized(TriggertSwingWorker.this)
                    {
                        if(cancelled)
                        {
                            return;
                        }
                        final P cp = p;
                    
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                process(cp);
                            }
                        });
                    }
                }
            } catch (InterruptedException ex) {
                //Interrupted!
            }
        }
    }){
        {
            //Start thread imediatly:
            setName("TriggerWorker p=" + Thread.currentThread().getName());
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
            cancelled = true;
            if(!Thread.currentThread().equals(t))
            {
                t.interrupt();
            }
        }
    }
    
    public final synchronized void trigger()
    {
        triggered = true;
        notify();
    }
        
}
