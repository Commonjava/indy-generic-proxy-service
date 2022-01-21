package org.commonjava.indy.service.httprox.client.repository;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StoreRequest
{

    private String key;

    private String name;

    private String type;

    private String packageType;

    private String description;

    private Map<String, String> metadata;

    private boolean disabled;

    @JsonProperty( "disable_timeout" )
    private int disableTimeout;

    @JsonProperty( "path_style" )
    private String pathStyle;

    @JsonProperty( "path_mask_patterns" )
    private Set<String> pathMaskPatterns;

    @JsonProperty("authoritative_index")
    private Boolean authoritativeIndex;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPackageType() {
        return packageType;
    }

    public void setPackageType(String packageType) {
        this.packageType = packageType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public int getDisableTimeout() {
        return disableTimeout;
    }

    public void setDisableTimeout(int disableTimeout) {
        this.disableTimeout = disableTimeout;
    }

    public String getPathStyle() {
        return pathStyle;
    }

    public void setPathStyle(String pathStyle) {
        this.pathStyle = pathStyle;
    }

    public Set<String> getPathMaskPatterns() {
        return pathMaskPatterns;
    }

    public void setPathMaskPatterns(Set<String> pathMaskPatterns) {
        this.pathMaskPatterns = pathMaskPatterns;
    }

    public Boolean getAuthoritativeIndex() {
        return authoritativeIndex;
    }

    public void setAuthoritativeIndex(Boolean authoritativeIndex) {
        this.authoritativeIndex = authoritativeIndex;
    }
}
