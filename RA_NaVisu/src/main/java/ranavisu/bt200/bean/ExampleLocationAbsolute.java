package ranavisu.bt200.bean;

import net.joinpad.argeo.LocationAbsolute;

public class ExampleLocationAbsolute extends LocationAbsolute {

    private Number A;
    private Number k;
    private Number altitude;

    public Number getA() {
        return this.A;
    }

    public void setA(Number a) {
        this.A = a;
    }

    public Number getK() {
        return this.k;
    }

    public void setK(Number k) {
        this.k = k;
    }

    public void setAltitude(Number altitude) {
        this.altitude = altitude;
    }

    @Override
    public double getLatitude() {
        return k.doubleValue();
    }

    @Override
    public double getLongitude() {
        return A.doubleValue();
    }

    @Override
    public double getAltitude() {
        return altitude.doubleValue();
    }

    @Override
    public String toString() {
        return "lat: " + k + " lng: " + A + " alt: " + altitude;
    }
}
