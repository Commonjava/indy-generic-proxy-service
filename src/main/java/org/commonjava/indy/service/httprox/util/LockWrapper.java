package org.commonjava.indy.service.httprox.util;

import com.google.common.util.concurrent.Striped;

import java.util.concurrent.locks.Lock;

public class LockWrapper
{

    private final static Striped<Lock> striped = Striped.lock(50);

    public static Lock getLockByKey( String key )
    {
        return striped.get( key );
    }

}
