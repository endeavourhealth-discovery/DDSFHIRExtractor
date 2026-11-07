package org.endeavourhealth.certchecker;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

public class CertCheckerRunner {

    public static void main(String... args) throws IOException, SQLException {
        String params="";

        Properties properties = loadProperties( args );

        try (  CertChecker certChecker = new CertChecker( properties  ) ) {

            certChecker.export();

        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    private static Properties loadProperties(String[] args) throws IOException {

        Properties properties = new Properties();

        InputStream inputStream = CertCheckerRunner.class.getClassLoader().getResourceAsStream("cert.checker.properties");

        properties.load( inputStream );

        return properties;
    }

}