package org.commonjava.indy.service.httprox.util;

import static io.vertx.core.http.impl.HttpUtils.normalizePath;

public class ProxyUtils
{

    public static <R> R normalizePathAnd( String path, CheckedFunction<String, R> action ) throws Exception
    {
        return action.apply( normalizePath( path ) );
    }

    @FunctionalInterface
    public interface CheckedFunction<T, R>
    {
        R apply( T t ) throws Exception;
    }

}
