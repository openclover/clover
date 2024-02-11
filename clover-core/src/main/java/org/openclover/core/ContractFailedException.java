package org.openclover.core;


public class ContractFailedException
    extends RuntimeException
{

    public ContractFailedException(String msg)
    {
        super(msg);
    }
}
