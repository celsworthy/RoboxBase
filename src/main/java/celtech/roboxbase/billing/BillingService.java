package celtech.roboxbase.billing;

import celtech.roboxbase.ApplicationFeature;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.configuration.BaseConfiguration;
import static java.lang.Thread.sleep;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author George Salter
 */
public class BillingService implements Runnable {

    private static final Stenographer STENO = StenographerFactory.getStenographer(BillingService.class.getName());
    
    private boolean keepRunning = true;
    
    @Override
    public void run() {
        
        // First we need to check for some kind of user data?
        // If we have data and it's all valid then no need for log in / sign up
        // Else we need to present a sign in dialogue
        
        // Wait for application to load up before popup
        try {
            sleep(500);
        } catch (InterruptedException ex) {
            STENO.warning("Billing service initial wait period was interrupted");
        }
        
        boolean signInResult = BaseLookup.getSystemNotificationHandler().showSignInDialogue();
        
        while(keepRunning) {
            
        }
    }
    
}
