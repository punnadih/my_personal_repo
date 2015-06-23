package src.main.java;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by punnadi on 6/1/15.
 */
public class Agent {
    private static Log log = LogFactory.getLog(Agent.class);
    private static Connection connection = null;

    private static void createConnection(String[] args) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.jdbc.Driver");
        connection = DriverManager.getConnection(
                "jdbc:mysql://"+ args[0]+":"+args[1]+"?user="+args[2]+"&password="+args[3]);
        /*connection = DriverManager.getConnection(
                "jdbc:mysql://192.168.16.4:3306?user=root&password=root");*/
    }

    private static void closeConnection() throws SQLException {
        connection.close();
    }

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("help")) {
            System.out.println("Please specify host, port, database username and password space separated manner");
        } else if (args.length == 4) {
            try {
                createConnection(args);
                List<Integer> tenantIds = getTenantIDs();
                List<AppVersion> versionList = new ArrayList<AppVersion>();
                for (Integer tenantId : tenantIds) {
                    List<RegResource> regResource = retrieveRegResources(tenantId);
                    for (RegResource resource : regResource) {
                        String content = retrieveBLOB(resource.getContentId());
                        String stage = getStage(resource.getRegVersion());
                        if(content != null) {
                            AppVersion appVersion = new AppVersion();
                            appVersion.setTenantId(tenantId);
                            appVersion.setStage(stage);
                            parseFile(content, appVersion);
                            if(appVersion.getAppKey() != null) {
                                versionList.add(appVersion);
                            }
                        }
                    }
                }
                List<String> queryList = generateUpdateQueries(versionList);
                generateFile(queryList);
            } catch (ClassNotFoundException e) {
                log.error("Error occurred ", e);
            } catch (SQLException e) {
                log.error("Error occurred ", e);
            } catch (Exception e) {
                log.error("Error occurred ", e);
            } finally {
                try {
                    closeConnection();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("Invalid set of arguments, Please refer help command");
        }
    }

    private static void generateFile(List<String> queryList) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("MigrationUpdateQueries.sql", "UTF-8");
            for (String query : queryList) {
                writer.println(query);
            }
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    private static String getStage(int version) throws Exception {
        String sql1 = "SELECT REG_VALUE FROM dbGovernanceCloud.REG_PROPERTY WHERE REG_ID IN (SELECT REG_PROPERTY_ID FROM" +
                      " dbGovernanceCloud.REG_RESOURCE_PROPERTY WHERE REG_VERSION = ?) AND REG_NAME LIKE 'registry.lifecycle.%.state'";
        ResultSet resultSet = null;
        PreparedStatement preparedStatement = null;
        String stage = null;
        try {
            preparedStatement = connection.prepareStatement(sql1);
            preparedStatement.setInt(1, version);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()){
                stage = resultSet.getString("REG_VALUE");
            }
        } catch (SQLException e) {
            String msg = "Error while retrieving stage from database";
            log.error(msg, e);
            throw new Exception(msg, e);
        } finally {
            resultSet.close();
            preparedStatement.close();
        }
        return stage;
    }

    private static List<String> generateUpdateQueries(List<AppVersion> versionList) {
        List<String> queries = new ArrayList<String>(versionList.size());
        for (AppVersion version : versionList) {
            String updateQuery = "UPDATE AF_APPLICATION APP JOIN AF_VERSION VERSION ON APP.ID=VERSION.APPLICATION_ID " +
                                 "SET VERSION.STAGE='SEVEN', VERSION.AUTO_BUILD=ONE, VERSION.AUTO_DEPLOY=TWO, VERSION.SUBDOMAIN='THREE' " +
                                 "WHERE APP.TENANT_ID=FOUR AND APP.APPLICATION_KEY='FIVE' AND VERSION.VERSION_NAME='SIX';";
            updateQuery = updateQuery.replace("ONE", Integer.toString(version.getAutoBuild()));
            updateQuery = updateQuery.replace("TWO", Integer.toString(version.getAutoDeploy()));
            updateQuery = updateQuery.replace("THREE", version.getSubDomain());
            updateQuery = updateQuery.replace("FOUR", Integer.toString(version.getTenantId()));
            updateQuery = updateQuery.replace("FIVE", version.getAppKey());
            updateQuery = updateQuery.replace("SIX", version.getVersion());
            updateQuery = updateQuery.replace("SEVEN", version.getStage());
            queries.add(updateQuery);

        }
        return queries;
    }

    private static List<Integer> getTenantIDs() throws Exception {
        List<Integer> tenantIds = new ArrayList<Integer>();
        String sql = "SELECT DISTINCT TENANT_ID AS TID FROM afdb.AF_APPLICATION;";
        ResultSet resultSet = null;
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("TID");
                tenantIds.add(id);
            }
        } catch (SQLException e) {
            log.error("Error occurred while retrieving tenant ids", e);
            throw new Exception("Error occurred while retrieving tenant ids", e);
        } finally {
            resultSet.close();
            preparedStatement.close();
        }
        return tenantIds;
    }

    private static void parseFile(String content, AppVersion appVersionDTO) throws Exception {
        try {
            OMElement root = AXIOMUtil.stringToOM(content);
            OMElement appVersion = root.getFirstChildWithName(
                    new QName("http://www.wso2.org/governance/metadata", "appversion"));
            if(appVersion != null) {
                OMElement appKey = appVersion.getFirstChildWithName(
                        new QName("http://www.wso2.org/governance/metadata", "key"));
                OMElement version = appVersion.getFirstChildWithName(
                        new QName("http://www.wso2.org/governance/metadata", "version"));
                OMElement autoBuild = appVersion.getFirstChildWithName(
                        new QName("http://www.wso2.org/governance/metadata", "isAutoBuild"));
                OMElement autoDeploy = appVersion.getFirstChildWithName(
                        new QName("http://www.wso2.org/governance/metadata", "isAutoDeploy"));
                OMElement subDomain = appVersion.getFirstChildWithName(
                        new QName("http://www.wso2.org/governance/metadata", "prodmappedsubdomain"));
                appVersionDTO.setAutoBuild(autoBuild.getText().equals("true") ? 1 : 0);
                appVersionDTO.setAutoDeploy(autoDeploy.getText().equals("true") ? 1 : 0);
                appVersionDTO.setSubDomain(subDomain == null ? "null" : subDomain.getText());
                appVersionDTO.setVersion(version.getText());
                appVersionDTO.setAppKey(appKey.getText());

            }else{
                log.error("Invalid RXT type " + content);
            }
        } catch (XMLStreamException e) {
            log.error("Error occurred while parsing rxt info", e);
            throw new Exception("Error occurred while parsing rxt info", e);
        }
    }

    public static List<RegResource> retrieveRegResources(int tenantId) throws Exception {
        List<RegResource> resources = new ArrayList<RegResource>();
        String sql =
                "SELECT REG_NAME,REG_CONTENT_ID, REG_VERSION FROM dbGovernanceCloud.REG_RESOURCE where " +
                "REG_MEDIA_TYPE=\"application/vnd.wso2-appversion+xml\" and REG_TENANT_ID=" + tenantId;
        ResultSet resultSet = null;
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                RegResource resource = new RegResource();
                int contentId = resultSet.getInt("REG_CONTENT_ID");
                String version = resultSet.getString("REG_NAME");
                int regVersion = resultSet.getInt("REG_VERSION");
                resource.setContentId(contentId);
                resource.setVersion(version);
                resource.setRegVersion(regVersion);
                resources.add(resource);
            }


        } catch (SQLException e) {
            log.error("Error occurred while retrieving reg resource info", e);
            throw new Exception("Error occurred while retrieving reg resource info", e);
        } finally {
            resultSet.close();
            preparedStatement.close();
        }
        return resources;
    }

    public static String retrieveBLOB(int contentId) throws Exception {
        String sql =
                "SELECT CAST(REG_CONTENT_DATA AS CHAR(10000) CHARACTER SET utf8) AS CONTENT FROM" +
                " dbGovernanceCloud.REG_CONTENT where REG_CONTENT_ID=" + contentId;
        ResultSet resultSet = null;
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("CONTENT");
            }
        } catch (SQLException e) {
            log.error("Error occurred while retrieving BLOB content", e);
            throw new Exception("Error occurred while retrieving BLOB content", e);
        } finally {
            resultSet.close();
            preparedStatement.close();
        }
        return null;
    }

}
