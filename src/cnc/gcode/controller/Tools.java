/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JToolTip;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 *
 * @author patrick
 */
public class Tools {
    private static DecimalFormat df;

    // Creates and show a tooltip over the component passed as parameter
    public static void popUpToolTip(JComponent comp, String text) {
        // build ToolTip from JComponent
        JToolTip toolTip = comp.createToolTip();
        // with the good text
        toolTip.setTipText(text);
        // get JComponent position
        Point point = comp.getLocationOnScreen();
        final Popup popup = PopupFactory.getSharedInstance().getPopup(comp, toolTip, point.x , point.y + comp.getHeight());
        // show it
        popup.show();
        // and start a thread to remove it 
        new Timer(5000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            popup.hide();
                        }
                    });
                }
            }).start();
    }
            
    
    static {
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setDecimalSeparator('.');
        s.setMonetaryDecimalSeparator('.');
        s.setMinusSign('-');
        df = new DecimalFormat("0.00",s);
    }
       
    public static Double strtod(String s) throws ParseException
    {
        return df.parse(s).doubleValue();
    }

    /**
     * on error returns 0.0
     * @param s
     * @return
     * @throws ParseException 
     */
    public static Double strtodsave(String s)
    {
        try {
            return strtod(s);
        } catch (ParseException ex) {
            return 0.0;
        }
    }
    
    public static String dtostr(Double d)
    {
        return df.format(d);
    }
    
    public static Double[] getValues(String[] messages,Double[] defaults, Double[] max, Double[] min)
    {
        if(messages.length!=defaults.length || messages.length!=max.length || messages.length!=min.length)
            throw new IllegalArgumentException("Length of parameter not the same!");

        Double[] values= new Double[messages.length];

        for(int i=0;i<messages.length;i++)
        {
            values[i]=defaults[i];
            while(true)
            {
                try
                {
                    values[i]=Tools.strtod(JOptionPane.showInputDialog(messages[i], Tools.dtostr(values[i])));
                }
                catch(ParseException ex)
                {
                    JOptionPane.showMessageDialog(null, ex.toString() );
                    continue;
                }
                catch(NullPointerException ex)
                {
                    return null;
                }
                if(values[i]<min[i])
                {
                    JOptionPane.showMessageDialog(null, "The value should be bigger then "+Tools.dtostr(min[i]) );
                    continue;
                }
                if(values[i]>max[i])
                {
                    JOptionPane.showMessageDialog(null, "The value should be less then "+Tools.dtostr(max[i]) );
                    continue;
                }
                
                break;
            }
        }
        return values;
    }

    
    
}

