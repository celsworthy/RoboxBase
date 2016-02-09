package celtech.roboxbase.configuration;

import java.lang.reflect.Method;
import java.util.prefs.Preferences;

public class WindowsRegistry
{

    private static final int KEY_READ = 0x20019;

    public static void currentMachine(String key)
    {

        //Retrieve a reference to the root of the system preferences tree
        final Preferences systemRoot = Preferences.systemRoot();
        final Class clz = systemRoot.getClass();

        try
        {
            Class[] params1 =
            {
                byte[].class, int.class, int.class
            };
            final Method openKey = clz.getDeclaredMethod(
                    "openKey", params1);
            openKey.setAccessible(true);

            Class[] params2 =
            {
                int.class
            };
            final Method closeKey = clz.getDeclaredMethod(
                    "closeKey", params2);
            closeKey.setAccessible(true);

            final Method winRegQueryValue = clz.getDeclaredMethod(
                    "WindowsRegQueryValueEx",
                    int.class,
                    byte[].class);
            winRegQueryValue.setAccessible(true);

//            String key = "SOFTWARE\\Microsoft\\Internet Explorer";
            int hKey = (Integer) openKey.invoke(systemRoot,
                    toByteEncodedString(key),
                    KEY_READ,
                    KEY_READ);
            byte[] valb = (byte[]) winRegQueryValue.invoke(systemRoot, hKey,
                    toByteEncodedString("Version"));
            String vals = (valb != null ? new String(valb).trim() : null);
            System.out.println("Internet Explorer Version = " + vals);
            closeKey.invoke(Preferences.systemRoot(), hKey);

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static String currentUser(String key, String value)
    {
        //Retrieve a reference to the root of the user preferences tree
        final Preferences userRoot = Preferences.userRoot();
        final Class clz = userRoot.getClass();
        String vals = null;

        try
        {
            Class[] params1 =
            {
                byte[].class, int.class, int.class
            };
            final Method openKey = clz.getDeclaredMethod("openKey",
                    params1);
            openKey.setAccessible(true);

            Class[] params2 =
            {
                int.class
            };
            final Method closeKey = clz.getDeclaredMethod("closeKey",
                    params2);
            closeKey.setAccessible(true);

            final Method winRegQueryValue = clz.getDeclaredMethod(
                    "WindowsRegQueryValueEx",
                    int.class,
                    byte[].class);
            winRegQueryValue.setAccessible(true);

            int hKey = (Integer) openKey.invoke(userRoot,
                    toByteEncodedString(key),
                    KEY_READ,
                    KEY_READ);

            byte[] valb = (byte[]) winRegQueryValue.invoke(
                    userRoot,
                    hKey,
                    toByteEncodedString(
                            value));

            vals = (valb != null ? new String(valb).trim() : null);
//            System.out.println("MimeExclusionListForCache = " + vals);
            closeKey.invoke(Preferences.userRoot(), hKey);

        } catch (Exception e)
        {
            e.printStackTrace();
        }

        return vals;
    }

    private static byte[] toByteEncodedString(String str)
    {

        byte[] result = new byte[str.length() + 1];
        for (int i = 0; i < str.length(); i++)
        {
            result[i] = (byte) str.charAt(i);
        }
        result[str.length()] = 0;
        return result;
    }
}
