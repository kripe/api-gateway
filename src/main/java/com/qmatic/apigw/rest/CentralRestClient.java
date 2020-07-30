package com.qmatic.apigw.rest;

import com.qmatic.apigw.GatewayConstants;
import com.qmatic.apigw.SslCertificateManager;
import com.qmatic.apigw.exception.CentralCommunicationException;
import com.qmatic.apigw.filters.FilterConstants;
import com.qmatic.apigw.properties.OrchestraProperties;
import com.qmatic.apigw.util.GlobalVariableParser;
import com.qmatic.common.geo.Branch;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;

@Component
public final class CentralRestClient {

    private static final Logger log = LoggerFactory.getLogger(CentralRestClient.class);
    private static final String PATH_SERVICE_ID = "{serviceId}";

    @Value("${geoService.branches_url}")
    private String mobileBranchesUrl;
    @Value("${geoService.service_branches_url}")
    private String mobileServiceBranchesUrl;
    @Value("${currentStatus.visits_on_branch_url}")
    private String visitsOnBranchUrl;
    @Value("${currentStatus.visitsOnBranchFromGlobalVariablesUrl:}")
    String visitsOnBranchFromGlobalVariablesUrl;
    @Value("${orchestra.central.getVisitsUsingGlobalVariables:false}")
    Boolean getVisitsUsingGlobalVariables = false;
    @Value("#{'${orchestra.central.globalVariableAllowBranches:}'.split(',')}")
    List<Long> globalVariableAllowBranches = new ArrayList<>();
    @Value("#{'${orchestra.central.globalVariableDisallowBranches:}'.split(',')}")
    List<Long> globalVariableDisallowBranches = new ArrayList<>();
    @Value("${orchestra.central.connectTimeout:0}")
    String connectTimeout;
    @Value("${orchestra.central.readTimeout:0}")
    String readTimeout;
    @Autowired
    private RestTemplateBuilder restTemplateBuilder;
    private RestTemplate restTemplate;
    private RestTemplate restTemplateForGlobalVariables;
    // This needs to be loaded before this class to enable the keystore settings to be applied before this class loads it's components
    @Autowired
    private SslCertificateManager sslCertificateManager;

    @PostConstruct
    protected void init() {
        CentralHttpErrorHandler centralErrorHandler = new CentralHttpErrorHandler();
        if (getConnectTimeout() != 0 || getReadTimeout() != 0) {
            restTemplate = restTemplateBuilder
                    .setConnectTimeout(getConnectTimeout())
                    .setReadTimeout(getReadTimeout())
                    .build();
            restTemplateForGlobalVariables = restTemplateBuilder
                    .setConnectTimeout(getConnectTimeout())
                    .setReadTimeout(getReadTimeout())
                    .build();
        } else {
            restTemplate = new RestTemplate();
            restTemplateForGlobalVariables = new RestTemplate();
        }
        restTemplate.setErrorHandler(centralErrorHandler);

        globalVariableAllowBranches.remove(null);
        globalVariableDisallowBranches.remove(null);
        log.info("connectTimeout: {}", connectTimeout);
        log.info("readTimeout: {}", readTimeout);
    }

    private int getConnectTimeout() {
        return parseInteger(connectTimeout, "orchestra.central.connectTimeout");
    }

    private int parseInteger(String value, String attribute) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            log.error("Unable to use value {} from setting {}, it is not a valid integer. Defaulting to 0.", value, attribute);
        }
        return 0;
    }

    private int getReadTimeout() {
        return parseInteger(readTimeout, "orchestra.central.readTimeout");
    }

    /**
     * @throws com.qmatic.apigw.exception.CentralCommunicationException Thrown upon received error from central
     */
    public Branch[] getAllBranchesFromCentral(OrchestraProperties.UserCredentials userCredentials) {
        try {
            log.debug("Retrieving all branches from central");
            ResponseEntity<Branch[]> allBranches = restTemplate.exchange(mobileBranchesUrl, HttpMethod.GET,
                new HttpEntity<>(createAuthorizationHeader(userCredentials)), Branch[].class, new Object[]{});
            return allBranches.getBody();
        } catch(RuntimeException e) {
            throw logAndConvertException(e);
        }
    }

    private RuntimeException logAndConvertException(RuntimeException e) {
        log.warn("", e);
        if (e instanceof ResourceAccessException && e.getCause() instanceof IOException) {
            return new CentralCommunicationException("1000", e.getMessage(), GATEWAY_TIMEOUT.value(), "");
        } else {
            throw e;
        }
    }

    /**
     * @throws com.qmatic.apigw.exception.CentralCommunicationException Thrown upon received error from central
     */
    public Branch[] getBranchesForServiceFromCentral(Long serviceId, OrchestraProperties.UserCredentials userCredentials) {
        try {
            log.debug("Retrieving branches for service {} from central", serviceId);
            try {
                String url = mobileServiceBranchesUrl.replace(PATH_SERVICE_ID, Long.toString(serviceId));
                ResponseEntity<Branch[]> allBranches = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(createAuthorizationHeader(userCredentials)), Branch[].class, new Object[]{});
                return allBranches.getBody();
            } catch (IllegalArgumentException e) {
                log.debug("Could not replace " + PATH_SERVICE_ID + " in: " + mobileServiceBranchesUrl);
            }
            return new Branch[] {};
        } catch(RuntimeException e) {
            throw logAndConvertException(e);
        }
    }

    private HttpHeaders createAuthorizationHeader(final OrchestraProperties.UserCredentials userCredentials) {
        return new HttpHeaders() {
            {
                String auth = userCredentials.getUser() + ":" + userCredentials.getPasswd();
                byte[] encodedAuth = Base64.encodeBase64(
                    auth.getBytes(GatewayConstants.UTF8_CHARSET));
                String authHeader = "Basic " + new String(encodedAuth, Charset.forName("US-ASCII"));
                set("Authorization", authHeader);
            }
        };
    }

    public VisitStatusMap getAllVisitsOnBranch(Long branchId, OrchestraProperties.UserCredentials userCredentials) {
        VisitStatusMap visitStatusMap = null;
        try {
            if (shouldFetchFromGlobalVariables(branchId)) {
                visitStatusMap = getVisitsOnBranchFromCallViaGlobalVariables(branchId, userCredentials);
            }
            if (visitStatusMap == null) {
                return getVisitsOnBranchFromCallViaMobileConnector(branchId, userCredentials);
            }
        } catch(RuntimeException e) {
            throw logAndConvertException(e);
        }
        return visitStatusMap;
    }

    boolean shouldFetchFromGlobalVariables(Long branchId) {
        return globalVariableSettingsAreEnabled() &&
                branchIsNotInForbiddenList(branchId) &&
                branchIsInAllowedListIfDefined(branchId);
    }

    private boolean globalVariableSettingsAreEnabled() {
        return getVisitsUsingGlobalVariables &&
                StringUtils.isNotBlank(visitsOnBranchFromGlobalVariablesUrl);
    }

    private boolean branchIsNotInForbiddenList(Long branchId) {
        return !(globalVariableDisallowBranches != null && globalVariableDisallowBranches.contains(branchId));
    }

    private boolean branchIsInAllowedListIfDefined(Long branchId) {
        return globalVariableAllowBranches == null || globalVariableAllowBranches.isEmpty() || globalVariableAllowBranches.contains(branchId);
    }

    private VisitStatusMap getVisitsOnBranchFromCallViaGlobalVariables(Long branchId, OrchestraProperties.UserCredentials userCredentials) {
        log.debug("Retrieving visits on branch {} from central global variables", branchId);
        try {
            String url = visitsOnBranchFromGlobalVariablesUrl.replace(FilterConstants.BRANCH_ID_PATTERN, Long.toString(branchId));
            ResponseEntity<String> allVisitsOnBranch = restTemplateForGlobalVariables.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(createAuthorizationHeader(userCredentials)), String.class, new Object[]{});
            if (allVisitsOnBranch.getStatusCodeValue() == 200) {
                return GlobalVariableParser.parseToVisitStatusMap(allVisitsOnBranch);
            }
        } catch (IllegalArgumentException | HttpClientErrorException | HttpServerErrorException e) {
            log.debug("Could not fetch visits for branch {} from central global variables", branchId);
        }
        return null;
    }

    private VisitStatusMap getVisitsOnBranchFromCallViaMobileConnector(Long branchId, OrchestraProperties.UserCredentials userCredentials) {
        try {
            log.debug("Retrieving visits on branch {} from central", branchId);
            try {
                String url = visitsOnBranchUrl.replace(FilterConstants.BRANCH_ID_PATTERN, Long.toString(branchId));
                ResponseEntity<VisitStatusMap> allVisitsOnBranch = restTemplate.exchange(url, HttpMethod.GET,
                        new HttpEntity<>(createAuthorizationHeader(userCredentials)), VisitStatusMap.class, new Object[]{});
                return allVisitsOnBranch.getBody();
            } catch (IllegalArgumentException e) {
                log.debug("Could not fetch visits for branch {} from central", branchId);
            }
            return new VisitStatusMap();
        } catch(RuntimeException e) {
            throw logAndConvertException(e);
        }
    }
}