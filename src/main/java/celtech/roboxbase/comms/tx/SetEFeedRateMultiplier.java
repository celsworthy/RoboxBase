package celtech.roboxbase.comms.tx;

import celtech.roboxbase.comms.remote.FixedDecimalFloatFormat;

/**
 *
 * @author ianhudson
 */
public class SetEFeedRateMultiplier extends RoboxTxPacket
{

    /**
     *
     */
    public SetEFeedRateMultiplier()
    {
        super(TxPacketTypeEnum.SET_E_FEED_RATE_MULTIPLIER, false, false);
    }

    /**
     *
     * @param byteData
     * @return
     */
    @Override
    public boolean populatePacket(byte[] byteData)
    {
        setMessagePayloadBytes(byteData);
        return false;
    }

    /**
     *
     * @param feedRateMultiplier
     */
    public void setFeedRateMultiplier(double feedRateMultiplier)
    {
        StringBuilder payload = new StringBuilder();

        FixedDecimalFloatFormat decimalFloatFormatter = new FixedDecimalFloatFormat();

        payload.append(decimalFloatFormatter.format(feedRateMultiplier));

        this.setMessagePayload(payload.toString());
    }
}
