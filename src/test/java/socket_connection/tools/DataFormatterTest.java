package socket_connection.tools;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;


class DataFormatterTest {

    /**
     * This test ensure that boxing and than unboxing a data will produce
     * as result the same data.
     * This test runs all standard charset
     */
    @Test
    void boxAndUnBoxCorrectly(){
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
}
