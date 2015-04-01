/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

/**
 *
 * @author nra
 */
public class XYZPosition {

    private double X = 0.0;
    private double Y = 0.0;
    private double Z = 0.0;
    private boolean isValidPosition = false;

    public boolean IsValidPosition() {
        return isValidPosition;
    }

    public void setIsValidPosition(boolean isValidPosition) {
        this.isValidPosition = isValidPosition;
    }

    public double getX() {
        return X;
    }

    public void setX(double X) {
        this.X = X;
    }

    public double getY() {
        return Y;
    }

    public void setY(double Y) {
        this.Y = Y;
    }

    public double getZ() {
        return Z;
    }

    public void setZ(double Z) {
        this.Z = Z;
    }

}
