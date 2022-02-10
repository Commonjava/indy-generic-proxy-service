/**
 * Copyright (C) 2021 Red Hat, Inc. (https://github.com/Commonjava/indy-generic-proxy-service)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.service.httprox.config;

import io.quarkus.runtime.Startup;
import org.commonjava.indy.service.httprox.model.TrackingType;
import org.commonjava.propulsor.config.annotation.ConfigName;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;

@Startup
@ApplicationScoped
public class ProxyConfiguration {

    private static final int DEFAULT_MITM_SO_TIMEOUT_MINUTES = 30;

    private static final String DEFAULT_TRACKING_TYPE = TrackingType.SUFFIX.name();

    @Inject
    @ConfigProperty(name = "proxy.port")
    Optional<Integer> port;

    @Inject
    @ConfigProperty(name = "MITM.enabled")
    public Boolean MITMEnabled;

    @Inject
    @ConfigProperty(name = "MITM.ca.key")
    public String MITMCAKey;

    @Inject
    @ConfigProperty(name = "MITM.ca.cert")
    public String MITMCACert;

    @Inject
    @ConfigProperty(name = "MITM.dn.template")
    public String MITMDNTemplate;

    @Inject
    @ConfigProperty(name = "MITM.so.timeout.minutes")
    public Integer MITMSoTimeoutMinutes;

    @Inject
    @ConfigProperty(name="secured")
    public Boolean secured;

    @Inject
    @ConfigProperty(name="tracking.type")
    public String trackingType;

    public Integer getPort() {
        return port.orElse(8081);
    }

    public void setPort(Integer port) {
        this.port = Optional.of(port);
    }

    public Boolean isMITMEnabled() {
        return MITMEnabled;
    }

    public void setMITMEnabled(Boolean MITMEnabled) {
        this.MITMEnabled = MITMEnabled;
    }

    public void setMITMCAKey(String MITMCAKey )
    {
        this.MITMCAKey = MITMCAKey;
    }

    public String getMITMCAKey()
    {
        return MITMCAKey;
    }

    public String getMITMCACert()
    {
        return MITMCACert;
    }

    public void setMITMCACert( String MITMCACert )
    {
        this.MITMCACert = MITMCACert;
    }

    public String getMITMDNTemplate()
    {
        return MITMDNTemplate;
    }

    public void setMITMDNTemplate( String MITMDNTemplate )
    {
        this.MITMDNTemplate = MITMDNTemplate;
    }

    public Integer getMITMSoTimeoutMinutes()
    {
        return MITMSoTimeoutMinutes == null ? DEFAULT_MITM_SO_TIMEOUT_MINUTES : MITMSoTimeoutMinutes;
    }

    public void setMITMSoTimeoutMinutes( Integer MITMSoTimeoutMinutes )
    {
        this.MITMSoTimeoutMinutes = MITMSoTimeoutMinutes;
    }

    public Boolean getMITMEnabled() {
        return MITMEnabled;
    }

    public Boolean getSecured() {
        return secured;
    }

    public void setSecured(Boolean secured) {
        this.secured = secured;
    }

    public TrackingType getTrackingType()
    {
        return TrackingType.valueOf( trackingType == null ? DEFAULT_TRACKING_TYPE : trackingType.toUpperCase() );
    }

    public void setTrackingType(TrackingType trackingType) {
        this.trackingType = trackingType.name();
    }
}