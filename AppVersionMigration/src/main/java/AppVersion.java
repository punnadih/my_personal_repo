package src.main.java;

/**
 * Created by punnadi on 6/12/15.
 */
public class AppVersion {

    private int autoBuild;
    private int autoDeploy;
    private String subDomain;
    private String appKey;
    private int tenantId;
    private String version;
    private String stage;

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getSubDomain() {
        return subDomain;
    }

    public void setSubDomain(String subDomain) {
        this.subDomain = subDomain;
    }

    public int getAutoBuild() {
        return autoBuild;
    }

    public void setAutoBuild(int autoBuild) {
        this.autoBuild = autoBuild;
    }

    public int getAutoDeploy() {
        return autoDeploy;
    }

    public void setAutoDeploy(int autoDeploy) {
        this.autoDeploy = autoDeploy;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String toString() {
        return "tenantId " + tenantId + " appkey "+ appKey + " version " + version + " autoBuild " +autoBuild + " autoDeploy " + autoDeploy;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
