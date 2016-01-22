/*
 * Copyright 2014 CEL UK
 */
package celtech.roboxbase;

/**
 *
 * @author tony
 */
public enum SystemErrorHandlerOptions
{
    
    ABORT, CLEAR_CONTINUE, RETRY, OK, OK_ABORT, OK_CONTINUE;
    
    public String getErrorTitleKey()
    {
        return "error.handler." + name() + ".title";
    }

    public String getErrorMessageKey()
    {
        return "error.handler." + name() + ".message";
    }
}
