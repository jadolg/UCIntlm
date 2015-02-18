package uci.ucintlm.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.widget.Toast;




import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;

import uci.ucintlm.R;
import uci.ucintlm.util.ServiceUtils;
import uci.ucintlm.util.WifiSettings;

/**
 * Esta clase configura automáticamente la wifi a la que nos conectemos si el servicio está activo
 */
public class WifiAutoConfig extends BroadcastReceiver {

    /**********Este código consulta los parámetros de la WiFi del sistema************/

	public static Proxy getProxySelectorConfiguration(URI uri) throws Exception
	{
		ProxySelector defaultProxySelector = ProxySelector.getDefault();
		Proxy proxy = null;
		List<Proxy> proxyList = defaultProxySelector.select(uri);
		if (proxyList.size() > 0)
		{
			proxy = proxyList.get(0);
		}
		else
			throw new Exception("Not found valid proxy configuration!");

		return proxy;
	}

	public static Proxy getCurrentProxyConfiguration(URI uri) throws Exception
	{
		Proxy proxyConfig;
		proxyConfig = getProxySelectorConfiguration(uri);
		if (proxyConfig == null)
		{
			proxyConfig = Proxy.NO_PROXY;
		}

		return proxyConfig;
	}

	public  Proxy getCurrentHttpProxyConfiguration() throws Exception
	{
		URI uri = new URI("http", "wwww.google.it", null, null);
		return getCurrentProxyConfiguration(uri);
	}
    /******************************************************************************/


	@Override
	public void onReceive(Context context, Intent intent) {
		NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
		SharedPreferences settings = context.getSharedPreferences("UCIntlm.conf", Context.MODE_PRIVATE);

		String outputport = settings.getString("outputport", "8080");
        String bypass = settings.getString("bypass", "");

		if(info != null) {
			if(info.isConnected()) {
//                Toast.makeText(context,  context.getString(R.string.Connected), Toast.LENGTH_SHORT).show();
				Proxy p = null;
				try {
					p = getCurrentHttpProxyConfiguration();
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (p != null) {
					if (ServiceUtils.isMyServiceRunning(context)) {
						if (p.type() != Proxy.Type.HTTP && p.toString() != "HTTP@127.0.0.1:" + outputport) {
							Toast.makeText(context,  context.getString(R.string.OnProxy), Toast.LENGTH_SHORT).show();
							WifiSettings.setWifiProxySettings(context,Integer.valueOf(outputport),bypass);
						}

					} else {
                        if (p.type() != Proxy.Type.DIRECT) {
                            Toast.makeText(context,  context.getString(R.string.OnNoProxy), Toast.LENGTH_SHORT).show();
                            WifiSettings.unsetWifiProxySettings(context);
                        }
//
					}
				} else {
//                    Toast.makeText(context, "Ha ocurrido un error obteniendo la configuración del Proxy", Toast.LENGTH_SHORT).show();
                }

				// e.g. To check the Network Name or other info:
//                WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
//                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//                String ssid = wifiInfo.getSSID();
			} else {
//				Toast.makeText(context, "Desconectado",
//						Toast.LENGTH_SHORT).show();
			}
		}
	}

}
