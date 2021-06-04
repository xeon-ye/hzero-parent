package org.hzero.sso.core.constant;

import org.hzero.common.HZeroService;

public class SsoConstant {

    public static final String DEFAULT_CALLBACK_SUFFIX = "callback";
    public static final String PARAM_PROVIDER = "provider";
    public static final String PARAM_CHANNEL = "channel";
    public static final String PARAM_STATE = "state";
    public static final String PARAM_BIND_REDIRECT_URI = "bind_redirect_uri";
    public static final String PARAM_OPEN_ID = "open_id";
    public static final String PARAM_UNION_ID = "union_id";
    public static final String PARAM_OPEN_ACCESS_TOKEN = "open_access_token";
    public static final String PARAM_OPEN_USER_NAME = "open_user_name";
    public static final String PARAM_IMAGE_URL = "image_url";

    public static final String PREFIX_ACCESS_TOKEN = "access_token";
    public static final String PREFIX_REDIRECT_URL = "redirect_url";

    public interface UrlParamKey {
        String TENANT_ID = "tenantId";
        String SERVER_URL = "server_url";
        String CAS_VERSION = "cas_version";
        String LOGIN_NAME_FIELD = "login_name_field";
        String SSO_HTTPS = "sso_https";
    }


    /**
     * 二级域名缓存
     */
    public static final String HIAM_DOMAIN = HZeroService.Iam.CODE +":domain";
    
    public static final String CAS = "CAS";

    public static final String SAML = "SAML";
    
    public static final String IDM = "IDM";
    
    public static final String AUTH = "AUTH";
    
    public static final String NULL = "NULL";
    
    public static final String CAS2 = "CAS2";
    
    public static final String CAS3 = "CAS3";

    public static final String AZURE = "AZURE";
}
