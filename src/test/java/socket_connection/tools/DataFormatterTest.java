package socket_connection.tools;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


class DataFormatterTest {

    /**
     * This test ensure that boxing and than unboxing a data will produce
     * as result the same data.
     * This test runs all standard charset
     */
    @Test
    void boxAndUnBoxCorrectlyWithAllStandardCharsets() throws NoSuchAlgorithmException {
        String casualData= "data";
        ArrayList<Charset> standardCharset= new ArrayList<>();
        //adding all standard charset
        standardCharset.add(StandardCharsets.UTF_8);
        standardCharset.add(StandardCharsets.UTF_16);
        standardCharset.add(StandardCharsets.UTF_16LE);
        standardCharset.add(StandardCharsets.UTF_16BE);
        standardCharset.add(StandardCharsets.US_ASCII);
        standardCharset.add(StandardCharsets.ISO_8859_1);
        //check that casual data can be encoded by every standard charset
        standardCharset.forEach(charset -> charset.newEncoder().canEncode(casualData));
        //check boxing - unboxing operation
        ArrayList<DataFormatter> formatterList=new ArrayList<>();
        standardCharset.forEach(charset -> formatterList.add(new DataFormatter(charset)));
        formatterList.forEach(formatter->
                assertEquals(casualData,formatter.unBox(formatter.box(casualData))));

    }


    /**
     * This test ensure that boxing and than unboxing a data will produce
     * as result the same data also when encryption is enabled
     */
    @Test
    void boxAndUnBoxCorrectlyWithEncryption() throws NoSuchAlgorithmException {
        //SETUP TEST
        String casualData= "data";
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        Key pub = kp.getPublic();
        Key pvt = kp.getPrivate();
        //check that casual data can be encoded by the selected charset
        StandardCharsets.UTF_8.newEncoder().canEncode(casualData);
        //check boxing - unboxing operation
        DataFormatter trialFormatter =new DataFormatter(StandardCharsets.UTF_8);
        //SETUP ENCRYPTION
        trialFormatter.setUpEncryption(pvt,pub);
        //CHECK ENCRYPTION
        //check that a formatter can't decrypt the message if it was not initialized with the private key
        assertNotEquals(casualData,new DataFormatter(StandardCharsets.UTF_8).unBox(trialFormatter.box(casualData)));
        //check that a formatter can't decrypt the message if it was not initialized with the private key
        assertEquals(casualData,trialFormatter.unBox(trialFormatter.box(casualData)));
    }
}
