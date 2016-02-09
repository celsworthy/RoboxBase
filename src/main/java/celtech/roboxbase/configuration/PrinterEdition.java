/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.roboxbase.configuration;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public enum PrinterEdition
{

    /**
     *
     */
    Kickstarter("KS", "Kickstarter Edition"),
    /**
     *
     */
    TradeSample(
        "TS", "Trade Sample"),
    /**
     *
     */
    FirstEditionUK(
        "BK", "First Edition UK"),
    /**
     *
     */
    FirstEditionEurope(
        "BE", "First Edition Europe"),
    /**
     *
     */
    FirstEditionUSA(
        "BU", "First Edition USA"),
    /**
     *
     */
    FirstEditionSouthAsia_Australia(
        "BA", "First Edition South Asia / Australia"),
    /**
     *
     */
    FirstEditionJapan(
        "BN", "First Edition Japan"),
    /**
     *
     */
    FirstEditionChina(
        "BC", "First Edition China"),
    /**
     *
     */
    FirstEditionSwitzerland(
        "BV", "First Edition Switzerland"),
    /**
     *
     */
    FirstEditionWorldwide(
        "BW", "First Edition Worldwide"),
    /**
     *
     */
    FirstEditionKorea(
        "BR", "First Edition Korea"),
    /**
     *
     */
    FirstEditionIsrael(
        "BI", "First Edition Israel");

    private String codeName;
    private String friendlyName;

    private PrinterEdition(String codeName, String friendlyName)
    {
        this.codeName = codeName;
        this.friendlyName = friendlyName;
    }

    /**
     *
     * @return
     */
    public String getCodeName()
    {
        return codeName;
    }

    /**
     *
     * @return
     */
    public String getFriendlyName()
    {
        return friendlyName;
    }

//    public static PrinterModel getPrintHeadForType(final String headTypeCode)
//    {
//        PrinterModel printHead = null;
//        //interchangeably looks for RBX-ABS-GR499 or RBX_ABS_GR499
//        String headTypeCodeToCheck = headTypeCode.trim();
//        try
//        {
//            printHead = PrinterModel.valueOf(headTypeCodeToCheck);
//        } catch (IllegalArgumentException ex)
//        {
//            try
//            {
//                printHead = PrinterModel.valueOf(headTypeCodeToCheck.replaceAll("-", "_"));
//            } catch (IllegalArgumentException ex1)
//            {
//                printHead = PrinterModel.UNRECOGNISED;
//            }
//        }
//
//        return printHead;
//    }

    @Override
    public String toString()
    {
        return getCodeName() + " - " + getFriendlyName();
    }
}
