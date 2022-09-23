package com.atlassian.clover.remote;

import com.atlassian.clover.Logger;
import com.atlassian.clover.CloverNames;

import java.io.Serializable;

/**
 * Configuration of the Distributed Coverage feature - server host and port, connection timeout etc.
 */
public class DistributedConfig implements Config, Serializable {
    private static final long serialVersionUID = 1L;

    public static final String OFF = "OFF" ;
    public static final String ON = "ON" ;

    private final InitStringData data;
    public static final String NAME = "name";
    public static final String PORT = "port";
    public static final String HOST = "host";
    public static final String TIMEOUT = "timeout";
    public static final String NUM_CLIENTS = "numClients";
    public static final String RETRY_PERIOD = "retryPeriod";

    public static DistributedConfig ON() {
        return new DistributedConfig(ON);
    }

    public static DistributedConfig OFF() {
        return new DistributedConfig(OFF);
    }
    /**
     * Parses a String of the form: "ON|OFF|name=name;host=host;port=port;timeout=timeout;numclients=2;".
     * <p/>
     * If str is "ON" or "on", then this config be enabled with default values for all properties.
     * If str is "OFF" or "off", then this config will be disabled, and no values should be queuried.
     * 
     * @param str the configuration data to initialize this Config from.
     */
    public DistributedConfig(String str) {
        Logger.getInstance().verbose("DistributedConfig: data = " + str);

        if (OFF.equalsIgnoreCase(str)) {
            data = null;
        } else if (ON.equalsIgnoreCase(str)) {
            data =  new InitStringData("");
        } else {
            data = new InitStringData(str);
        }
    }

    /**
     * Creates a distributed config using all default values.
     */
    public DistributedConfig() {
        this("");
    }

    @Override
    public boolean isEnabled() {
        return data != null;
    }

    @Override
    public String getName() {
        return data.get(NAME, CloverNames.CAJO_TCP_SERVER_NAME);
    }

    public void setName(String value) {
        data.set(NAME, value);
    }

    public int getPort() {
        return data.get(PORT, 1198);
    }

    public void setPort(int value) {
        data.set(PORT, value);
    }

    public String getHost() {
        return data.get(HOST, "localhost");
    }

    public void setHost(String value) {
        data.set(HOST, value);
    }

    public int getTimeout() {
        return data.get(TIMEOUT, 5000);
    }

    public void setTimeout(int value) {
        data.set(TIMEOUT, value);
    }

    public int getNumClients() {
        return data.get(NUM_CLIENTS, 0);
    }

    public void setNumClients(int value) {
        data.set(NUM_CLIENTS, value);
    }

    public int getRetryPeriod() {
        return data.get(RETRY_PERIOD, 1000);
    }

    public void setRetryPeriod(int value) {
        data.set(RETRY_PERIOD, value);
    }

    public String getServerLocation() {
        return getHost() + SEP + getPort();
    }

    public String toString() {
        if (!isEnabled()) {
            return null;
        }
        return String.format("%s=%s;%s=%s;%s=%d;%s=%d;%s=%d;%s=%d",
                NAME, getName(),
                HOST, getHost(),
                PORT, getPort(),
                TIMEOUT, getTimeout(),
                NUM_CLIENTS, getNumClients(),
                RETRY_PERIOD, getRetryPeriod());
    }
    
    //For use in Maven where it uses a trivial subclass (for Maven mojo configuration/instrumentaiton purposes)
    //but that class can't be deserialised when loading the serialised config from Grover
    public String getConfigString() {
        if (data == null) {
            return OFF;
        } else if (data.toString().length() == 0) {
            return ON;
        } else {
            return data.toString();
        }
    }
}
