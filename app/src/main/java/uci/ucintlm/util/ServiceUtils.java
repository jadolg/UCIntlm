package uci.ucintlm.util;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import uci.ucintlm.service.NTLMProxyService;

/**
 * Created by akiel on 2/18/15.
 */
public final class ServiceUtils {
    public static boolean isMyServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager
                .getRunningServices(Integer.MAX_VALUE)) {
            if (NTLMProxyService.class.getName().equals(
                    service.service.getClassName())) {
                Log.i(ServiceUtils.class.getName(),"Service running");
                return true;
            }
        }
        Log.i(ServiceUtils.class.getName(),"Service not running");
        return false;
    }
}
