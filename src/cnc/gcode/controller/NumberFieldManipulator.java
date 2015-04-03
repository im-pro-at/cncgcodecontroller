/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.ParseException;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 *
 * @author patrick
 */
class NumberFieldManipulator {

    public interface IAxesEvent
    {
        void fired(NumberFieldManipulator axis);
    }
    
    private final JComponent element;
    private final IAxesEvent event;

    public NumberFieldManipulator(JComponent element, IAxesEvent event) {
        this.element    = element;
        this.event      = event;
        FocusAdapter f = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) 
            {
                NumberFieldManipulator.this.event.fired(NumberFieldManipulator.this);
            }

            @Override
            public void focusGained(FocusEvent e) 
            {
                if (e.getSource() instanceof JComboBox) 
                {
                    ((JComboBox) e.getSource()).getEditor().selectAll();
                }
                if (e.getSource() instanceof JTextField) 
                {
                    ((JTextField) e.getSource()).selectAll();
                }
            }
        };
        KeyListener k = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) 
                {
                    NumberFieldManipulator.this.event.fired(NumberFieldManipulator.this);
                }
            }
        };
        if (element instanceof JComboBox) 
        {
            ((JComboBox) element).getEditor().getEditorComponent().addFocusListener(f);
            ((JComboBox) element).getEditor().getEditorComponent().addKeyListener(k);
        } else 
        {
            element.addFocusListener(f);
            element.addKeyListener(k);
        }
    }
  
    public void setFocus() 
    {
        element.requestFocusInWindow();
    }

    public void dispatchEvent() 
    {
        event.fired(this);
    }

    public void set(String text) {
        if (element instanceof JTextField) 
        {
            ((JTextField) element).setText(text);
        } 
        else if (element instanceof JComboBox) 
        {
            ((JComboBox) element).setSelectedItem(text);
        } 
        else 
        {
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    public String get() {
        if (element instanceof JTextField) 
        {
            return ((JTextField) element).getText();
        } 
        else if (element instanceof JComboBox) 
        {
            return ((JComboBox) element).getSelectedItem().toString();
        } 
        else 
        {
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    public void set(Double d) {
        set(Tools.dtostr(d));
    }

    public Double getd() throws ParseException {
        return Tools.strtod(get());
    }

    /**
     * Return 0.0 if its not a Number!
     */
    public Double getdsave() {
        try {
            return Tools.strtod(get());
        } 
        catch (ParseException ex) 
        {
            return 0.0;
        }
    }

    // Creates and show a tooltip over the component passed as parameter
    public void popUpToolTip(String text) {
        // build ToolTip from JComponent
        JToolTip toolTip = element.createToolTip();
        // with the good text
        toolTip.setTipText(text);
        // get JComponent position
        Point point = element.getLocationOnScreen();
        final Popup popup = PopupFactory.getSharedInstance().getPopup(element,
                                                                      toolTip,
                                                                      point.x ,
                                                                      point.y + element.getHeight());
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
            
    
}
