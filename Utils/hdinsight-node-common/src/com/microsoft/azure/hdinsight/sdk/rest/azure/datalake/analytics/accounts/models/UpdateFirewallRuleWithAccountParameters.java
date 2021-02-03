/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.rest.serializer.JsonFlatten;

/**
 * The parameters used to update a firewall rule while updating a Data Lake
 * Analytics account.
 */
@JsonFlatten
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateFirewallRuleWithAccountParameters {
    /**
     * The unique name of the firewall rule to update.
     */
    @JsonProperty(value = "name", required = true)
    private String name;

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Properties {
        /**
         * The start IP address for the firewall rule. This can be either ipv4 or
         * ipv6. Start and End should be in the same protocol.
         */
        @JsonProperty(value = "startIpAddress")
        private String startIpAddress;

        /**
         * The end IP address for the firewall rule. This can be either ipv4 or
         * ipv6. Start and End should be in the same protocol.
         */
        @JsonProperty(value = "endIpAddress")
        private String endIpAddress;
    }

    /**
     * The properties
     */
    @JsonProperty(value = "properties")
    private Properties properties;

    /**
     * Get the name value.
     *
     * @return the name value
     */
    public String name() {
        return this.name;
    }

    /**
     * Set the name value.
     *
     * @param name the name value to set
     * @return the UpdateFirewallRuleWithAccountParameters object itself.
     */
    public UpdateFirewallRuleWithAccountParameters withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get the startIpAddress value.
     *
     * @return the startIpAddress value
     */
    public String startIpAddress() {
        return this.properties == null ? null : properties.startIpAddress;
    }

    /**
     * Set the startIpAddress value.
     *
     * @param startIpAddress the startIpAddress value to set
     * @return the UpdateFirewallRuleWithAccountParameters object itself.
     */
    public UpdateFirewallRuleWithAccountParameters withStartIpAddress(String startIpAddress) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.startIpAddress = startIpAddress;
        return this;
    }

    /**
     * Get the endIpAddress value.
     *
     * @return the endIpAddress value
     */
    public String endIpAddress() {
        return this.properties == null ? null : properties.endIpAddress;
    }

    /**
     * Set the endIpAddress value.
     *
     * @param endIpAddress the endIpAddress value to set
     * @return the UpdateFirewallRuleWithAccountParameters object itself.
     */
    public UpdateFirewallRuleWithAccountParameters withEndIpAddress(String endIpAddress) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.endIpAddress = endIpAddress;
        return this;
    }

}
