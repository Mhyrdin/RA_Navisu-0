package ranavisu.bt200.demo;

import ranavisu.bt200.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import net.joinpad.argeo.AREntities;
import net.joinpad.argeo.AREntity;
import net.joinpad.argeo.ARGeoData;
import net.joinpad.argeo.ARGeoFragment;
import net.joinpad.argeo.ARGeoRenderer;
import net.joinpad.argeo.ARWidget;
import net.joinpad.argeo.LocationAbsolute;
import net.joinpad.argeo.interfaces.OnAREntityLocationChangedListener;
import net.joinpad.argeo.interfaces.OnARGeoInitializedListener;
import net.joinpad.argeo.interfaces.OnARGeoRenderCycleListener;
import net.joinpad.argeo.interfaces.OnLocationChangedListener;
import net.joinpad.argeo.interfaces.OnLocationResolvedListener;
import net.joinpad.arrakis.argeo.ARGeoMoverioFragment;
import net.joinpad.arrakis.interaction.PointerHandler;
import net.joinpad.arrakis.layout.SystemBarsCtrl;
import net.joinpad.locationprovider.LocationProvider;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import jp.epson.moverio.bt200.DisplayControl;
import rajawali.Object3D;
import rajawali.animation.Animation;
import rajawali.animation.Animation3D;
import rajawali.animation.RotateOnAxisAnimation;
import rajawali.materials.Material;
import rajawali.math.vector.Vector3;
import rajawali.parser.LoaderOBJ;
import rajawali.parser.ParsingException;
import rajawali.primitives.Line3D;
import rajawali.scene.RajawaliScene;
import rajawali.util.OnObjectPickedListener;
import ranavisu.bt200.bean.ExampleLocationAbsolute;
import ranavisu.bt200.util.GPSTracker;
import ranavisu.bt200.util.I_LocationSet;

// net.joinpad.locationprovider.GpsTracker;
//import jp.epson.moverio.bt200.SensorControl;

public class Test1RaNavisu extends Activity implements OnARGeoInitializedListener, OnLocationChangedListener, OnLocationResolvedListener,
        OnARGeoRenderCycleListener, OnObjectPickedListener, I_LocationSet {

    private String TAG = "Test1RaNavisu";

    private DisplayControl mDisplayControl;
    private CustomLocationProvider mCustomLocationProvider;

    private ARGeoMoverioFragment mARGeoMoverioFragment;
    private PointerHandler mPointerHandler;


    private Random alea=new Random();

    private Button buttonSetCoordinates, bBackward_button, bForward_button;
    private TextView mActualLocationTextView;
    private TextView mManualLocLegend;

    private ToggleButton GPSswitch;
    private boolean GPS_on;// if false, then you can choose a mock location
    private boolean gpsFirst=true;// to suggest activating GPS only once
    GPSTracker my_gps;

    //----------------------------- Some georeferenced places --------------------------------------

    private ArrayList <String> listLoc = new ArrayList <String>();
    private LinkedList<Double> distList =new LinkedList<Double>();
    private ArrayList <double[]> listLatLongAlt = new ArrayList <double[]>();
    private String[] tabL = {/*"point O",*/ "poste de garde","place d'armes","entrée RDE","bâtiment D","bâtiment E","bâtiment A",
            "bâtiment F","bâtiment L","bâtiment M","bâtiment N","entrée Schreu", "gymnase","stade",
            "cours Tennis", "terrain Basket"/*, "point C", "point P", "point K1", "point K2"*/, "passerelle E-M", "passerelle D-L",
           /* "corner1", "corner2", "corner3", "corner4",*/"M109", "L114"};
    private double[][] tabAlatLong = {
            /*{3.0, 48.418682, -4.472090},*/
            {2.5, 48.418959, -4.471688},
            {5.0, 48.418847, -4.472463},
            {3.0, 48.419416, -4.471172},
            {3.5, 48.418362, -4.472915},
            {3.5, 48.418286, -4.472365},
            {3.8, 48.419178, -4.472483},
            {3.5, 48.419638, -4.472491},
            {3.5, 48.417947, -4.472961},
            {3.5, 48.417987, -4.472314},
            {3.0, 48.418061, -4.471638},
            {3.0, 48.419536, -4.471566},
            {3.3, 48.419354, -4.474097},
            {3.0, 48.418453, -4.473937},
            {3.0, 48.418995, -4.473290},
            {3.0,  48.418656, -4.473239},
            /*{3.0, 48.419096, -4.471261},
            {3.0,  48.419383, -4.471649},
            {3.0, 48.418919, -4.472608},
            {3.5, 48.418943, -4.472347},*/
            {3.5,  48.418100, -4.472254},
            {3.5,  48.418078, -4.472785},
            //corners of building E
            /*{2.5,  48.418246, -4.472069},
            {2.5,  48.418368, -4.472092},
            {2.5,  48.418328, -4.472670},
            {2.5,  48.418197, -4.472645},*/
            {4.5,  48.417985, -4.472016},
            {4.5,  48.417976, -4.473114},
    };
    //----------------------------------------------------------------------------------------------
    double[] newLoc =new double[3];// newLoc=user's location

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //super.onCreate(savedInstanceState);

        mDisplayControl = new DisplayControl(Test1RaNavisu.this);

        setContentView(R.layout.test1_ranavisu);

        mARGeoMoverioFragment = new ARGeoMoverioFragment();

        mARGeoMoverioFragment.setOnARGeoInitializedListener(this);
        mARGeoMoverioFragment.setOnLocationResolvedListener(this);
        mARGeoMoverioFragment.setOnLocationChangedListener(this);
        mCustomLocationProvider = new CustomLocationProvider(mARGeoMoverioFragment);
        mARGeoMoverioFragment.setLocationProvider(mCustomLocationProvider);
        mARGeoMoverioFragment.setOnARGeoRenderCycleListener(this);
        mARGeoMoverioFragment.setOnObjectPickedListener(this);

        getFragmentManager().beginTransaction().replace(R.id.container, mARGeoMoverioFragment).commit();

        /*
        getting access to the various widget on our layout .........................................
         */
        mActualLocationTextView = (TextView) findViewById(R.id.actual_location_textview);
        mManualLocLegend = (TextView) findViewById(R.id.manualLoc);
        buttonSetCoordinates = (Button) findViewById(R.id.set_coordinates_button);
        bBackward_button = (Button) findViewById(R.id.backward_button);
        bForward_button= (Button) findViewById(R.id.forward_button);
        Button spawnAREntityButton = (Button) findViewById(R.id.bAction);// currently used to spawn lines

        // This is the view that will be used as pointer
        //final ImageView mPointer = (ImageView) findViewById(R.id.pointer);

        // This is the pointer handler that will dispatch events when the pointer view have to trigger some event in an HoverableLayout
        //mPointerHandler = new PointerHandler(mPointer);

        //==========================================================================================
        /*
         updating position thanks to GPS data ......................................................
          */
        GPSswitch = (ToggleButton) findViewById(R.id.GPSswitch);
        GPS_on = false;
        my_gps = new GPSTracker(Test1RaNavisu.this);

        GPSswitch.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
                GPS_on = !GPS_on;
                if (GPS_on){
                    GPSswitch.setTextColor(Color.GREEN);
                    buttonSetCoordinates.setTextColor(Color.RED);
                    bForward_button.setTextColor(Color.RED);
                    bBackward_button.setTextColor(Color.RED);
                    // check if GPS enabled
                    if (gpsFirst){
                        if (my_gps.canGetLocation()) {
                            double latitude = my_gps.getLatitude();
                            double longitude = my_gps.getLongitude();
                            Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
                        } else {
                            // can't get location
                            // GPS or Network is not enabled
                            // Ask user to enable GPS/network in settings
                            my_gps.showSettingsAlert();
                        }
                        gpsFirst=false;
                    }
                } else {
                    GPSswitch.setTextColor(Color.RED);
                    buttonSetCoordinates.setTextColor(Color.GREEN);
                    bForward_button.setTextColor(Color.GREEN);
                    bBackward_button.setTextColor(Color.GREEN);
                }
            }
        });
        //==========================================================================================

        /*
        When GPS is not activated, user can set his position and then move forward or backward .....
         */

        // setting once position at a known place
        buttonSetCoordinates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!GPS_on) {
                    //Creating the instance of PopupMenu
                    PopupMenu popup = new PopupMenu(Test1RaNavisu.this, buttonSetCoordinates);
                    //Inflating the Popup using xml file
                    popup.getMenuInflater().inflate(R.menu.menu_loc, popup.getMenu());
                    //registering popup with OnMenuItemClickListener
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            String titleL = (String) item.getTitle();
                            android.location.Location mockLocation = new android.location.Location("");
                            double lat, lon, alti;
                            if (!titleL.equals("avion")) {
                                int inde = listLoc.indexOf(titleL);
                                newLoc = listLatLongAlt.get(inde);
                                lat = newLoc[1];
                                lon = newLoc[2];
                                alti = 1.7; //newLoc[0];
                            } else { // vue aérienne
                                Location mocklo = mARGeoMoverioFragment.getMockLocation();
                                lat = mocklo.getLatitude();
                                lon = mocklo.getLongitude();
                                alti = 150; //newLoc[0]
                            }
                            Toast.makeText(Test1RaNavisu.this, "You selected: " + lat + " " + lon, Toast.LENGTH_SHORT).show();

                            mockLocation.setLatitude(lat);
                            mockLocation.setLongitude(lon);
                            mockLocation.setAltitude(alti);

                            // Here we set a mock location
                            mARGeoMoverioFragment.setMockLocation(mockLocation);
                            mManualLocLegend.setText(titleL);
                            //Toast.makeText(Test1RaNavisu.this, "Location resolved: " + mARGeoMoverioFragment.getMockLocation().getLatitude() + " " + mARGeoMoverioFragment.getMockLocation().getLongitude(), Toast.LENGTH_LONG).show();
                            return true;
                        }
                    });
                    popup.show();//showing popup menu
                }
            }
        });

        // handling input to move forward
        bForward_button.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionevent) {
                if (!GPS_on) {
                    int action = motionevent.getAction();
                    if (action == MotionEvent.ACTION_DOWN) {
                        mCustomLocationProvider.startMove(1);
                    } else if (action == MotionEvent.ACTION_UP) {
                        mCustomLocationProvider.stopMove();
                    }
                }
                return false;
            }
        });

        // handling input to move backward
        bBackward_button.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionevent) {
                if (!GPS_on) {
                    int action = motionevent.getAction();
                    if (action == MotionEvent.ACTION_DOWN) {
                        mCustomLocationProvider.startMove(-1);
                    } else if (action == MotionEvent.ACTION_UP) {
                        mCustomLocationProvider.stopMove();
                    }
                }
                return false;
            }
        });
        //==========================================================================================

        //to add lines starting from user's location
        spawnAREntityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spawnAREntity();
            }
        });

        int idx0=0;
        for (String strL : tabL){
            listLoc.add(strL);
            listLatLongAlt.add(tabAlatLong[idx0]);
            distList.add(0.0);
            idx0++;
        }
    }// end of onCreate method

    // update user's location to a specified location (here coming from GPS)
    public void setGPSlocation(Location locationS){
        if (GPS_on) {
            double lat = locationS.getLatitude();
            double lon = locationS.getLongitude();
            double alti = 1.7; //arbitrary value

            Location mockLocationM = new android.location.Location("");

            mockLocationM.setLatitude(lat);
            mockLocationM.setLongitude(lon);
            mockLocationM.setAltitude(alti);

            // Here we set the location provided by the gps
            mARGeoMoverioFragment.setMockLocation(mockLocationM);
        }
    }

    /**
     *
     * @param arRGeoFragment
     * @param lati    -> Ex: -4.472785
     * @param longi   -> Ex: 48.418078
     * @param alti    -> Ex: 0 meters
     * @param id3Dobj -> Ex: R.raw.my3d_obj
     * @param option  -> Val: 1=obj rotates | else=no option
     */
    public void add3Dobj(ARGeoFragment arRGeoFragment, AREntities mAREntities, Number lati, Number longi, Number alti, final int id3Dobj, int option){
        ARGeoRenderer arGeoRenderer = arRGeoFragment.getARGeoRenderer();
        RajawaliScene scene = arGeoRenderer.getCurrentScene();
        // note that ARGeo's 3D space coordinates  matches the Android coordinate-system:
        // so axis are mapped this way:
        // X = longitude
        // Z = -latitude,
        // Y = altitude
        AREntity iAREntity = new AREntity(arRGeoFragment);

        ExampleLocationAbsolute eAbsolute = new ExampleLocationAbsolute();
        eAbsolute.setK(lati);
        eAbsolute.setA(longi);
        eAbsolute.setAltitude(alti);
        iAREntity.setLocationAbsolute(eAbsolute);

       /* FrameLayout frameLayout = (FrameLayout) getLayoutInflater().inflate(R.layout.navisu3d_view, mARGeoMoverioFragment.getRootView(), false);
        frameLayout.setPadding(0, 0, 0, 200);


        ARWidget arWidgetYellow = new ARWidget(frameLayout);

        iAREntity.setARWidget(arWidgetYellow);*/

        LoaderOBJ loaderObj;//LoaderOBJ to load .obj 3D Model
        loaderObj = new LoaderOBJ(getResources(), arRGeoFragment.getARGeoRenderer().getTextureManager(), id3Dobj);
            try {
                loaderObj.parse();
                Object3D object3D = loaderObj.getParsedObject();
                object3D.setScale(1);
                // Object3D is added to the AREntity
                iAREntity.setObject3D(object3D);
                if (option==1) {
                    // here the object can be made rotate
                    Vector3 axis = new Vector3(0, 1, 0);
                    Animation3D anim = new RotateOnAxisAnimation(axis, 0, 360);
                    anim.setDurationMilliseconds(8000);
                    anim.setRepeatMode(Animation.RepeatMode.INFINITE);
                    anim.setInterpolator(new AccelerateDecelerateInterpolator());
                    anim.setTransformable3D(object3D);
                    arRGeoFragment.getARGeoRenderer().getCurrentScene().registerAnimation(anim);
                    anim.play();
                }
            } catch (ParsingException e) {
                e.printStackTrace();
            }

        // We add the AREntity to the list of AREntity
        mARGeoMoverioFragment.addAREntity(iAREntity);
    }

    // |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||


    // This callback is called when ARGeo have been initialized and is ready to handle AREntities,
    // this is where you create and add AREntities to ARGeoFragment
    @Override
    public void onARGeoInitialized(final ARGeoFragment arRGeoFragment) {
        //inits location at point O--------------------------------------------------------
        android.location.Location mockLocation = new android.location.Location("");
        mockLocation.setLatitude(48.418682);
        mockLocation.setLongitude(-4.472090);
        mARGeoMoverioFragment.setMockLocation(mockLocation);
        //---------------------------------------------------------------------------------
        AREntities mAREntities = new AREntities();// kind of ArrayList to contain all the AREntity

        mARGeoMoverioFragment.setDistanceFilterEnable(false);
        mARGeoMoverioFragment.setDistanceFilter(600);

        int idx0=0;
        Stack<Vector3> waypointsP = new Stack<Vector3>();
        for (String strL : tabL){ // loop to add all sample locations
            double[] hLatLong = tabAlatLong[idx0];
            // This is an AREntity, it represents a localized AR element
            AREntity exampleAREntityEnsta = new AREntity(arRGeoFragment);
            // This is an LocationAbsolute object, it holds the location in the world of the AREntity
            // expressed in latitude, longitude and altitude
            ExampleLocationAbsolute absLocationEnsta = new ExampleLocationAbsolute();
            absLocationEnsta.setK(hLatLong[1]);
            absLocationEnsta.setA(hLatLong[2]);
            absLocationEnsta.setAltitude(hLatLong[0]);
            // Note that we pass the ARGeoMoverioFragment's root as the rootView but we don't immediately attach the
            // inflated view to it, the widget will be later attached by the ARGeo engine
            LinearLayout exampleARViewEnsta = (LinearLayout) getLayoutInflater().inflate(R.layout.test1_ranavisu_ar_widget_view, mARGeoMoverioFragment.getRootView(), false);
            ((TextView) exampleARViewEnsta.findViewById(R.id.info_textview)).setText(strL);
            /*if (strL.equals(Test1RaNavisu.this.getString(R.string.pointO)) || strL.equals(Test1RaNavisu.this.getString(R.string.coursTennis))
                    || strL.equals(Test1RaNavisu.this.getString(R.string.terrainBasket))  || strL.equals(Test1RaNavisu.this.getString(R.string.pointP))
                    || strL.equals(Test1RaNavisu.this.getString(R.string.pointC) ) || strL.equals(Test1RaNavisu.this.getString(R.string.pointK1)) || strL.equals(Test1RaNavisu.this.getString(R.string.pointK2))) {
                ((ImageView) exampleARViewEnsta.findViewById(R.id.poi_image_imageview)).setImageResource(R.drawable.logo_ranavisu);
            }else*/ if (strL.equals(Test1RaNavisu.this.getString(R.string.place_d_armes))){
                ((ImageView) exampleARViewEnsta.findViewById(R.id.poi_image_imageview)).setImageResource(R.drawable.logoensta_alpha);
            }else if (strL.substring(0, 3).equals("cor")){
                ((ImageView) exampleARViewEnsta.findViewById(R.id.poi_image_imageview)).setImageResource(R.drawable.marker_b);
            }else {
                ((ImageView) exampleARViewEnsta.findViewById(R.id.poi_image_imageview)).setImageResource(R.drawable.markerr);
            }
            // This is an ARWidget, it holds an Android native View object that will be shown as an AR element
            // and some configuration parameters
            ARWidget exampleARWidgetEnsta = new ARWidget(exampleARViewEnsta);

            // Widget size decreases when it gets far from the user
            exampleARWidgetEnsta.setScaleEnable(true);
            exampleAREntityEnsta.setDistanceScaleAttenuation(0.008f);
            exampleARWidgetEnsta.setBaseDepth(30);
            if (strL.substring(0, 3).equals("cor")){
                exampleARWidgetEnsta.setBaseDepth(30);
            }

            // We set the ARWidget and the LocationAbsolute to the AREntity we just created
            exampleAREntityEnsta.setLocationAbsolute(absLocationEnsta);
            exampleAREntityEnsta.setARWidget(exampleARWidgetEnsta);

            /*LoaderOBJ loaderObj;//LoaderOBJ to load .obj 3D Model
            loaderObj = new LoaderOBJ(getResources(), arRGeoFragment.getARGeoRenderer().getTextureManager(), R.raw.marker_obj);
            try {
                loaderObj.parse();
                Object3D object3D = loaderObj.getParsedObject();
                object3D.setScale(1);
                // Object3D is added to the AREntity
                exampleAREntityEnsta.setObject3D(object3D);
            } catch (ParsingException e) {
                e.printStackTrace();
            }*/

            //fired every time the location of this changes relatively to your location
            // so it gets fired if the entity location is changed at runtime and even when your location changes
            exampleAREntityEnsta.setOnAREntityLocationChangedListener(new OnAREntityLocationChangedListener() {
                @Override
                public void onAREntityLocationChanged(final AREntity arEntity) {
                    // We update the distance shown in the AR widget, we do this for all the entities
                    final TextView distanceTextView = (TextView) arEntity.getARWidget().getView().findViewById(R.id.location_textview);
                    final TextView infoTextView = (TextView) arEntity.getARWidget().getView().findViewById(R.id.info_textview);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            double dist = arEntity.getARDistanceFlat(); // We get it in meters
                            double alt1 = arEntity.getLocationAbsolute().getAltitude();

                            int idxp = listLoc.indexOf(infoTextView.getText());
                            DecimalFormat df = new DecimalFormat("#.##m");
                            distanceTextView.setText(df.format(dist));
                            double lastDist = distList.get(idxp);
                            double diff = Math.abs(dist - lastDist);
                            double offset = 0;
                            ExampleLocationAbsolute arLocAbs = (ExampleLocationAbsolute) arEntity.getLocationAbsolute();
                            // DO something when the entity is at a certain distance; here when an entity is far, it is set a higher altitude
                            if (diff > 5.5 || (diff > 0.5 && dist <5)) {
                                offset = dist*0.15;
                                if (dist > 50) {
                                    offset = dist*0.5;
                                }
                                if (dist<2){
                                    offset=7;
                                }
                                /*
                                if (dist < 3) {
                                    offset = dist*0.4;
                                } else if (dist < 40) {
                                    offset = 1.2 +dist*0.15;
                                } else if (dist >= 40) {
                                    offset = 7.2 + dist * 0.045;
                                }*/

                                double alt2 = arEntity.getLocationAbsolute().getAltitude();//arEntity.recalculateARAbsoluteCoordinates();
                                if (dist<2) {
                                    arLocAbs.setAltitude(offset);
                                    arEntity.setLocationAbsolute(arLocAbs);
                                    Logger.getAnonymousLogger().log(Level.INFO, "LOC update->  " + Double.toString(alt2) + " = OLD( " + Double.toString(alt1) + " ) | DIST (current: " + Double.toString(dist) + " ;old: " + Double.toString(lastDist) + " )");

                                }/*else if (dist>8) {
                                    arLocAbs.setAltitude(2);
                                    arEntity.setLocationAbsolute(arLocAbs);
                                    Logger.getAnonymousLogger().log(Level.INFO, "LOC update->  " + Double.toString(alt2) + " = OLD( " + Double.toString(alt1) + " ) | DIST (current: " + Double.toString(dist) + " ;old: " + Double.toString(lastDist) + " )");

                                }*/
                            }
                            distList.set(idxp, dist);
                            //arEntity.getARWidget().setPosition(0, (float) offset, 0, (float) offset, 0, 0, 0);
                        }
                    });

                }
            });

            mAREntities.add(exampleAREntityEnsta);
            idx0++;
        }
        arRGeoFragment.setMaxScalingDistance(85);
        arRGeoFragment.setAREntities(mAREntities);

        //add3Dobj(arRGeoFragment, mAREntities, 48.418246, -4.472069, 0.5, R.raw.plane_bat_e, 3);

    }

    // |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||

    // Location first resolved callback
    @Override
    public void onLocationResolved(final Location location) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                DecimalFormat df = new DecimalFormat("#.######");
                String latitude = df.format(location.getLatitude());
                String longitude = df.format(location.getLongitude());

                Toast.makeText(Test1RaNavisu.this, "Location resolved: " + latitude + " " + longitude, Toast.LENGTH_SHORT).show();
                mActualLocationTextView.setText(latitude + ", " + longitude);
            }
        });
    }

    // |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||

    // Location changed callback
    @Override
    public void onLocationChanged(final Location location) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                DecimalFormat df = new DecimalFormat("#.######");
                String latitude = df.format(location.getLatitude());
                String longitude = df.format(location.getLongitude());

                //Toast.makeText(Test1RaNavisu.this, "Location changed: " + latitude + " " + longitude, Toast.LENGTH_SHORT).show();
                mActualLocationTextView.setText(latitude + ", " + longitude);
            }
        });
    }

    // |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||

    @Override
    protected void onResume() {
        super.onResume();
        // We are not using 3D mode in this example so we make sure that we are in 2D mode
        mDisplayControl.setMode(DisplayControl.DISPLAY_MODE_2D, false);
        // We get rid of the System Menu Bar
        SystemBarsCtrl.hide(getWindow());
    }

    // |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||

    private void spawnAREntity() {

        // The AREntities are handled inside a separate thread and any action performed on them must be executed on that separate thread
        // so we cannot directly add, remove or modify AREntities directly, any operation involving AREntities must be executed like how is shonw below,
        // namely by adding a Runnable to a ARGeo render cycle.
        // The Runnable you pass to mARGeoMoverioFragment.addRenderCycleRunnable will be executed ONCE in the proper thread during the first available render cycle
        // (by default a render cycle is triggered at 60fps, means 60 times every second)
        mARGeoMoverioFragment.addRenderCycleRunnable(new Runnable() {
            @Override
            public void run() {
                final AREntity arEntity = new AREntity(mARGeoMoverioFragment);

                ExampleLocationAbsolute locationAbsolute = new ExampleLocationAbsolute();
                double latit = mARGeoMoverioFragment.data.currentLocation.getLatitude();
                double longit = mARGeoMoverioFragment.data.currentLocation.getLongitude();
                double altit = mARGeoMoverioFragment.data.currentLocation.getAltitude();

                locationAbsolute.setK(mARGeoMoverioFragment.getMockLocation().getLatitude());
                locationAbsolute.setA(mARGeoMoverioFragment.getMockLocation().getLongitude());
                locationAbsolute.setAltitude(0);
                arEntity.setLocationAbsolute(locationAbsolute);

                LinearLayout linearLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.test1_ranavisu_ar_3d_view, mARGeoMoverioFragment.getRootView(), false);
                linearLayout.findViewById(R.id.remove_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        removeAREntity(arEntity);
                    }
                });
                ARWidget arWidget = new ARWidget(linearLayout);
                arWidget.setScaleEnable(false);
                arEntity.setARWidget(arWidget);
               // arWidget.setBaseDepth(28);

                Stack<Vector3> points = new Stack<Vector3>();
                int[] colors ={Color.CYAN,Color.YELLOW, Color.RED,Color.GREEN, Color.BLUE};

                    points.add(new Vector3(0, 0, 0));
                    Vector3 curPt = new Vector3(longit, 0, -latit);
                    int nbPts = 5;
                    LinkedList<Integer> already = new LinkedList<Integer>();
                for (int i=0;i<nbPts-1; i++){
                        int idxRnd = alea.nextInt(20);
                        while (already.contains(idxRnd) ||  mARGeoMoverioFragment.getAREntities().get(idxRnd).getLocationAbsolute().getLocation().equals(locationAbsolute.getLocation())){
                            idxRnd = alea.nextInt(20);
                        }
                        LocationAbsolute locaDest = mARGeoMoverioFragment.getAREntities().get(idxRnd).getLocationAbsolute();
                        Vector3 nextPt = new Vector3((locaDest.getLongitude()-longit)*ARGeoData.METERS_IN_ONE_EARTH_DEGREE_APPROXIMATION,0,-(locaDest.getLatitude()-latit)*ARGeoData.METERS_IN_ONE_EARTH_DEGREE_APPROXIMATION);
                        points.add(nextPt);
                        already.add(idxRnd);
                }

               /* Route rPath = new Route(waypoints, arEntity);
                if (routes.size()>0) {
                    Route rou = routes.get(0);
                    removeAREntity(rou.getArRouteEntity());
                    routes.remove(0);
                }
                routes.add(rPath);*/

                Object3D line = new Line3D(points, 80,Color.YELLOW);
                Material material = new Material();
                //material.useVertexColors(true); // in case a list of colors are given to Line3D constructor (last parameter)
                material.enableLighting(false);
                line.setMaterial(material);

                arEntity.setObject3D(line);

                mARGeoMoverioFragment.addAREntity(arEntity);
            }

        });}

    // |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||

    private void removeAREntity(final AREntity arEntity) {
        // To remove an AREntity we need to use mARGeoMoverioFragment.addRenderCycleRunnable
        mARGeoMoverioFragment.addRenderCycleRunnable(new Runnable() {
            @Override
            public void run() {
                mARGeoMoverioFragment.removeAREntity(arEntity);
            }
        });
    }

    // This callback is fired every time a render cycle is triggered, if you need to trigger some operation continuously
    // that involves your AREntities you should do in into this callback
    @Override
    public void onARGeoRenderCycle(ARGeoFragment arGeoFragment) {

        // Here we just rotate all the AREntities a little bit in every render cycle
        /*for (AREntity arEntity : arGeoFragment.getAREntities()) {
            //arEntity.getObject3D().setRotation(arEntity.getObject3D().getRotX() + 0.5, arEntity.getObject3D().getRotY() + 1, arEntity.getObject3D().getRotZ() + 0.25);
        }*/


    }
/*
    public void onProvidedLocationChanged(Location manualLocationMove){
        setGPSlocation(manualLocationMove);
    }*/

    // This is how we handle native object picking, this callback will be fired when a click on a Object 3D occurs
    @Override
    public void onObjectPicked(Object3D object) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Test1RaNavisu.this, "Native object picker click", Toast.LENGTH_SHORT).show();
                //AREntity aro = object.setp
                //double alt2 = arEntity.getLocationAbsolute().getAltitude();//arEntity.recalculateARAbsoluteCoordinates();
                /*if (dist<2) {
                    arLocAbs.setAltitude(offset);
                    arEntity.setLocationAbsolute(arLocAbs);*/
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    // |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||

    private class CustomLocationProvider extends LocationProvider {

        private Handler mHandler;
        private Runnable mUpdateTask;
        private Location mLocation = new Location("");

        private int mDirection;

        public CustomLocationProvider(OnProvidedLocationChangedListener onProvidedLocationChangedListener) {
            super(onProvidedLocationChangedListener);
        }

        public CustomLocationProvider(OnProvidedLocationChangedListener onProvidedLocationChangedListener, Runnable connectionFailedCallback) {
            super(onProvidedLocationChangedListener, connectionFailedCallback);
        }

        public void startMove(int direction) {
            // This will continuously trigger the mUpdateTask
            mDirection = direction;
            mHandler.removeCallbacks(mUpdateTask);
            mHandler.post(mUpdateTask);
            Logger.getAnonymousLogger().log(Level.WARNING, "startMove(int dir= "+direction+" )");
        }

        public void stopMove() {
            // This will stop the mUpdateTask triggering
            mHandler.removeCallbacks(mUpdateTask);
            Logger.getAnonymousLogger().log(Level.WARNING, "stopMove");
        }

        @Override
        public void initLocationProvider(Context context) {
            mHandler = new Handler();

            // Here you should initialize your provider, in this example we initialize a location update task
            mUpdateTask = new Runnable() {
                public void run() {
                    mHandler.postAtTime(mUpdateTask, SystemClock.uptimeMillis() + 50);

                    // Here is where the location update occurs, on each trigger we advance (or back off) by 25cm in the direction we are looking at
                    mLocation.setLatitude(mARGeoMoverioFragment.getMockLocation().getLatitude() + mDirection * 0.25f * Math.cos(mARGeoMoverioFragment.getOrientationVector()[0]) / ARGeoData.METERS_IN_ONE_EARTH_DEGREE_APPROXIMATION);
                    mLocation.setLongitude(mARGeoMoverioFragment.getMockLocation().getLongitude() + mDirection * 0.25f * Math.sin(mARGeoMoverioFragment.getOrientationVector()[0]) / ARGeoData.METERS_IN_ONE_EARTH_DEGREE_APPROXIMATION);
                    mLocation.setAltitude(1.7);
                    // By calling this line we notify ARGeo that a new location have been discovered
                    // (the onProvidedLocationChangedListener in this case is the ARGeo instance we passed in the CustomLocationProvider constructor)
                    // calling this callback is mandatory, otherwise your custom location provider will never notify ARGeo for discovered locations
                    onProvidedLocationChangedListener.onProvidedLocationChanged(mLocation);
                    mARGeoMoverioFragment.setMockLocation(mLocation);
                }
            };
        }

        @Override
        public void startLocationTracking() {
            // Here you should make yout tracker start the tracking,
            // in this example we don't need to start anything, instead we just notify
            // the listener that the tracker discovered a location (mLocation is 0,0 now)
            onProvidedLocationChangedListener.onProvidedLocationChanged(mLocation);
            mARGeoMoverioFragment.setMockLocation(mLocation);

        }

        @Override
        public void stopLocationTracking() {
            // We didn't start anything so we don't have to stop anything
        }
    }

}

