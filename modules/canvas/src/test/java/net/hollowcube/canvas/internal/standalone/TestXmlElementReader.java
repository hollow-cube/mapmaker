package net.hollowcube.canvas.internal.standalone;

import org.junit.jupiter.api.Test;

public class TestXmlElementReader {

    @Test
    public void workingTest() {
        var component = XmlElementReader.load("/Users/matt/dev/projects/mmo/mapmaker/modules/canvas/src/test/resources/net/hollowcube/canvas/demo/PaginatedList.xml", false);
    }

}
