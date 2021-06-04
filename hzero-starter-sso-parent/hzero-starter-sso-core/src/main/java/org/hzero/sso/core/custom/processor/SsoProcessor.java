package org.hzero.sso.core.custom.processor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author XCXCXCXCX
 * @since 1.0
 */
public interface SsoProcessor {

    Object process(HttpServletRequest request, HttpServletResponse response);

    int getOrder();
}
