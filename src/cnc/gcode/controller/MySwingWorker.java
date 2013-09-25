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
    
    private boolean cancelled=false;
    private boolean done=false;
    private boolean started=false;
    final private Object syncprogress=new Object();
    boolean progressstate=false;
    private int progress=0;
    private String mprogress="";
    final private Object syncprocess=new Object();
    boolean processstate=false;
    private LinkedList<P> chunkes= new LinkedList<>();
    final private Object syncpause= new Object();
    private boolean pause=false;
    
    private Thread t= new Thread(new Runnable() {
        @Override
        public void run() {
            Exception exception=null;
            R rvalue=null;
            try {
                rvalue=doInBackground();
            } catch (Exception ex) {
                exception=ex;
            }
            
            //Done:
            synchronized(MySwingWorker.this)
            {
                done=true;
                final Exception cexception=exception;
                final R crvalue=rvalue;
                final boolean ccancelled=cancelled;
                
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        done(crvalue, cexception, ccancelled);
                    }
                });
            }
        
        }
    });    
    
    protected final void publish(P p)
    {
        if(!Thread.currentThread().equals(t))
            throw new UnsupportedOperationException("Must be called from worker Thread!");
        synchronized(syncprocess)
        {
            chunkes.add(p);
            if(!processstate)
            {
                processstate=true;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        List<P> list;
                        synchronized(syncprocess)
                        {
                            MySwingWorker.this.processstate=false;
                            list=MySwingWorker.this.chunkes;
                            MySwingWorker.this.chunkes= new LinkedList<>();
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
            throw new UnsupportedOperationException("Must be called from worker Thread!");
        synchronized(syncprogress)
        {
            this.progress=progress;
            this.mprogress=message;
            if(!progressstate)
            {
                progressstate=true;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        int value;
                        String message;
                        //Acess Value
                        synchronized(syncprogress)
                        {
                            MySwingWorker.this.progressstate=false;
                            value=MySwingWorker.this.progress;
                            message=MySwingWorker.this.mprogress;
                        }
                        progress(value,message);
                    }
                });
            }
        }
    }
    
    public final synchronized void execute()
    {
        if(!started)
        {
            started=true;
            t.setName("MyWorker p="+Thread.currentThread().getName());
            t.start();
        }
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
        if(started && !cancelled && !done)
        {
            cancelled=true;
            if(!Thread.currentThread().equals(t))
                t.interrupt();
        }
    }
    
    public final void pause(boolean state)
    {
        synchronized(syncpause)
        {
            if(pause==state)
                return;
            pause=state;
            if(!pause)
                syncpause.notify();
        }
    }
    
    public final boolean isPuased()
    {
        synchronized(syncpause)
        {
            return pause;
        }
    }
    
    protected final void dopause() throws InterruptedException
    {
        if(!Thread.currentThread().equals(t))
            throw new UnsupportedOperationException("Must be called from worker Thread!");
        synchronized(syncpause)
        {
            if(pause)
                syncpause.wait();
        }        
    }

    public synchronized void trigger()
    {
        notify();
    }
    
    protected synchronized void waitfortrigger(long timout) throws InterruptedException
    {
        wait(timout);
    }
    
}
