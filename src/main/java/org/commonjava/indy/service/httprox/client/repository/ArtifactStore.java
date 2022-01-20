package org.commonjava.indy.service.httprox.client.repository;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ArtifactStore {

    public StoreKey key;


    public StoreKey getKey() {
        return key;
    }

    public void setKey(StoreKey key) {
        this.key = key;
    }
}
