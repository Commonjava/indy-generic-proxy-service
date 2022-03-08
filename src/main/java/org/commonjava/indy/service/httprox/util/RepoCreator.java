package org.commonjava.indy.service.httprox.util;

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.service.httprox.handler.AbstractProxyRepositoryCreator;
import org.commonjava.indy.service.httprox.handler.ProxyCreationResult;
import org.slf4j.Logger;

import java.util.function.Predicate;

public class RepoCreator extends AbstractProxyRepositoryCreator
{

    public Predicate<ArtifactStore> getNameFilter(String name )
    {
        return store -> store.getName().startsWith( name );
    }

    @Override
    public ProxyCreationResult create(String trackingID, String name, String baseUrl, UrlInfo urlInfo, UserPass userPass, Logger logger) {
        ProxyCreationResult ret = new ProxyCreationResult();
        if (trackingID == null) {
            RemoteRepository remote = createRemote(name, baseUrl, urlInfo, userPass, logger);
            ret.setRemote(remote);
        } else {
            String host = urlInfo.getHost();
            int port = urlInfo.getPort();

            String remoteName = formatId(host, port, 0, trackingID, "remote");
            RemoteRepository remote = createRemote(trackingID, remoteName, baseUrl, urlInfo, userPass, logger);
            ret.setRemote(remote);

            String hostedName = formatId(host, port, 0, trackingID, "hosted");
            HostedRepository hosted = createHosted(trackingID, hostedName, urlInfo, logger);
            ret.setHosted(hosted);

            String groupName = formatId(host, port, 0, trackingID, "group");
            ret.setGroup(createGroup(trackingID, groupName, urlInfo, logger, remote.getKey(), hosted.getKey()));
        }
        return ret;
    }


}
