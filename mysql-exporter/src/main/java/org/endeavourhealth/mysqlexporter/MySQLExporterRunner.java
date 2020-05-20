package org.endeavourhealth.mysqlexporter;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

public class MySQLExporterRunner {

    public static void main(String... args) throws IOException, SQLException {
        String params="";

        Properties properties = loadProperties( args );

        // params is a list of filtered* table id's
        for (String s: args) {
            System.out.println(s);

            String[] ss = s.split("\\:");

            params=params+s+"~";
        }

        properties.setProperty("params",params);

        try (  MySQLExporter mysqlExporter = new MySQLExporter( properties  ) ) {

            mysqlExporter.export();

        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    private static Properties loadProperties(String[] args) throws IOException {

        Properties properties = new Properties();

        InputStream inputStream = MySQLExporterRunner.class.getClassLoader().getResourceAsStream("mysql.exporter.properties");

        properties.load( inputStream );

        return properties;
    }

}