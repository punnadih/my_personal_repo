package org.wso2.carbon.appfactory;

/**
 * Created by punnadi on 9/17/15.
 */
public class Domain {
    private String domainName;
    private String applicationContext;
    private String newDomainName;

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getApplicationContext() {
        return applicationContext;
    }

    public void setApplicationContext(String applicationContext) {
        this.applicationContext = applicationContext;
    }

    public String getNewDomainName() {
        return newDomainName;
    }

    public void setNewDomainName(String newDomainName) {
        this.newDomainName = newDomainName;
    }

    @Override
    public String toString() {
        return "Old Domain: " + domainName + " New Domain: " + newDomainName;
    }
}
