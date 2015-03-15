package uci.ucintlm.service.service;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

/**
 * Created by akiel on 2/18/15.
 * This class provides service util functions
 */
public final class ServiceUtils {
    /*
    * this function returns the state of the service
    * */
    public static boolean isMyServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager
                .getRunningServices(Integer.MAX_VALUE)) {
            if (NTLMProxyService.class.getName().equals(
                    service.service.getClassName())) {
                Log.i(ServiceUtils.class.getName(), "Service running");
                return true;
            }
        }
        Log.i(ServiceUtils.class.getName(), "Service not running");
        return false;
    }
}
