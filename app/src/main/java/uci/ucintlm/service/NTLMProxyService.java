package uci.ucintlm.service;

import uci.ucintlm.R;
import uci.ucintlm.ui.Antlm;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class NTLMProxyService extends Service {
	/*
	 * Este es el servicio que inicial el servidor
	 * Permanece en el área de notificación
	 * */
	private String user = "";
	private String pass = "";
	private String domain = "";
	private String server = "";
	private int inputport = 8080;
	private int outputport = 8080;
	private String bypass = "";
	private ServerTask s;


/**********Este código se utiliza para configurar la WiFi del sistema************/

    public static Object getField(Object obj, String name)
            throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
        Field f = obj.getClass().getField(name);
        Object out = f.get(obj);
        return out;
    }

    public static Object getDeclaredField(Object obj, String name)
            throws SecurityException, NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        Object out = f.get(obj);
        return out;
    }

    public static void setEnumField(Object obj, String value, String name)
            throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
        Field f = obj.getClass().getField(name);
        f.set(obj, Enum.valueOf((Class<Enum>) f.getType(), value));
    }

    public static void setProxySettings(String assign , WifiConfiguration wifiConf)
            throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException{
        setEnumField(wifiConf, assign, "proxySettings");
    }


    WifiConfiguration GetCurrentWifiConfiguration(WifiManager manager)
    {
        if (!manager.isWifiEnabled())
            return null;

        List<WifiConfiguration> configurationList = manager.getConfiguredNetworks();
        WifiConfiguration configuration = null;
        int cur = manager.getConnectionInfo().getNetworkId();
        for (int i = 0; i < configurationList.size(); ++i)
        {
            WifiConfiguration wifiConfiguration = configurationList.get(i);
            if (wifiConfiguration.networkId == cur)
                configuration = wifiConfiguration;
        }

        return configuration;
    }

    void setWifiProxySettings()
    {
        //get the current wifi configuration
        WifiManager manager;
        manager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration config = GetCurrentWifiConfiguration(manager);
        if(null == config)
            return;

        try
        {
            //get the link properties from the wifi configuration
            Object linkProperties = getField(config, "linkProperties");
            if(null == linkProperties)
                return;

            //get the setHttpProxy method for LinkProperties
            Class proxyPropertiesClass = Class.forName("android.net.ProxyProperties");
            Class[] setHttpProxyParams = new Class[1];
            setHttpProxyParams[0] = proxyPropertiesClass;
            Class lpClass = Class.forName("android.net.LinkProperties");
            Method setHttpProxy = lpClass.getDeclaredMethod("setHttpProxy", setHttpProxyParams);
            setHttpProxy.setAccessible(true);

            //get ProxyProperties constructor
            Class[] proxyPropertiesCtorParamTypes = new Class[3];
            proxyPropertiesCtorParamTypes[0] = String.class;
            proxyPropertiesCtorParamTypes[1] = int.class;
            proxyPropertiesCtorParamTypes[2] = String.class;

            Constructor proxyPropertiesCtor = proxyPropertiesClass.getConstructor(proxyPropertiesCtorParamTypes);

            //create the parameters for the constructor
            Object[] proxyPropertiesCtorParams = new Object[3];
            proxyPropertiesCtorParams[0] = "127.0.0.1";
            proxyPropertiesCtorParams[1] = outputport;
            proxyPropertiesCtorParams[2] = null;

            //create a new object using the params
            Object proxySettings = proxyPropertiesCtor.newInstance(proxyPropertiesCtorParams);

            //pass the new object to setHttpProxy
            Object[] params = new Object[1];
            params[0] = proxySettings;
            setHttpProxy.invoke(linkProperties, params);

            setProxySettings("STATIC", config);

            //save the settings
            manager.updateNetwork(config);
            manager.disconnect();
            manager.reconnect();
        }
        catch(Exception e)
        {
        }
    }
    void unsetWifiProxySettings()
    {
        WifiManager manager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration config = GetCurrentWifiConfiguration(manager);
        if(null == config)
            return;

        try
        {
            //get the link properties from the wifi configuration
            Object linkProperties = getField(config, "linkProperties");
            if(null == linkProperties)
                return;

            //get the setHttpProxy method for LinkProperties
            Class proxyPropertiesClass = Class.forName("android.net.ProxyProperties");
            Class[] setHttpProxyParams = new Class[1];
            setHttpProxyParams[0] = proxyPropertiesClass;
            Class lpClass = Class.forName("android.net.LinkProperties");
            Method setHttpProxy = lpClass.getDeclaredMethod("setHttpProxy", setHttpProxyParams);
            setHttpProxy.setAccessible(true);

            //pass null as the proxy
            Object[] params = new Object[1];
            params[0] = null;
            setHttpProxy.invoke(linkProperties, params);

            setProxySettings("NONE", config);

            //save the config
            manager.updateNetwork(config);
            manager.disconnect();
            manager.reconnect();
        }
        catch(Exception e)
        {
        }
    }
/******************************************************************************/

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onDestroy() {
		s.stop();
        unsetWifiProxySettings();
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		user = intent.getStringExtra("user");
		pass = intent.getStringExtra("pass");
		domain = intent.getStringExtra("domain");
		server = intent.getStringExtra("server");
		inputport = Integer.valueOf(intent.getStringExtra("inputport"));
		outputport = Integer.valueOf(intent.getStringExtra("outputport"));
//		bypass = intent.getStringExtra("bypass");
		setWifiProxySettings();
		Log.i(getClass().getName(), "Starting for user " + user+"@"+domain+", server "+server+", input port "+String.valueOf(inputport)+", output port"+String.valueOf(outputport)+" and bypass string: "+bypass);
		s = new ServerTask(user, pass, domain, server, inputport, outputport,bypass);
		s.execute();
		notifyit();

		return START_NOT_STICKY;
	}

	@SuppressWarnings("deprecation")
	public void notifyit() {
		/*
		 * Este método asegura que el servicio permanece en el área de notificación
		 * */
		Notification note = new Notification(R.drawable.ic_launcher, getApplicationContext().getString(R.string.notif1),System.currentTimeMillis());
		Intent i = new Intent(this, Antlm.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);

		PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

		note.setLatestEventInfo(this, "UCIntlm", getApplicationContext().getString(R.string.notif2) + " " + user, pi);

		note.flags |= Notification.FLAG_NO_CLEAR;

		startForeground(1337, note);
	}

}
