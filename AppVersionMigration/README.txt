How to create the Executable jar
===============================

├── AppVersionMigration.MF
└── src
    ├── lib
    │   ├── axiom_1.2.11.wso2v4.jar
    │   ├── commons-logging-1.2.jar
    │   └── mysql-connector-java-5.1.27-bin.jar
    └── main
        └── java
            ├── Agent.java
            ├── AppVersion.java
            └── RegResource.java

Make sure you have the package name defined in all the class files.

Add AppVersionMigration.MF manually to Project root.

Manifest-Version: 1.0
Main-Class: src.main.java.Agent
Class-Path: src/lib/axiom_1.2.11.wso2v4.jar src/lib/commons-logging-1.2.jar src/lib/mysql-connector-java-5.1.27-bin.jar




Commands:
javac -cp .:src/lib/* src/main/java/*.java
jar cmf AppVersionMigration.MF AppVersionMigration.jar src/main/java/*.class

How to run the jar
==================
Copy the following artifacts as shown below to a location:
├── AppVersionMigration.jar
└── src
    ├── lib
    │   ├── axiom_1.2.11.wso2v4.jar
    │   ├── commons-logging-1.2.jar
    │   └── mysql-connector-java-5.1.27-bin.jar
The execute the below command.
java -jar AppVersionMigration.jar <mysql_host_name> <mysql_port> <mysql_username> <mysql_password>
e.g: java -jar AppVersionMigration.jar localhost 3306 root root
