package ranavisu.bt200.util;

import net.joinpad.argeo.AREntity;
import net.joinpad.argeo.ARGeoFragment;
import net.joinpad.argeo.ARWidget;
import net.joinpad.argeo.LocationAbsolute;
import net.joinpad.argeo.LocationRelative;

import rajawali.Object3D;

/**
 * Created by Jean-Philibert on 02/04/2016.
 */
public class ARgeoEntity extends AREntity {

    private ARgeoData argd;

    public ARgeoEntity(ARGeoFragment arGeoFragment, ARgeoData argd) {
        super(arGeoFragment);
        this.argd = argd;
    }

    public ARgeoEntity(ARGeoFragment arGeoFragment, LocationAbsolute locationAbsolute, LocationRelative locationRelative, Object3D object3D, ARWidget arWidget, ARgeoData argd) {
        super(arGeoFragment, locationAbsolute, locationRelative, object3D, arWidget);
        this.argd = argd;
    }


    public ARgeoData getArgd() {
        return argd;
    }

    public void setArgd(ARgeoData argd) {
        this.argd = argd;
    }
}
