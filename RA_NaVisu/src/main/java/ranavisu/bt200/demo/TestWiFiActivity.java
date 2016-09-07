package ranavisu.bt200.demo;

import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.java_websocket.drafts.Draft_10;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ranavisu.bt200.R;
import ranavisu.bt200.util.ARgeoData;
import ranavisu.bt200.util.ParserXML;
import ranavisu.bt200.util.WSclient;


public class TestWiFiActivity extends Activity {

    private final String IP_NAVISU = "192.168.15.1";

    private ToggleButton bWifi;
    private TextView textRbig;
    private boolean booWifi = false;

    private WSclient wsc;
    private final String ROUTE = "Route0.nds";

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_test_wi_fi);
        bWifi = (ToggleButton) findViewById(R.id.toggleButton);
        textRbig = (TextView) findViewById(R.id.resultList);

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

        bWifi.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
                booWifi = !booWifi;
                if (booWifi) {
                    bWifi.setTextColor(Color.GREEN);
                    try {
                        new WebSock("NaVigationDataSetCmd").execute();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    bWifi.setTextColor(Color.RED);
                }
            }
        });
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
                handleRep();
            } else if (cmdInp.equals("OwnerShipCmd")) {
                cmdOwnerShipRequest();
                handleRep();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void resulto) {
        textRbig.setText(resultBig);
        bWifi.toggle();
    }

    public String getCmdInp() {
        return cmdInp;
    }
    public void setCmdInp(String cmd) {
        cmdInp = cmd;
    }

    private void handleRep() {
        int i = 0;
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

        resultBig = responseDebug(message);
        i++;
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

    private String responseDebug(String resp) {
        String ans = "";
        Logger.getAnonymousLogger().log(Level.WARNING, resp);
        if (resp != null) {
            customParser = new ParserXML(resp);
            customParser.process();
            List<ARgeoData> argeoDatasList = customParser.getARgeoDatas();
            ans += "------------------------";
            Logger.getAnonymousLogger().log(Level.WARNING, "-----------------------------------------------");
            Logger.getAnonymousLogger().log(Level.WARNING, ">>>>>>> " + argeoDatasList.size() + " ARgeoData objects found:");
            ans += ">>> " + argeoDatasList.size() + " ARgeoData objects found:";
            for (ARgeoData argD : argeoDatasList) {
                Logger.getAnonymousLogger().log(Level.WARNING, "argeoData{ " + "name: " + argD.getName() + " | img: " + argD.getImageAddress() + " |  (lat: " + argD.getLat() + " ,lon: " + argD.getLon() + " ) }");
                ans += "\n#  { " + "name: " + argD.getName() + " | img: " + argD.getImageAddress() + " |  (lat: " + argD.getLat() + " ,lon: " + argD.getLon() + " ) }";
            }
            Logger.getAnonymousLogger().log(Level.WARNING, "-----------------------------------------------");
        }
        return ans;
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
