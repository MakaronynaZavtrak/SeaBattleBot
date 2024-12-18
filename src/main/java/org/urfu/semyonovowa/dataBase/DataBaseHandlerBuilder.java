package org.urfu.semyonovowa.dataBase;

import java.util.Properties;

public class DataBaseHandlerBuilder
{
    private String forName;
    private String url;
    private Properties properties;
    public DataBaseHandlerBuilder forName(String forName)
    {
        this.forName = forName;
        return this;
    }
    public DataBaseHandlerBuilder url(String url)
    {
        this.url = url;
        return this;
    }
    public DataBaseHandlerBuilder properties(Properties properties)
    {
        this.properties = properties;
        return this;
    }
    public DataBaseHandler build() { return new DataBaseHandler(forName, url, properties);}
}
