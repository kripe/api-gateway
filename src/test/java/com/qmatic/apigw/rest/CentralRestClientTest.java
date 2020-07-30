package com.qmatic.apigw.rest;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class CentralRestClientTest {

    private CentralRestClient testee;

    @BeforeMethod
    public void setUp() {
        testee = new CentralRestClient();
    }

    @Test
    void doNotFetchFromGlobalVariableByDefault() {

        boolean shouldFetch = testee.shouldFetchFromGlobalVariables(1337L);

        assertFalse(shouldFetch);
    }

    @Test
    public void doNotFetchFromGlobalVariablesIfUrlIsNull() {
        testee.getVisitsUsingGlobalVariables = true;
        testee.visitsOnBranchFromGlobalVariablesUrl = null;

        boolean shouldFetch = testee.shouldFetchFromGlobalVariables(1337L);

        assertFalse(shouldFetch);
    }

    @Test
    public void doNotFetchFromGlobalVariablesIfUrlIsEmpty() {
        testee.getVisitsUsingGlobalVariables = true;
        testee.visitsOnBranchFromGlobalVariablesUrl = " ";

        boolean shouldFetch = testee.shouldFetchFromGlobalVariables(1337L);

        assertFalse(shouldFetch);
    }

    @Test
    public void doNotFetchFromGlobalVariablesIfFlagIsDisabled() {
        testee.getVisitsUsingGlobalVariables = false;
        testee.visitsOnBranchFromGlobalVariablesUrl = "http://localhost:8080";

        boolean shouldFetch = testee.shouldFetchFromGlobalVariables(1337L);

        assertFalse(shouldFetch);
    }

    @Test
    public void fetchFromGlobalVariablesIfEnabledAndUrlIsSet() {
        testee.getVisitsUsingGlobalVariables = true;
        testee.visitsOnBranchFromGlobalVariablesUrl = "http://localhost:8080";

        boolean shouldFetch = testee.shouldFetchFromGlobalVariables(1337L);

        assertTrue(shouldFetch);
    }

    @Test
    public void doNotFetchFromGlobalVariablesIfBranchInDisallowedList() {
        testee.getVisitsUsingGlobalVariables = true;
        testee.visitsOnBranchFromGlobalVariablesUrl = "http://localhost:8080";
        testee.globalVariableDisallowBranches = Arrays.asList(42L);

        boolean shouldFetch = testee.shouldFetchFromGlobalVariables(42L);

        assertFalse(shouldFetch);
    }

    @Test
    public void doNotFetchFromGlobalVariablesIfBranchIsNotInDefinedAllowedList() {
        testee.getVisitsUsingGlobalVariables = true;
        testee.visitsOnBranchFromGlobalVariablesUrl = "http://localhost:8080";
        testee.globalVariableAllowBranches = Arrays.asList(42L, 666L, 1337L);

        boolean shouldFetch = testee.shouldFetchFromGlobalVariables(34L);

        assertFalse(shouldFetch);
    }

    @Test
    public void fetchFromGlobalVariablesIfBranchAllowedListIsNull() {
        testee.getVisitsUsingGlobalVariables = true;
        testee.visitsOnBranchFromGlobalVariablesUrl = "http://localhost:8080";
        testee.globalVariableAllowBranches = null;

        boolean shouldFetch = testee.shouldFetchFromGlobalVariables(34L);

        assertTrue(shouldFetch);
    }

    @Test
    public void fetchFromGlobalVariablesIfBranchAllowedListIsEmpty() {
        testee.getVisitsUsingGlobalVariables = true;
        testee.visitsOnBranchFromGlobalVariablesUrl = "http://localhost:8080";
        testee.globalVariableAllowBranches = new ArrayList<>();

        boolean shouldFetch = testee.shouldFetchFromGlobalVariables(34L);

        assertTrue(shouldFetch);
    }

    @Test
    public void fetchFromGlobalVariablesIfBranchDisallowedListIsNull() {
        testee.getVisitsUsingGlobalVariables = true;
        testee.visitsOnBranchFromGlobalVariablesUrl = "http://localhost:8080";
        testee.globalVariableDisallowBranches = null;

        boolean shouldFetch = testee.shouldFetchFromGlobalVariables(34L);

        assertTrue(shouldFetch);
    }

    @Test
    public void fetchFromGlobalVariablesIfBranchDisallowedListIsEmpty() {
        testee.getVisitsUsingGlobalVariables = true;
        testee.visitsOnBranchFromGlobalVariablesUrl = "http://localhost:8080";
        testee.globalVariableDisallowBranches = new ArrayList<>();

        boolean shouldFetch = testee.shouldFetchFromGlobalVariables(34L);

        assertTrue(shouldFetch);
    }

    @Test
    public void doNotFetchFromGlobalVariablesIfBranchIsAllowedButFunctionNotEnabled() {
        testee.getVisitsUsingGlobalVariables = false;
        testee.visitsOnBranchFromGlobalVariablesUrl = "http://localhost:8080";
        testee.globalVariableAllowBranches = Arrays.asList(42L, 666L, 1337L);

        boolean shouldFetch = testee.shouldFetchFromGlobalVariables(1337L);

        assertFalse(shouldFetch);
    }

    @Test
    public void doNotFetchFromGlobalVariablesIfBranchIsBothAllowedAndDisallowed() {
        testee.getVisitsUsingGlobalVariables = false;
        testee.visitsOnBranchFromGlobalVariablesUrl = "http://localhost:8080";
        testee.globalVariableAllowBranches = Arrays.asList(42L, 666L, 1337L);
        testee.globalVariableDisallowBranches = Arrays.asList(666L);

        boolean shouldFetch = testee.shouldFetchFromGlobalVariables(666L);

        assertFalse(shouldFetch);
    }
}