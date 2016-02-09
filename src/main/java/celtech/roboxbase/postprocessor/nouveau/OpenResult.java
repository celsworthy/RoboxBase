package celtech.roboxbase.postprocessor.nouveau;

/**
 *
 * @author Ian
 */
public class OpenResult
{

    private double outstandingEReplenish;
    private double outstandingDReplenish;
    private final boolean nozzleOpen;
    private final int lastToolNumber;
    private final int opensInLastTool;

    public OpenResult(double outstandingEReplenish,
            double outstandingDReplenish,
            boolean nozzleOpen,
            int lastToolNumber,
            int opensInLastTool)
    {
        this.outstandingEReplenish = outstandingEReplenish;
        this.outstandingDReplenish = outstandingDReplenish;
        this.nozzleOpen = nozzleOpen;
        this.lastToolNumber = lastToolNumber;
        this.opensInLastTool = opensInLastTool;
    }

    public double getOutstandingDReplenish()
    {
        return outstandingDReplenish;
    }

    public void setOutstandingDReplenish(double outstandingDReplenish)
    {
        this.outstandingDReplenish = outstandingDReplenish;
    }

    public double getOutstandingEReplenish()
    {
        return outstandingEReplenish;
    }

    public void setOutstandingEReplenish(double outstandingEReplenish)
    {
        this.outstandingEReplenish = outstandingEReplenish;
    }

    public boolean isNozzleOpen()
    {
        return nozzleOpen;
    }

    public int getLastToolNumber()
    {
        return lastToolNumber;
    }

    public int getOpensInLastTool()
    {
        return opensInLastTool;
    }
}
