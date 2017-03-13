package com.atlassian.clover;


public class ContractFailedException
    extends RuntimeException
{

    public ContractFailedException(String msg)
    {
        super(msg);
    }
}
