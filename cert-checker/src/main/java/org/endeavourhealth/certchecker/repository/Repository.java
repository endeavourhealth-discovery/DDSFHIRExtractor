package org.endeavourhealth.certchecker.repository;

import org.endeavourhealth.common.config.ConfigManager;

import java.util.*;

public class Repository {
    public String baseURL;
    public String clientid;
    public String clientsecret; public String scope; public String granttype;
    public String tokenurl; public String token;
    public String config;

    public Repository(Properties properties) throws Exception {
        init( properties );
    }

    private void init(Properties props) throws Exception {
        config = props.getProperty("config");
        String conStr = getConfig();
        String[] ss = conStr.split("\\`");
        baseURL = ss[7];
        clientid = ss[3];
        clientsecret = ss[4];
        scope = ss[5];
        granttype = props.getProperty("granttype");
        tokenurl = ss[6];
    }

    public void close() throws Exception {
    }

    public String getConfig()
    {
        //String conStr = ConfigManager.getConfiguration("database","knowdiabetes");
        String conStr = ConfigManager.getConfiguration("database",config);
        return conStr;
    }
}