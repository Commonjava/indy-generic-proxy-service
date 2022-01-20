package org.commonjava.indy.service.httprox.util;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.text.MessageFormat;

public class IndyProxyException extends Exception {
    private static final long serialVersionUID = 1L;

    private Object[] params;

    private int status;

    public IndyProxyException(final String message, final Throwable cause, final Object... params) {
        super(message, cause);
        this.params = params;
    }

    public IndyProxyException(final String message, final Object... params) {
        super(message);
        this.params = params;
    }

    public IndyProxyException(final int status, final String message, final Throwable cause, final Object... params) {
        super(message, cause);
        this.params = params;
        this.status = status;
    }

    public IndyProxyException(final int status, final String message, final Object... params) {
        super(message);
        this.params = params;
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        String format = super.getMessage();

        if (format == null || params == null || params.length < 1) {
            return format;
        }

        String formattedMessage = null;
        format = format.replaceAll("\\{\\}", "%s");

        try {
            formattedMessage = String.format(format, params);
        } catch (final Throwable e) {
            try {
                formattedMessage = MessageFormat.format(format, params);
            } catch (Throwable ex) {
                formattedMessage = format;
            }
        }

        return formattedMessage;
    }

    /**
     * Stringify all parameters pre-emptively on serialization, to prevent {@link NotSerializableException}.
     * Since all parameters are used in {@link String#format} or {@link MessageFormat#format}, flattening them
     * to strings is an acceptable way to provide this functionality without making the use of {@link Serializable}
     * viral.
     */
    private Object writeReplace() {
        final Object[] newParams = new Object[params.length];
        int i = 0;
        for (final Object object : params) {
            newParams[i] = String.valueOf(object);
            i++;
        }

        this.params = newParams;
        return this;
    }

}