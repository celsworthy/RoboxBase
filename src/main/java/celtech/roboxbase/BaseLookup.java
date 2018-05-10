package celtech.roboxbase;

import celtech.roboxbase.appManager.ConsoleSystemNotificationManager;
import celtech.roboxbase.appManager.SystemNotificationManager;
import celtech.roboxbase.configuration.datafileaccessors.FilamentContainer;
import celtech.roboxbase.configuration.datafileaccessors.SlicerMappingsContainer;
import celtech.roboxbase.configuration.fileRepresentation.SlicerMappings;
import celtech.roboxbase.i18n.languagedata.LanguageData;
import celtech.roboxbase.postprocessor.GCodeOutputWriter;
import celtech.roboxbase.postprocessor.GCodeOutputWriterFactory;
import celtech.roboxbase.postprocessor.LiveGCodeOutputWriter;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.printerControl.model.PrinterListChangesNotifier;
import celtech.roboxbase.utils.tasks.LiveTaskExecutor;
import celtech.roboxbase.utils.tasks.TaskExecutor;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import libertysystems.stenographer.LogLevel;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class BaseLookup
{

    private static final Stenographer steno = StenographerFactory.getStenographer(
            BaseLookup.class.getName());

    private static ResourceBundle i18nbundle;
    private static Locale applicationLocale;
    private static TaskExecutor taskExecutor;
    private static SlicerMappings slicerMappings;
    private static GCodeOutputWriterFactory<GCodeOutputWriter> postProcessorGCodeOutputWriterFactory;
    private static SystemNotificationManager systemNotificationHandler;
    private static boolean shuttingDown = false;

    private static PrinterListChangesNotifier printerListChangesNotifier;
    private static final ObservableList<Printer> connectedPrinters = FXCollections.observableArrayList();
    private static final ObservableList<Printer> connectedPrintersUnmodifiable = FXCollections.unmodifiableObservableList(connectedPrinters);

    private static Set<Locale> availableLocales = null;

    /**
     * The database of known filaments.
     */
    private static FilamentContainer filamentContainer;

    public static ResourceBundle getLanguageBundle()
    {
        return i18nbundle;
    }

    public static String i18n(String stringId)
    {
        String langString = null;
        try
        {
           langString = i18nbundle.getString(stringId);
        }
        catch (MissingResourceException ex)
        {
            langString = stringId;
        }
        langString = substituteTemplates(langString);
        return langString;
    }

    /**
     * Strings containing templates (eg *T14) should be substituted with the
     * correct text.
     *
     * @param langString
     * @return
     */
    public static String substituteTemplates(String langString)
    {
        String patternString = ".*\\*T(\\d\\d).*";
        Pattern pattern = Pattern.compile(patternString);
        while (true)
        {
            Matcher matcher = pattern.matcher(langString);
            if (matcher.find())
            {
                String template = "*T" + matcher.group(1);
                String templatePattern = "\\*T" + matcher.group(1);
                langString = langString.replaceAll(templatePattern, i18n(template));
            } else
            {
                break;
            }
        }
        return langString;
    }

    public static TaskExecutor getTaskExecutor()
    {
        return taskExecutor;
    }

    public static void setTaskExecutor(TaskExecutor taskExecutor)
    {
        BaseLookup.taskExecutor = taskExecutor;
    }

    public static void setSlicerMappings(SlicerMappings slicerMappings)
    {
        BaseLookup.slicerMappings = slicerMappings;
    }

    public static SlicerMappings getSlicerMappings()
    {
        return slicerMappings;
    }

    public static GCodeOutputWriterFactory getPostProcessorOutputWriterFactory()
    {
        return postProcessorGCodeOutputWriterFactory;
    }

    public static void setPostProcessorOutputWriterFactory(
            GCodeOutputWriterFactory<GCodeOutputWriter> factory)
    {
        postProcessorGCodeOutputWriterFactory = factory;
    }

    public static SystemNotificationManager getSystemNotificationHandler()
    {
        return systemNotificationHandler;
    }

    public static void setSystemNotificationHandler(
            SystemNotificationManager systemNotificationHandler)
    {
        BaseLookup.systemNotificationHandler = systemNotificationHandler;
    }

    public static boolean isShuttingDown()
    {
        return shuttingDown;
    }

    public static void setShuttingDown(boolean shuttingDown)
    {
        BaseLookup.shuttingDown = shuttingDown;
    }

    public static PrinterListChangesNotifier getPrinterListChangesNotifier()
    {
        return printerListChangesNotifier;
    }

    public static void printerConnected(Printer printer)
    {
        BaseLookup.getTaskExecutor().runOnGUIThread(() ->
        {
            steno.debug(">>>Printer connection notification - " + printer);
            doPrinterConnect(printer);
        });
    }

    private static synchronized void doPrinterConnect(Printer printer)
    {
        connectedPrinters.add(printer);
    }

    public static void printerDisconnected(Printer printer)
    {
        BaseLookup.getTaskExecutor().runOnGUIThread(() ->
        {
            steno.debug("<<<Printer disconnection notification - " + printer);
            doPrinterDisconnect(printer);
        });
    }

    private static synchronized void doPrinterDisconnect(Printer printer)
    {
        connectedPrinters.remove(printer);
    }

    public static ObservableList<Printer> getConnectedPrinters()
    {
        return connectedPrintersUnmodifiable;
    }

    public static void setupDefaultValues()
    {
        setupDefaultValues(LogLevel.INFO, Locale.ENGLISH, new ConsoleSystemNotificationManager());
    }

    public static Set<Locale> getAvailableLocales()
    {
        return availableLocales;
    }

    public static Locale getApplicationLocale()
    {
        return applicationLocale;
    }

    public static void setApplicationLocale(Locale locale)
    {
        applicationLocale = locale;
    }

    public static void setupDefaultValues(LogLevel logLevel, Locale appLocale, SystemNotificationManager notificationManager)
    {
        StenographerFactory.changeAllLogLevels(logLevel);

        steno.debug("Starting AutoMaker - loading resources...");

        applicationLocale = appLocale;
        
        LanguageData languageData = new LanguageData();
        availableLocales = languageData.getAvailableLocales();

        i18nbundle = ResourceBundle.getBundle("celtech.roboxbase.i18n.languagedata.LanguageData", applicationLocale);

        BaseLookup.setTaskExecutor(
                new LiveTaskExecutor());
        steno.info("Using locale - " + appLocale.toLanguageTag());

        printerListChangesNotifier = new PrinterListChangesNotifier(BaseLookup.getConnectedPrinters());

        setSystemNotificationHandler(notificationManager);

        setSlicerMappings(SlicerMappingsContainer.getSlicerMappings());

        setPostProcessorOutputWriterFactory(LiveGCodeOutputWriter::new);
    }
}
