package uci.ucintlm.ui;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import android.widget.Toast;

import uci.ucintlm.R;
import uci.ucintlm.service.NTLMProxyService;
import uci.ucintlm.util.Encripter;

public class UCIntlmWidget extends AppWidgetProvider {
	private static final String ACTION_cambiarlayout = "a_cambiarlayout";

	private static boolean isMyServiceRunning(Context context) {
		ActivityManager manager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (NTLMProxyService.class.getName().equals(
					service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onEnabled(Context context) {
		if (isMyServiceRunning(context)) {
			actualizarWidget(context, AppWidgetManager.getInstance(context),
					"on");
		} else {
			actualizarWidget(context, AppWidgetManager.getInstance(context),
					"off");
		}
		super.onEnabled(context);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		String mensaje = "";
		if (isMyServiceRunning(context)) {
			mensaje = "on";
		} else {
			mensaje = "off";
		}
		actualizarWidget(context, appWidgetManager, mensaje);
	}

	public static void actualizarWidget(Context context,
			AppWidgetManager appWidgetManager, String value) {

		RemoteViews remoteViews;

		ComponentName thisWidget;

		int lay_id = 0;

		// Asignamos el layout a la variable lay_id segun el parametro recibido
		// por value
		if (value.equals("on")) {
			// ON
			lay_id = R.layout.main_on;
		}

		if (value.equals("off")) {
			// off
			lay_id = R.layout.main_of;

		}
		// Vamos a acceder a la vista y cambiar el layout segun lay_id
		remoteViews = new RemoteViews(context.getPackageName(), lay_id);
		// identifica a nuestro widget
		thisWidget = new ComponentName(context, UCIntlmWidget.class);

		// Creamos un intent a nuestra propia clase
		Intent intent = new Intent(context, UCIntlmWidget.class);
		// seleccionamos la accion ACTION_cambiarlayout
		intent.setAction(ACTION_cambiarlayout);

		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
				intent, 0);

		/*
		 * Equivalente a setOnClickListener de un boton comun lo asocio con el
		 * layout1 ya que al tocar este se ejecutara la accion y con
		 * pendingIntent
		 */

		remoteViews.setOnClickPendingIntent(R.id.layout1, pendingIntent);

		// actualizamos el widget
		appWidgetManager.updateAppWidget(thisWidget, remoteViews);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		// Controlamos que la accion recibida sea la nuestra
		if (intent.getAction().equals(ACTION_cambiarlayout)) {
			String new_status = "";
			if (loggedOnce(context)) {
				Intent serviceIntent = new Intent(context,
						NTLMProxyService.class);// Proxy

				SharedPreferences settings = context.getSharedPreferences(
						"UCIntlm.conf", Context.MODE_PRIVATE);

				String user, pass, domain, server, inputport, outputport, bypass;

				user = settings.getString("user", "");
				pass = Encripter.decrypt(settings.getString("password", ""));
				domain = settings.getString("domain", "");
				server = settings.getString("server", "");
				inputport = settings.getString("inputport", "");
				outputport = settings.getString("outputport", "");
//				bypass = settings.getString("bypass", "");

				
				serviceIntent.putExtra("user", user);
				serviceIntent.putExtra("pass", pass);
				serviceIntent.putExtra("domain", domain);
				serviceIntent.putExtra("server", server);
				serviceIntent.putExtra("inputport", inputport);
				serviceIntent.putExtra("outputport", outputport);
//				serviceIntent.putExtra("bypass", bypass);
				if (isMyServiceRunning(context)) {
					new_status = "off";

					context.stopService(serviceIntent);// Deteniendo el servicio
														// del proxy
					Toast.makeText(context, context.getString(R.string.notif3),
							Toast.LENGTH_SHORT).show();
				} else {
					context.startService(serviceIntent);// Se inicia el proxy
					new_status = "on";

					Toast.makeText(context, context.getString(R.string.notif1),
							Toast.LENGTH_SHORT).show();
					Toast.makeText(context,
							context.getString(R.string.notif2) + user,
							Toast.LENGTH_LONG).show();

				}
				// Actualizamos el estado del widget.
				AppWidgetManager widgetManager = AppWidgetManager
						.getInstance(context);
				actualizarWidget(context, widgetManager, new_status);
			} else
				Toast.makeText(context, context.getString(R.string.nodata),
						Toast.LENGTH_SHORT).show();
		}

		super.onReceive(context, intent);
	}

	public boolean loggedOnce(Context context) {
		SharedPreferences settings = context.getSharedPreferences(
				"UCIntlm.conf", Context.MODE_PRIVATE);
		String user, pass, domain, server, inputport, outputport;

		user = settings.getString("user", "");
		pass = Encripter.decrypt(settings.getString("password", ""));
		domain = settings.getString("domain", "");
		server = settings.getString("server", "");
		inputport = settings.getString("inputport", "");
		outputport = settings.getString("outputport", "");

		if (!user.equals("") && !pass.equals("") && !domain.equals("")
				&& !server.equals("") && !inputport.equals("")
				&& !outputport.equals(""))
			return true;
		else
			return false;
	}

}