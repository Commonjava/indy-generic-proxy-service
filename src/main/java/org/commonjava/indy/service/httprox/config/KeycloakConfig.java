/**
 * Copyright (C) 2021-2023 Red Hat, Inc. (https://github.com/Commonjava/service-parent)
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
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Properties;

@Startup
@ApplicationScoped
public class KeycloakConfig
{

    private static final boolean DEFAULT_ENABLED = false;

    private static final String DEFAULT_REALM = "indy";

    private static final String DEFAULT_KEYCLOAK_JSON = "keycloak/keycloak.json";

    private static final String DEFAULT_KEYCLOAK_UI_JSON = "keycloak/keycloak-ui.json";

    private static final String DEFAULT_SECURITY_BINDINGS_JSON = "keycloak/security-bindings.json";

    private static final String DEFAULT_SERVER_RESOURCE = "indy";

    private static final String DEFAULT_UI_RESOURCE = "indy-ui";

    public static final String KEYCLOAK_REALM = "keycloak.realm";

    public static final String KEYCLOAK_URL = "keycloak.url";

    public static final String KEYCLOAK_SERVER_RESOURCE = "keycloak.serverResource";

    public static final String KEYCLOAK_UI_RESOURCE = "keycloak.uiResource";

    private static final String KEYCLOAK_SERVER_CREDENTIAL_SECRET = "keycloak.serverCredentialSecret";

    public static final String KEYCLOAK_REALM_PUBLIC_KEY = "keycloak.realmPublicKey";

    @ConfigProperty( name = "auth.realm", defaultValue = DEFAULT_REALM )
    public String realm;

    @ConfigProperty(name = "auth.enabled")
    public Boolean enabled;

    @ConfigProperty(name = "auth.keycloak-json")
    public String keycloakJson;

    @ConfigProperty(name = "auth.keycloak-ui-json")
    public String keycloakUiJson;

    @ConfigProperty(name = "auth.security-bindings-json")
    public String securityBindingsJson;

    @ConfigProperty(name = "auth.url")
    public String url;

    @ConfigProperty(name = "auth.server.credential.secret")
    public String serverCredentialSecret;

    @ConfigProperty(name = "auth.server.resource")
    public String serverResource;

    @ConfigProperty(name = "auth.ui.resource")
    public String uiResource;

    @ConfigProperty(name = "auth.public-key")
    public String realmPublicKey;

    public boolean isEnabled()
    {
        return enabled == null ? DEFAULT_ENABLED : enabled;
    }

    public void setEnabled( final Boolean enabled )
    {
        this.enabled = enabled;
    }

    public String getRealm()
    {
        return realm == null ? DEFAULT_REALM : realm;
    }

    public void setRealm( final String realm )
    {
        this.realm = realm;
    }

    public String getKeycloakJson()
    {
        if ( keycloakJson == null )
        {
            return getDefaultConfFile( DEFAULT_KEYCLOAK_JSON );
        }

        return keycloakJson;
    }

    public void setKeycloakJson( final String keycloakJson )
    {
        this.keycloakJson = keycloakJson;
    }

    public String getKeycloakUiJson()
    {
        if ( keycloakUiJson == null )
        {
            return getDefaultConfFile( DEFAULT_KEYCLOAK_UI_JSON );
        }

        return keycloakUiJson;
    }

    public void setKeycloakUiJson( final String keycloakUiJson )
    {
        this.keycloakUiJson = keycloakUiJson;
    }

    public String getSecurityBindingsJson()
    {
        if ( securityBindingsJson == null )
        {
            return getDefaultConfFile( DEFAULT_SECURITY_BINDINGS_JSON );
        }

        return securityBindingsJson;
    }

    public void setSecurityBindingsJson( final String securityConstraintsJson )
    {
        this.securityBindingsJson = securityConstraintsJson;
    }

    /**
     * Set system properties for keycloak to use when filtering keycloak.json...
     */
    public KeycloakConfig setSystemProperties()
    {
        if ( !isEnabled() )
        {
            return this;
        }

        final Properties properties = System.getProperties();
        properties.setProperty( KEYCLOAK_REALM, getRealm() );
        properties.setProperty( KEYCLOAK_URL, getUrl() );

        if ( getServerResource() != null )
        {
            properties.setProperty( KEYCLOAK_SERVER_RESOURCE, getServerResource() );
        }

        if ( getServerCredentialSecret() != null )
        {
            properties.setProperty( KEYCLOAK_SERVER_CREDENTIAL_SECRET, getServerCredentialSecret() );
        }

        if ( getRealmPublicKey() != null )
        {
            properties.setProperty( KEYCLOAK_REALM_PUBLIC_KEY, getRealmPublicKey() );
        }

        System.setProperties( properties );

        return this;
    }

    private String getDefaultConfFile( final String confFile )
    {
        //TODO
        return "";
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl( final String url )
    {
        this.url = url;
    }

    public String getServerCredentialSecret()
    {
        return serverCredentialSecret;
    }

    public void setServerCredentialSecret( final String serverCredentialSecret )
    {
        this.serverCredentialSecret = serverCredentialSecret;
    }

    public String getServerResource()
    {
        return serverResource == null ? DEFAULT_SERVER_RESOURCE : serverResource;
    }

    public void setServerResource( final String serverResource )
    {
        this.serverResource = serverResource;
    }

    public String getUiResource()
    {
        return uiResource == null ? DEFAULT_UI_RESOURCE : uiResource;
    }

    public void setUiResource( final String uiResource )
    {
        this.uiResource = uiResource;
    }

    public String getRealmPublicKey()
    {
        return realmPublicKey;
    }

    public void setRealmPublicKey( final String realmPublicKey )
    {
        this.realmPublicKey = realmPublicKey;
    }

}
