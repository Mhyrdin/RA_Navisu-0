package ranavisu.bt200.demo;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.CompoundButton;
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
import net.joinpad.arrakis.layout.SystemBarsCtrl;
import net.joinpad.locationprovider.LocationProvider;

import org.java_websocket.drafts.Draft_10;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
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
import ranavisu.bt200.R;
import ranavisu.bt200.bean.ExampleLocationAbsolute;
import ranavisu.bt200.util.ARgeoData;
import ranavisu.bt200.util.ARgeoEntity;
import ranavisu.bt200.util.GPSTracker;
import ranavisu.bt200.util.I_LocationSet;
import ranavisu.bt200.util.ParserXML;
import ranavisu.bt200.util.WSclient;

// net.joinpad.locationprovider.GpsTracker;
//import jp.epson.moverio.bt200.SensorControl;

public class RA_Navisu extends Activity implements OnARGeoInitializedListener, OnLocationChangedListener, OnLocationResolvedListener,
        OnARGeoRenderCycleListener, OnObjectPickedListener, I_LocationSet {

    // IP address of the computer on which NaVisu is running
    private final String IP_NAVISU = "192.168.173.1"; // adapt to fit your configuration !!!!!!!!!!!!!

    private String TAG = "RA-NaVisu";
    private WSclient wsc; // websocket client
    private final String ROUTE = "Route0.nds"; // example static data file

    //Array to store static AR entities (lighthouses, beacons, buoys, reefs) + planned route
    private List<ARgeoData> static_ARgeoDataArray;
    private DisplayControl mDisplayControl;
    private CustomLocationProvider mCustomLocationProvider;

    private ARGeoMoverioFragment mARGeoMoverioFragment;

    private Random alea=new Random();

    private Button bStaticData;
    private TextView mActualLocationTextView;

    private ToggleButton GPSswitch;
    private boolean GPS_on;// if false, then you can choose a mock location
    private boolean staticData_loaded = false;// if false, then you can choose a mock location
    private boolean gpsFirst=true;// to suggest activating GPS only once
    GPSTracker my_gps;// handle BT-200 GPS updating

    //----------------------------- Some georeferenced places --------------------------------------

    /*private ArrayList <String> listLoc = new ArrayList <String>();
    private ArrayList <double[]> listLatLongAlt = new ArrayList <double[]>();
    private LinkedList<Double> distList =new LinkedList<Double>();*/

    //----------------------------------------------------------------------------------------------
    double[] newLoc =new double[3];// newLoc=user's location

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //super.onCreate(savedInstanceState);

        mDisplayControl = new DisplayControl(RA_Navisu.this);

        setContentView(R.layout.layout_ranavisu);

        mARGeoMoverioFragment = new ARGeoMoverioFragment();

        mARGeoMoverioFragment.setOnARGeoInitializedListener(this);
        mARGeoMoverioFragment.setOnLocationResolvedListener(this);
        mARGeoMoverioFragment.setOnLocationChangedListener(this);
        mCustomLocationProvider = new CustomLocationProvider(mARGeoMoverioFragment);
        mARGeoMoverioFragment.setLocationProvider(mCustomLocationProvider);
        mARGeoMoverioFragment.setOnARGeoRenderCycleListener(this);
        mARGeoMoverioFragment.setOnObjectPickedListener(this);

        getFragmentManager().beginTransaction().replace(R.id.container, mARGeoMoverioFragment).commit();

        // enable communication with NaVisu server thanks to websocket
        initWSC();

        /*
        getting access to the various widget on our layout .........................................
         */
        mActualLocationTextView = (TextView) findViewById(R.id.actual_location_textview);
        bStaticData = (Button) findViewById(R.id.bStaticData);

        //==========================================================================================
        /*
         updating position thanks to GPS data ......................................................
          */
        GPSswitch = (ToggleButton) findViewById(R.id.GPSswitch);
        GPS_on = false;
        my_gps = new GPSTracker(RA_Navisu.this);

        GPSswitch.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
                GPS_on = !GPS_on;
                if (GPS_on) {
                    GPSswitch.setTextColor(Color.GREEN);
                    // check if GPS enabled
                    if (gpsFirst) {
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
                        gpsFirst = false;
                    }
                } else {
                    GPSswitch.setTextColor(Color.RED);
                }
            }
        });

        // handling initial pull of static data
        bStaticData.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionevent) {
                if (!staticData_loaded) {
                    try {
                        ws_request("NaVigationDataSetCmd");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    staticData_loaded = true;
                }
                return false;
            }
        });


        //==========================================================================================

    }// end of onCreate method

    // update user's location to a specified location (here coming from GPS)
    public void setGPSlocation(Location locationS){
        if (GPS_on) {
            double lat = locationS.getLatitude();
            double lon = locationS.getLongitude();
            double alti = 1.7; //arbitrary value

            Location mockLocationM = new Location("");

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
        Location mockLocation = new Location("");
        mockLocation.setLatitude(48.327276);
        mockLocation.setLongitude(-4.599837);
        mARGeoMoverioFragment.setMockLocation(mockLocation);

    }

    // |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||

    public void loadStaticData(){

        mARGeoMoverioFragment.addRenderCycleRunnable(new Runnable() {
            @Override
            public void run() {
        //---------------------------------------------------------------------------------
        AREntities mAREntities = new AREntities();// kind of ArrayList to contain all the AREntity

        int idx0=0;
        Stack<Vector3> waypointsP = new Stack<Vector3>();
        for (ARgeoData argd : static_ARgeoDataArray){ // loop to add all sample locations
            double[] hLatLong = {0.0, argd.getLat(), argd.getLon()};
            // This is an AREntity, it represents a localized AR element
            ARgeoEntity exampleAREntityEnsta = new ARgeoEntity(mARGeoMoverioFragment, argd);
            // This is an LocationAbsolute object, it holds the location in the world of the AREntity
            // expressed in latitude, longitude and altitude
            ExampleLocationAbsolute absLocationEnsta = new ExampleLocationAbsolute();
            absLocationEnsta.setK(hLatLong[1]);
            absLocationEnsta.setA(hLatLong[2]);
            absLocationEnsta.setAltitude(hLatLong[0]);
            // Note that we pass the ARGeoMoverioFragment's root as the rootView but we don't immediately attach the
            // inflated view to it, the widget will be later attached by the ARGeo engine
            LinearLayout exampleARViewEnsta = (LinearLayout) getLayoutInflater().inflate(R.layout.test1_ranavisu_ar_widget_view, mARGeoMoverioFragment.getRootView(), false);
            ((TextView) exampleARViewEnsta.findViewById(R.id.info_textview)).setText(argd.getName());
            if (argd.getImageAddress()!=null) {
                AssetManager assetManager = getAssets();
                InputStream is = null;
                try {
                    is = assetManager.open(argd.getImageAddress());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Bitmap  bitmap = BitmapFactory.decodeStream(is);
               // int resID = getResources().getIdentifier(imNameNoExtension, "drawable", getPackageName());
                //Drawable dIM = Drawable.createFromPath(argd.getImageAddress());
                ((ImageView) exampleARViewEnsta.findViewById(R.id.poi_image_imageview)).setImageBitmap(bitmap); //setImageResource(resID);
            } else {
                ((ImageView) exampleARViewEnsta.findViewById(R.id.poi_image_imageview)).setImageResource(R.drawable.marker);
            }
            // This is an ARWidget, it holds an Android native View object that will be shown as an AR element
            // and some configuration parameters
            ARWidget arWidget = new ARWidget(exampleARViewEnsta);

            // Widget size decreases when it gets far from the user
            arWidget.setScaleEnable(true);
            //exampleAREntityEnsta.setDistanceScaleAttenuation(0.05f);
            arWidget.setBaseDepth(350);// distance in meters at which the icon is at scale 1:1 / if scaleEnaled, icon size is increased below this distance and decreased beyond

            // We set the ARWidget and the LocationAbsolute to the AREntity we just created
            exampleAREntityEnsta.setLocationAbsolute(absLocationEnsta);
            exampleAREntityEnsta.setARWidget(arWidget);

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

                            //int idxp = listLoc.indexOf(infoTextView.getText());
                            DecimalFormat df = new DecimalFormat("#.##m");
                            distanceTextView.setText(df.format(dist));
                            Logger.getAnonymousLogger().log(Level.INFO, "DIST  "+df.format(dist));
                            //double lastDist = distList.get(idxp);
                            //double diff = Math.abs(dist - lastDist);
                            double offset = 0;
                            ExampleLocationAbsolute arLocAbs = (ExampleLocationAbsolute) arEntity.getLocationAbsolute();
                            // DO something when the entity is at a certain distance; here when an entity is far, it is set a higher altitude


                            double alt2 = arEntity.getLocationAbsolute().getAltitude();//arEntity.recalculateARAbsoluteCoordinates();
                            if (dist<2) {
                                arLocAbs.setAltitude(offset);
                                arEntity.setLocationAbsolute(arLocAbs);
                                Logger.getAnonymousLogger().log(Level.INFO, "LOC update->  " + Double.toString(alt2) + " = OLD( " + Double.toString(alt1) + " ) | DIST (current: " + Double.toString(dist) + " )");

                            }
                        }
                    });

                }
            });

            mAREntities.add(exampleAREntityEnsta);
            idx0++;
        }
                //mARGeoMoverioFragment.setMaxScalingDistance(320);// beyond that distance, ARenties are not displayed
                mARGeoMoverioFragment.setAREntities(mAREntities);
            }

        });}

    // Location first resolved callback
    @Override
    public void onLocationResolved(final Location location) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                DecimalFormat df = new DecimalFormat("#.######");
                String latitude = df.format(location.getLatitude());
                String longitude = df.format(location.getLongitude());

                Toast.makeText(RA_Navisu.this, "Location resolved: " + latitude + " " + longitude, Toast.LENGTH_SHORT).show();
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
                Toast.makeText(RA_Navisu.this, "Native object picker click", Toast.LENGTH_SHORT).show();
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
// -------------------------------------------------------------------------------------------------
    /*
    * AR commands via websockets
     */

    public void initWSC(){
        Thread thread0 = new Thread(new Runnable(){
            @Override
            public void run() {
                Log.d("#AAA", "Current Time =" + new Date());
                try {
                    wsc = new WSclient(new URI("ws://"+IP_NAVISU+":8787/navigation"), new Draft_10());
                    wsc.connect();
                } catch (URISyntaxException ex) {
                    Logger.getLogger(TestWiFiActivity.class.getName()).log(Level.WARNING, null, ex);
                }
            }
        });
        thread0.start();
    }

    /**
     *
     * @param cmd among {"NaVigationDataSetCmd", "OwnerShipCmd"}
     * @throws IOException
     */
    public void ws_request(String cmd) throws IOException {
        new WebSock(cmd).execute();
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


    //************************************************************************************************
    private class WebSock extends AsyncTask<String, Void, Void> {

        private String cmdInp;
        private ParserXML customParser;
        private String resultBig;


        public WebSock(String cmdIn) throws IOException {
            this.cmdInp = cmdIn;
        }

        @Override
        protected Void doInBackground(String... strIn) {

            Logger.getLogger(WebSock.class.getName()).log(Level.WARNING, "Init cmd...Request");
            try {
                if (cmdInp.equals("NaVigationDataSetCmd")) {
                    cmdNavigationDataSetRequest();
                    static_ARgeoDataArray = handleRepStaticData();
                } else if (cmdInp.equals("OwnerShipCmd")) {
                    cmdOwnerShipRequest();
                    handleRepShip();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void resulto) {
            if (static_ARgeoDataArray!=null) {
                bStaticData.setText("static data initialised");
                loadStaticData();
            }else {
                bStaticData.setText("static data initialisation failed");
            }
        }

        public String getCmdInp() {
            return cmdInp;
        }
        public void setCmdInp(String cmd) {
            cmdInp = cmd;
        }

        private List<ARgeoData> handleRepStaticData() {
            String message = "";
            List<String> messageList = wsc.getListID_Rep();
            boolean done = false;
            while (!done) {
                if (messageList.size() > 0 && messageList.get(0) != null) {
                    message = messageList.get(messageList.size() - 1);
                    done = true;
                }
            }
            Log.d("REC: ", "Received: " + message);

            return response(message);
        }

        private List<ARgeoData> handleRepShip() {
            String message = "";
            List<String> messageList = wsc.getListID_Rep();
            boolean done = false;
            while (!done) {
                if (messageList.size() > 0 && messageList.get(0) != null) {
                    message = messageList.get(messageList.size() - 1);
                    done = true;
                }
            }
            Log.d("REC: ", "Received: " + message);

            return response(message);
        }

        public void closeSo() {
            Logger.getLogger(WebSock.class.getName()).log(Level.WARNING, "Init close");
            wsc.close();
            Logger.getLogger(WebSock.class.getName()).log(Level.WARNING, "CLOSE");
        }

        public void cmdNavigationDataSetRequest() throws IOException {
            String cmd = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><arCommand><cmd>NaVigationDataSetCmd</cmd><arg>%s</arg></arCommand>", ROUTE);
            wsc.send(cmd);
        }

        public void cmdOwnerShipRequest() throws IOException {
            String cmd = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><arCommand><cmd>OwnerShipCmd</cmd><arg>%s</arg></arCommand>";
            wsc.send(cmd);
        }

        private List<ARgeoData> response(String resp) {
            String ans = "";
            Logger.getAnonymousLogger().log(Level.WARNING, resp);
            List<ARgeoData> argeoDatasList;
            if (resp != null) {
                customParser = new ParserXML(resp);
                customParser.process();
                argeoDatasList = customParser.getARgeoDatas();
                return argeoDatasList;
            }
            return null;
        }

    }


}

