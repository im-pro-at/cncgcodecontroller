/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import java.util.LinkedList;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 *
 * @author patrick
 */
public abstract class MySwingWorker<R,P> {
    
    protected abstract R doInBackground() throws Exception;
    protected abstract void done(R rvalue, Exception ex, boolean canceled);
    protected void process(List<P> chunks){}
    protected void progress(int progress,String message){}
    
    private boolean cancelled   = false;
    private boolean done        = false;
    private boolean started     = false;
    final private Object syncProgress = new Object();
    boolean progressState       = false;
    private int progress        = 0;
    private String mProgress    = "";
    final private Object syncProcess = new Object();
    boolean processState        = false;
    private LinkedList<P> chunks= new LinkedList<>();
    final private Object syncPause = new Object();
    private boolean pause          = false;
    
    private Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
            Exception exception = null;
            R rValue = null;
            try {
                rValue = doInBackground();
            } catch (Exception ex) {
                exception = ex;
            }
            
            //Done:
            synchronized(MySwingWorker.this)
            {
                done = true;
                final Exception cexception = exception;
                final R crValue = rValue;
                final boolean ccancelled = cancelled;
                
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        done(crValue, cexception, ccancelled);
                    }
                });
            }
        
        }
    });    
    
    protected final void publish(P p)
    {
        if(!Thread.currentThread().equals(t))
        {
            throw new UnsupportedOperationException("Must be called from worker thread!");
        }
        synchronized(syncProcess)
        {
            chunks.add(p);
            if(!processState)
            {
                processState = true;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        List<P> list;
                        synchronized(syncProcess)
                        {
                            MySwingWorker.this.processState = false;
                            list    = MySwingWorker.this.chunks;
                            MySwingWorker.this.chunks = new LinkedList<>();
                        }
                        process(list);
                    }
                });
            }
        }
    }

    
    protected final void setProgress(int progress, String message)
    {
        if(!Thread.currentThread().equals(t))
        {
            throw new UnsupportedOperationException("Must be called from worker thread!");
        }
        synchronized(syncProgress)
        {
            this.progress   = progress;
            this.mProgress  = message;
            if(!progressState)
            {
                progressState=true;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        int value;
                        String message;
                        //Acess Value
                        synchronized(syncProgress)
                        {
                            MySwingWorker.this.progressState = false;
                            value   = MySwingWorker.this.progress;
                            message = MySwingWorker.this.mProgress;
                        }
                        progress(value,message);
                    }
                });
            }
        }
    }
    
    public final synchronized void execute()
    {
        if(started == true)
        {
            return;
        }
        
        started = true;
        t.setName("MyWorker p=" + Thread.currentThread().getName());
        t.start();
    }
    
    public final synchronized boolean isRunning()
    {
        return started && !done;
    }
    
    public final synchronized boolean isDone()
    {
        return done;
    }
    
    public final synchronized boolean isCancelled()
    {
        return cancelled;
    }
    
    public final synchronized void cancel()
    {
        if(started == true 
            && cancelled == false 
            && done == false)
        {
            cancelled = true;
            if(!Thread.currentThread().equals(t))
            {
                t.interrupt();
            }
        }
    }
    
    public final void pause(boolean state)
    {
        synchronized(syncPause)
        {
            if(pause == state)
            {
                return;
            }
            pause = state;
            if(pause == false)
            {
                syncPause.notify();
            }
        }
    }
    
    public final boolean isPaused()
    {
        synchronized(syncPause)
        {
            return pause;
        }
    }
    
    protected final void dopause() throws InterruptedException
    {
        if(!Thread.currentThread().equals(t))
        {
            throw new UnsupportedOperationException("Must be called from worker thread!");
        }
        synchronized(syncPause)
        {
            if(pause)
            {
                syncPause.wait();
            }
        }        
    }

    public synchronized void trigger()
    {
        notify();
    }
    
    protected synchronized void waitForTrigger(long timout) throws InterruptedException
    {
        wait(timout);
    }
    
}
