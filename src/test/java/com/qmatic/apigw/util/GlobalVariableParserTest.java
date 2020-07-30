package com.qmatic.apigw.util;

import com.qmatic.apigw.rest.VisitStatusMap;
import org.springframework.http.ResponseEntity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

@Test
public class GlobalVariableParserTest {

    /*
     * Data format definition:
     * object separator: @
     * field separator: |
     *
     * order of fields:
     * Ticket number
     * VisitId
     * CheckSum
     * Position
     * QueueSize
     * Status
     * Service
     * FirstName
     * LastName
     * SpId
     * SpName
     */

    private ResponseEntity<String> responseEntity;
    private String variables = "B032|275|516947812|32|46|IN_QUEUE|VR||||@G001|160|442016551|null||CALLED|DL|Super|Administrator|22|Window 22";

    @BeforeClass
    public void setUp() {
        responseEntity = mock(ResponseEntity.class);
        doReturn(variables).when(responseEntity).getBody();
    }

    public void multipleTicketsAreParsed() {

        VisitStatusMap result = GlobalVariableParser.parseToVisitStatusMap(responseEntity);

        assertEquals(result.size(), 2);
    }

    public void ticketNumberIsParsed() {

        VisitStatusMap result = GlobalVariableParser.parseToVisitStatusMap(responseEntity);

        assertEquals(result.get(275L).getTicketId(), "B032");
        assertEquals(result.get(160L).getTicketId(), "G001");
    }

    public void checksumIsParsed() {

        VisitStatusMap result = GlobalVariableParser.parseToVisitStatusMap(responseEntity);

        assertEquals(result.get(275L).getChecksum(), "516947812");
        assertEquals(result.get(160L).getChecksum(), "442016551");
    }

    public void queuePositionIsParsed() {

        VisitStatusMap result = GlobalVariableParser.parseToVisitStatusMap(responseEntity);

        assertEquals(result.get(275L).getPosition(), new Integer(32));
        assertNull(result.get(160L).getPosition());
    }

    public void queueSizeIsParsed() {

        VisitStatusMap result = GlobalVariableParser.parseToVisitStatusMap(responseEntity);

        assertEquals(result.get(275L).getQueueSize(), new Integer(46));
        assertNull(result.get(160L).getQueueSize());
    }

    public void ticketStatusIsParsed() {

        VisitStatusMap result = GlobalVariableParser.parseToVisitStatusMap(responseEntity);

        assertEquals(result.get(275L).getCurrentStatus(), "IN_QUEUE");
        assertEquals(result.get(160L).getCurrentStatus(), "CALLED");
    }

    public void serviceNameIsParsed() {

        VisitStatusMap result = GlobalVariableParser.parseToVisitStatusMap(responseEntity);

        assertEquals(result.get(275L).getCurrentServiceName(), "VR");
        assertEquals(result.get(160L).getCurrentServiceName(), "DL");
    }

    public void staffNamesIsParsed() {

        VisitStatusMap result = GlobalVariableParser.parseToVisitStatusMap(responseEntity);

        assertNull(result.get(275L).getStaffFirstName());
        assertNull(result.get(275L).getStaffLastName());
        assertEquals(result.get(160L).getStaffFirstName(), "Super");
        assertEquals(result.get(160L).getStaffLastName(), "Administrator");
    }

    public void servicePointInfoIsParsed() {

        VisitStatusMap result = GlobalVariableParser.parseToVisitStatusMap(responseEntity);

        assertNull(result.get(275L).getServicePointLogicId());
        assertNull(result.get(275L).getServicePointName());
        assertEquals(result.get(160L).getServicePointLogicId(), new Integer(22));
        assertEquals(result.get(160L).getServicePointName(), "Window 22");
    }

    public void testShortData() {
        String shortEntry = "A001|1337";
        ResponseEntity<String> responseEntity = mock(ResponseEntity.class);
        doReturn(shortEntry).when(responseEntity).getBody();

        VisitStatusMap result = GlobalVariableParser.parseToVisitStatusMap(responseEntity);

        assertEquals(result.get(1337L).getTicketId(), "A001");
    }
}