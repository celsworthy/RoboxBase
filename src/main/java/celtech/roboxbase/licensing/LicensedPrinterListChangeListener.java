package celtech.roboxbase.licensing;

import celtech.roboxbase.printerControl.model.Head;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.printerControl.model.PrinterListChangesListener;
import celtech.roboxbase.printerControl.model.Reel;

/**
 *
 * @author George Salter
 */
public class LicensedPrinterListChangeListener implements PrinterListChangesListener {
    
        @Override
        public void whenPrinterAdded(Printer printer) {
            LicenceManager.getInstance().validateLicence(false);
        }

        @Override
        public void whenPrinterRemoved(Printer printer) {}

        @Override
        public void whenHeadAdded(Printer printer) {}

        @Override
        public void whenHeadRemoved(Printer printer, Head head) {}

        @Override
        public void whenReelAdded(Printer printer, int reelIndex) {}

        @Override
        public void whenReelRemoved(Printer printer, Reel reel, int reelIndex) {}

        @Override
        public void whenReelChanged(Printer printer, Reel reel) {}

        @Override
        public void whenExtruderAdded(Printer printer, int extruderIndex) {}

        @Override
        public void whenExtruderRemoved(Printer printer, int extruderIndex) {}
}
