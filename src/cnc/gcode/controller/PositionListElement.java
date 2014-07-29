/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import java.util.Objects;

/**
 *
 * @author patrick
 */
public class PositionListElement implements Comparable<PositionListElement> {
    private final String name;
    private final Double x;
    private final Double y;

    public PositionListElement(String name, Double x, Double y) {
        this.name = name;
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return name + " ( " + Tools.dtostr(x) + " ; " + Tools.dtostr(y) + " )";
    }

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) 
        {
            return false;
        }
        if (getClass() != obj.getClass()) 
        {
            return false;
        }
        final PositionListElement other = (PositionListElement) obj;
        if (!Objects.equals(this.name, other.name)) 
        {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(PositionListElement o) 
    {
        return name.compareTo(o.name);
    }


    
}
