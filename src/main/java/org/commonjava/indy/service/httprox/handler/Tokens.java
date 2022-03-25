package org.commonjava.indy.service.httprox.handler;

import javax.inject.Singleton;

@Singleton
public class Tokens
{

    private ThreadLocal<String> token = new ThreadLocal<>();

    public String getToken()
    {
        return token.get();
    }

    public void setToken( String token )
    {
        this.token.set( token );
    }

}
