/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import java.util.List;

/**
 *
 * @author patrick
 */
public class CNCOptimiser {
    public interface IProgress{
        public void publish(String message, int progess) throws MyException;
    }
    
    IProgress progress;

    public CNCOptimiser(IProgress progress) {
        this.progress = progress;
    }

    public CNCCommand[] execute(CNCCommand[] incmds) throws MyException{
        progress.publish("Checking commads", 0);
    
        //search first and last g1
        
   /*     double levelg1=Double.NaN;
        for(int i=0;i<incmds.length;i++)
        {
            //Test if all G0 have the same level
            if(cmds)
        }
    */
        
        
        
        throw new MyException("Not implement!");
    }
    
    
    
   
}
