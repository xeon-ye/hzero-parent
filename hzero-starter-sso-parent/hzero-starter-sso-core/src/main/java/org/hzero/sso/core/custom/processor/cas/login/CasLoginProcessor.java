package org.hzero.sso.core.custom.processor.cas.login;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hzero.sso.core.custom.processor.SsoProcessor;

/**
 * Cas 认证处理器
 *
 * @author bojiangzhou 2019/11/21
 */
public interface CasLoginProcessor extends SsoProcessor {

    @Override
    default Object process(HttpServletRequest request, HttpServletResponse response) {
        return null;
    }

    /**
     * @param ticket cas ticket
     */
    Object process(HttpServletRequest request, HttpServletResponse response, String ticket);


}
