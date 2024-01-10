/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/service-parent)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.service.httprox.config;

import io.quarkus.runtime.Startup;
import org.commonjava.indy.model.core.PathStyle;
import org.commonjava.indy.service.httprox.model.TrackingType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@Startup
@ApplicationScoped
public class ProxyConfiguration
{

    private static final boolean DEFAULT_SECURED = false;

    private static final int DEFAULT_MITM_SO_TIMEOUT_MINUTES = 30;

    private static final String DEFAULT_TRACKING_TYPE = TrackingType.SUFFIX.name();

    private static final String DEFAULT_PROXY_REALM = "httprox";

    private static final int DEFAULT_WORKER_IO_THREADS = 10;

    private static final int DEFAULT_WORKER_TASK_THREADS = 10;

    private static final String DEFAULT_STORAGE_PATH_STYLE = PathStyle.hashed.name();

    private static final int DEFAULT_EXECUTOR_MAX_ASYNC = 50;

    private static final int DEFAULT_EXECUTOR_MAX_QUEUED = 20;

    @ConfigProperty(name = "proxy.port")
    Optional<Integer> port;

    @ConfigProperty(name = "MITM.enabled")
    public Boolean MITMEnabled;

    @ConfigProperty(name = "MITM.ca.key")
    public String MITMCAKey;

    @ConfigProperty(name = "MITM.ca.cert")
    public String MITMCACert;

    @ConfigProperty(name = "MITM.dn.template")
    public String MITMDNTemplate;

    @ConfigProperty(name = "MITM.so.timeout.minutes")
    public Integer MITMSoTimeoutMinutes;

    @ConfigProperty(name="proxy.secured")
    public Boolean secured;

    @ConfigProperty(name="tracking.type")
    public String trackingType;

    @ConfigProperty( name="proxy.realm", defaultValue = DEFAULT_PROXY_REALM )
    public String proxyRealm;

    @ConfigProperty(name="proxy.worker.io.threads")
    public Integer ioThreads;

    @ConfigProperty(name="proxy.worker.task.threads")
    public Integer taskThreads;

    @ConfigProperty(name="proxy.worker.connection.high-water")
    public Integer highWater;

    @ConfigProperty(name="storage.path.style")
    public String storagePathStyle;

    @ConfigProperty(name="executor.mitm-transfers.max-async")
    public Integer mitmMaxAsync;

    @ConfigProperty(name="executor.mitm-transfers.max-queued")
    public Integer mitmMaxQueued;

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

    public Boolean isSecured() { return secured == null ? DEFAULT_SECURED : secured; }

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

    public String getProxyRealm()
    {
        return proxyRealm == null ? DEFAULT_PROXY_REALM : proxyRealm ;
    }

    public void setProxyRealm(String proxyRealm)
    {
        this.proxyRealm = proxyRealm;
    }

    public Integer getIoThreads() {
        return ioThreads == null ? DEFAULT_WORKER_IO_THREADS : ioThreads;
    }

    public void setIoThreads(Integer ioThreads) {
        this.ioThreads = ioThreads;
    }

    public Integer getTaskThreads() {
        return taskThreads == null ? DEFAULT_WORKER_TASK_THREADS : taskThreads;
    }

    public void setTaskThreads(Integer taskThreads) {
        this.taskThreads = taskThreads;
    }

    public String getStoragePathStyle()
    {
        return storagePathStyle == null ? DEFAULT_STORAGE_PATH_STYLE : storagePathStyle;
    }

    public void setStoragePathStyle(String storagePathStyle)
    {
        this.storagePathStyle = storagePathStyle;
    }


    public Integer getHighWater() {
        return highWater;
    }

    public void setHighWater(Integer highWater) {
        this.highWater = highWater;
    }

    public Integer getMitmMaxAsync() {
        return mitmMaxAsync == null ? DEFAULT_EXECUTOR_MAX_ASYNC : mitmMaxAsync;
    }

    public void setMitmMaxAsync(Integer mitmMaxAsync) {
        this.mitmMaxAsync = mitmMaxAsync;
    }

    public Integer getMitmMaxQueued() {
        return mitmMaxQueued == null ? DEFAULT_EXECUTOR_MAX_QUEUED : mitmMaxQueued;
    }

    public void setMitmMaxQueued(Integer mitmMaxQueued) {
        this.mitmMaxQueued = mitmMaxQueued;
    }
}