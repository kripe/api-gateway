package com.qmatic.apigw.util;

import com.qmatic.apigw.rest.VisitStatus;
import com.qmatic.apigw.rest.VisitStatusMap;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

public class GlobalVariableParser {

    public static VisitStatusMap parseToVisitStatusMap(ResponseEntity<String> responseEntity) {
        VisitStatusMap visitStatusMap = new VisitStatusMap();
        List<String> tickets = splitResponse(responseEntity);
        for (String ticket: tickets) {
            VisitStatus visitStatus = convert(ticket);
            if (visitStatus.getVisitId() != null) {
                visitStatusMap.put(visitStatus.getVisitId(), visitStatus);
            }
        }

        return visitStatusMap;
    }

    private static List<String> splitResponse(ResponseEntity<String> responseEntity) {
        return Arrays.asList(responseEntity.getBody().split("@"));
    }

    private static VisitStatus convert(String ticket) {
        String[] fields = ticket.split("\\|", 11);
        VisitStatus visitStatus = new VisitStatus();
        visitStatus.setTicketId(getField(fields, 0));
        visitStatus.setVisitId(getLong(getField(fields, 1)));
        visitStatus.setChecksum(getField(fields, 2));
        visitStatus.setPositionInQueue(getInt(getField(fields, 3)));
        visitStatus.setQueueSize(getInt(getField(fields, 4)));
        visitStatus.setCurrentStatus(getField(fields, 5));
        visitStatus.setCurrentServiceName(getField(fields, 6));
        visitStatus.setStaffFirstName(getField(fields, 7));
        visitStatus.setStaffLastName(getField(fields, 8));
        visitStatus.setServicePointLogicId(getInt(getField(fields, 9)));
        visitStatus.setServicePointName(getField(fields, 10));
        return visitStatus;
    }

    private static String getField(String[] fields, int i) {
        if (i < fields.length) {
            return StringUtils.isBlank(fields[i]) || "null".equals(fields[i]) ? null : fields[i];
        }
        return null;
    }

    private static Long getLong(String field) {
        return field == null ? null : Long.valueOf(field);
    }

    private static Integer getInt(String field) {
        return field == null ? null : Integer.valueOf(field);
    }
}