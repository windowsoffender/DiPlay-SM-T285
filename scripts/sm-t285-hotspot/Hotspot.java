import java.lang.reflect.Method;

/**
 * Turns the Wi-Fi hotspot on or off through the Wi-Fi service, skipping Samsung Settings' SIM check.
 * Run as root: CLASSPATH=hotspot.dex app_process /system/bin Hotspot on|off|status|name <name>|channel [n]|timeout [seconds]|fields|methods
 * scripts/sm-t285-hotspot.sh builds and runs it over adb.
 */
public final class Hotspot {
    private static final String[] STATES = {"disabling", "disabled", "enabling", "enabled", "failed"};

    public static void main(String[] args) throws Exception {
        Object binder = Class.forName("android.os.ServiceManager")
            .getMethod("getService", String.class).invoke(null, "wifi");
        Object wifi = Class.forName("android.net.wifi.IWifiManager$Stub")
            .getMethod("asInterface", Class.forName("android.os.IBinder")).invoke(null, binder);
        Class<?> config = Class.forName("android.net.wifi.WifiConfiguration");
        String command = args.length > 0 ? args[0] : "status";

        if (command.equals("fields")) {
            for (java.lang.reflect.Field f : config.getFields()) {
                String name = f.getName().toLowerCase();
                if (name.contains("channel") || name.contains("band") || name.startsWith("ap")) System.out.println(f);
            }
            return;
        }
        if (command.equals("methods")) {
            for (Method m : wifi.getClass().getMethods()) {
                String name = m.getName().toLowerCase();
                if (name.contains("ap") || name.contains("wifienabled")) System.out.println(m);
            }
            return;
        }
        // Samsung switches the hotspot off after this long without devices; 0 means never.
        if (command.equals("timeout")) {
            if (args.length > 1) {
                wifi.getClass().getMethod("setWifiApTimeOut", int.class).invoke(wifi, Integer.parseInt(args[1]));
            }
            System.out.println("timeout " + wifi.getClass().getMethod("getWifiApTimeOut").invoke(wifi));
            return;
        }
        // Renames the hotspot; the password stays. Change it while the hotspot is off.
        if (command.equals("name") && args.length > 1) {
            Object saved = wifi.getClass().getMethod("getWifiApConfiguration").invoke(wifi);
            config.getField("SSID").set(saved, args[1]);
            wifi.getClass().getMethod("setWifiApConfiguration", config).invoke(wifi, saved);
            return;
        }
        // The hotspot's fixed channel; 0 lets the radio pick one, which nothing can read back.
        if (command.equals("channel")) {
            // AOSP calls it apChannel; Samsung's Android 5 calls it channel.
            java.lang.reflect.Field field;
            try {
                field = config.getField("apChannel");
            } catch (NoSuchFieldException e) {
                field = config.getField("channel");
            }
            Object saved = wifi.getClass().getMethod("getWifiApConfiguration").invoke(wifi);
            if (args.length > 1) {
                field.setInt(saved, Integer.parseInt(args[1]));
                wifi.getClass().getMethod("setWifiApConfiguration", config).invoke(wifi, saved);
                saved = wifi.getClass().getMethod("getWifiApConfiguration").invoke(wifi);
            }
            System.out.println("channel " + field.getInt(saved));
            return;
        }
        if (command.equals("on") || command.equals("off")) {
            boolean on = command.equals("on");
            // The radio can't run the hotspot and a Wi-Fi connection at once.
            if (on && (Integer) wifi.getClass().getMethod("getWifiEnabledState").invoke(wifi) == 3) {
                wifi.getClass().getMethod("setWifiEnabled", boolean.class).invoke(wifi, false);
                Thread.sleep(1500);
            }
            wifi.getClass().getMethod("setWifiApEnabled", config, boolean.class).invoke(wifi, null, on);
            for (int i = 0; i < 20 && state(wifi) != (on ? 13 : 11); i++) Thread.sleep(500);
        }
        System.out.println("hotspot " + STATES[Math.max(0, Math.min(4, state(wifi) - 10))]);
        Object current = wifi.getClass().getMethod("getWifiApConfiguration").invoke(wifi);
        if (current != null) {
            System.out.println("name " + config.getField("SSID").get(current));
            System.out.println("password " + config.getField("preSharedKey").get(current));
        }
    }

    private static int state(Object wifi) throws Exception {
        return (Integer) wifi.getClass().getMethod("getWifiApEnabledState").invoke(wifi);
    }
}
