package org.hzero.sso.core.util;

import org.hzero.common.HZeroService;

/**
 *
 * @author bojiangzhou 2019/11/21
 */
public final class CasUtils {

    public static final String KEY_CAS_TICKET_TOKEN = HZeroService.Oauth.CODE + ":cas:cas_ticket_token:";
    public static final String KEY_CAS_TOKEN_TICKET = HZeroService.Oauth.CODE + ":cas:cas_token_ticket:";
    public static final String KEY_TOKEN_LOGOUT_URL = HZeroService.Oauth.CODE + ":cas:token_logout_url:";

    public static final String ATTRIBUTE_CAS_TICKET = "ATTRIBUTE_CAS_TICKET";

}
