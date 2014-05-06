/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import java.text.ParseException;
import java.util.ArrayList;

/**
 *
 * @author patrick
 */
public class CommandParsing {

    public static class Parameter
    {
        public final char letter;
        public final double value;
        public final boolean isint;

        private Parameter(char letter, double value, boolean isint) {
            this.letter = letter;
            this.value = value;
            this.isint= isint;
        }
        
        private Parameter(char letter)
        {
            this(letter,0.0,false);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + this.letter;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Parameter other = (Parameter) obj;
            if (this.letter != other.letter) {
                return false;
            }
            return true;
        }
        
    }
    
    public static final char[] axesName = {'X', 'Y', 'Z', 'F'};
    
    private boolean error = false;
    private ArrayList<Parameter> parameters = new ArrayList<>();
    private String cmdwithoutcomments;

    public CommandParsing(String command) {
        String save = "";
        cmdwithoutcomments="";
        char letter = 0;
        int comment = 0;
        for (char c : (command + " ").toCharArray()) {
            
            if(comment == 0 && c != '(' && c != ';')
            {
                cmdwithoutcomments+=""+c;
            }
            
            if ((c < '0' || c > '9') && c != '.' && c != '-') {
                if (letter != 0) {
                    //End of number
                    double number=0.0;
                    try {
                        number=Tools.strtod(save);
                    } catch (Exception ex) {
                        error = true;
                    }
                    Parameter p= new Parameter(letter, number, !save.contains("."));
                    
                    if (parameters.contains(p)) {
                        error = true;
                    } else {
                        parameters.add(p);
                    }
                    
                    save = "";
                    letter = 0;
                }
            } else if (letter != 0) {
                //Number
                save += c;
                continue;
            } else if (comment == 0) {
                //Number without letter!
                error = true;
            }
            
            if (c == ' ' || c == '\t') {
                continue; //Space
                //Space
            }
            
            if (c == '(' || c == ')') {
                //Comment
                if (c == '(') {
                    comment++;
                }
                if (c == ')') {
                    comment--;
                }
                if (comment < 0) {
                    error = true;
                }
                continue;
            }
            
            if (comment != 0) {
                continue; //in a comment
                //in a comment
            }
            
            if (c == ';') {
                break; //Start of line comment
                //Start of line comment
            }
            
            if (Character.isUpperCase(c)) {
                //Start Number
                letter = c;
                save = "";
                continue;
            }
            
            error = true;
        }
    }

    @Override
    public String toString() {
        String s = "";
        for (int i = 0; i < parameters.size(); i++) {
            s += "" + parameters.get(i).letter;
            if(parameters.get(i).isint)
                 s+=""+(int)(parameters.get(i).value) + " ";
            else
                 s+=""+Tools.dtostr(parameters.get(i).value) + " ";
        }
        return s;
    }

    public boolean isEmpty() {
        return parameters.isEmpty();
    }
    
    public int size()
    {
        return parameters.size();
    }
    
    public boolean contains(char letter)
    {
        return parameters.contains(new Parameter(letter));
    }
    
    public boolean insert(int i, char letter, double value, boolean isint){
        Parameter p = new Parameter(letter, value, isint);
        if (parameters.contains(p)){
            return false;
        } else {
            parameters.add(i, p);
        }
        return true;
    }
    
    public Parameter get(int index)
    {
        return parameters.get(index);
    }
    
    public Parameter get(char letter)
    {
        if(contains(letter))
            return parameters.get(parameters.indexOf(new Parameter(letter)));
        else
            return new Parameter(letter, Double.NaN, false);            
    }

    public boolean iserror()
    {
        return error;
    }

    public String getCmdwithoutcomments() {
        return cmdwithoutcomments;
    }
        
}
