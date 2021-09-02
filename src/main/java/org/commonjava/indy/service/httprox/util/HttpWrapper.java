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
package org.commonjava.indy.service.httprox.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public interface HttpWrapper
        extends Closeable {
    void writeError(Throwable e)
            throws IOException;

    void writeHeader(ApplicationHeader header, String value)
            throws IOException;

    void writeHeader(String header, String value)
            throws IOException;

    void writeStatus(ApplicationStatus status)
            throws IOException;

    void writeStatus(int code, String message)
            throws IOException;

    boolean isOpen();

    List<String> getHeaders(String name);
}
