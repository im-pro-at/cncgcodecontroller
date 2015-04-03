/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

/**
 *
 * @author patrick
 */
public class ObjectProxy<T> {
    private T t=null;
    public void set(T t){
        this.t=t;
    }
    public T get(){
        return this.t;
    }
}
