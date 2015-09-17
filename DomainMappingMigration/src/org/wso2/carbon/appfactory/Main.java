package org.wso2.carbon.appfactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.EOFException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class Main {

    private static final Log log = LogFactory.getLog(Main.class);
    private static final PropertyLoader prop = PropertyLoader.getInstance();

    private static Connection connection = null;

    private static void createConnection(String[] args, String datasource) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.jdbc.Driver");
        connection = DriverManager.getConnection(datasource);
    }

    private static void closeConnection() throws SQLException {
        connection.close();
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("javax.net.ssl.keyStore", prop.getProperty("trust_store_path"));
        System.setProperty("javax.net.ssl.keyStorePassword", prop.getProperty("trust_store_password"));
        System.setProperty("javax.net.ssl.trustStore", prop.getProperty("trust_store_path"));
        System.setProperty("javax.net.ssl.trustStorePassword", prop.getProperty("trust_store_password"));

        List<Tenant> tenants = null;
        try {
            createConnection(args, prop.getProperty("produserstoreds"));

        } catch (ClassNotFoundException e) {
            final String msg = "Error occurred while creating db connection";
            log.error(msg, e);
            throw new Exception(msg, e);
        } catch (SQLException e) {
            final String msg = "Error occurred while creating db connection";
            log.error(msg, e);
            throw new Exception(msg, e);
        } finally {
            try {
                closeConnection();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        try {
            tenants = getTenantIDs();
        } catch (Exception e) {
            final String msg = "Error occurred while retrieving tenants";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
        for (Tenant tenant : tenants) {
            List<Domain> newDomains = new ArrayList<Domain>();
            Domains domains = retrieveSubscriptionDomains(tenant);
            if (domains != null) {
                for (Domain domain : domains.getDomains()) {
                    final String oldDomainName = domain.getDomainName();
                    final String defaultDomain = prop.getProperty("defaultDomain");
                    boolean hasDefaultDomain = oldDomainName.endsWith(defaultDomain);
                    if (hasDefaultDomain) {
                        String subDomain = oldDomainName.split("." + defaultDomain)[0];
                        if (!subDomain.equals(defaultDomain) && StringUtils.countMatches(subDomain, ".") == 2 &&
                            subDomain.contains(tenant.getTenantDomain())) {
                            String newDomain =
                                    subDomain.split("\\.")[0] + "-" + tenant.getTenantDomain().replace(".", "dot") +
                                    "." + defaultDomain;
                            domain.setNewDomainName(newDomain);
                            newDomains.add(domain);
                        }
                    }
                }

                for (Domain domain : newDomains) {
                    updateSubscriptionDomain(tenant, domain);
                }
            }
        }

    }

    private static void updateSubscriptionDomain(Tenant tenant, Domain domain) throws Exception {
        System.out.println(tenant + " " + domain);
        final String tenantAdmin = tenant.getAdmin() + "@" +
                                   tenant.getTenantDomain();
        String path = "cartridge/" + prop.getProperty("cartridgeType") + "/subscription/"
                      + getSubscriptionAlias(tenant) + "/domains/" + domain.getDomainName();
        ServerResponse serverResponse = null;
        try {
            serverResponse = StratosHttpClient.sendDeleteRequest(getEndPointUrl(path),
                                                                 tenantAdmin);
            System.out.println(serverResponse.getStatusCode() + " " + serverResponse.getResponse());
        } catch (Exception e) {
            final String msg =
                    "Error occurred while deleting old domain " + domain.getDomainName() + " for tenant " + tenant;
            log.error(msg, e);
            throw new Exception(msg, e);
        }
        if (serverResponse.getStatusCode() == 200) {

            path = "cartridge/" + prop.getProperty("cartridgeType") + "/subscription/"
                   + getSubscriptionAlias(tenant) + "/domains/";
            try {
                serverResponse = StratosHttpClient.sendPostRequest(generateAddSubscriptionBody(domain),
                                                                   getEndPointUrl(path), tenantAdmin);
                System.out.println(serverResponse.getStatusCode() + " " + serverResponse.getResponse());
            } catch (Exception e) {
                final String msg =
                        "Error occurred while adding new domain " + domain.getNewDomainName() + " for tenant " + tenant;
                log.error(msg, e);
                throw new Exception(msg, e);
            }
        } else {
            final String msg =
                    "Error occurred while deleting old domain " + domain.getDomainName() + " for tenant " + tenant +
                    " Response code: " + serverResponse.getStatusCode() + " Message: " + serverResponse.getResponse();
            log.error(msg);
            throw new Exception(msg);
        }

    }

    public static String generateAddSubscriptionBody(Domain domainObj) throws Exception {
        JSONObject domain = new JSONObject();
        JSONObject domains = new JSONObject();
        try {
            domain.put("domainName", domainObj.getNewDomainName());
            domain.put("applicationContext", domainObj.getApplicationContext());
            domains.put("domains", domain);
        } catch (JSONException e) {
            String errorMsg = "Error while generating json string for add subscription";
            log.error(errorMsg, e);
            throw new Exception(errorMsg);
        }
        return domains.toString();
    }

    private static Domains retrieveSubscriptionDomains(Tenant tenant) throws Exception {
        String path = "cartridge/" + prop.getProperty("cartridgeType") + "/subscription/"
                      + getSubscriptionAlias(tenant) + "/domains";
        String response = null;
        try {
            ServerResponse serverResponse = StratosHttpClient.sendGetRequest(getEndPointUrl(path),
                                                                             tenant.getAdmin() + "@" +
                                                                             tenant.getTenantDomain());
            response = serverResponse.getResponse();
        } catch (Exception e) {
            final String msg = "Error occurred while retrieving subscription domains ";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
        ObjectMapper mapper = new ObjectMapper();
        Domains domains = null;
        try {
            domains = mapper.readValue(response, Domains.class);
        } catch (IOException e) {
            final String msg = "Error occurred while generating json ";
            log.error(msg, e);
            //throw new Exception(msg, e);
        }
        return domains;
    }

    private static String getSubscriptionAlias(Tenant tenant) {
        return prop.getProperty("CartridgeAlias") + tenant.getTenantDomain().replace(".", "dot");
    }

    private static String getEndPointUrl(String uri) {
        return prop.getProperty("stratos_url") + "/stratos/admin/" + uri;
    }

    private static List<Tenant> getTenantIDs() throws Exception {
        List<Tenant> tenants = new ArrayList<Tenant>();
        String sql = "SELECT UM_ID, UM_DOMAIN_NAME, UM_EMAIL, CAST(UM_USER_CONFIG AS CHAR(10000) CHARACTER SET utf8) AS CONTENT FROM UM_TENANT";
        ResultSet resultSet = null;
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("UM_ID");
                String tenantDomain = resultSet.getString("UM_DOMAIN_NAME");
                String email = resultSet.getString("UM_EMAIL");
                String content = resultSet.getString("CONTENT");
                String tenantAdmin = (content.split("</UserName>")[0]).split("<UserName>")[1];
                tenants.add(new Tenant(id, tenantDomain, email, tenantAdmin));
            }
        } catch (SQLException e) {
            log.error("Error occurred while retrieving tenant information", e);
            throw new Exception("Error occurred while retrieving tenant ids", e);
        } finally {
            resultSet.close();
            preparedStatement.close();
        }
        return tenants;
    }
}
