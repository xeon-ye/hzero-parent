package org.hzero.sso.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = SsoProperties.PREFIX)
public class SsoProperties {

    public static final String PREFIX = "hzero.oauth";

    private Login login = new Login();

    private Sso sso = new Sso();

    public static class Sso {
        /**
         * 启用二级域名单点登录
         */
        private boolean enabled = true;
        /**
         * 可通过此参数禁止跳转到 sso 页面
         */
        private String disableSsoParameter = "disable_sso";

        private Service service = new Service();

        private Provider provider = new Provider();

        private Saml saml = new Saml();

        public static class Service {
            /**
             * CAS Service base url (your application base url)
             */
            private String baseUrl;

            /**
             * CAS Service login path that will be append to
             * {@link Service#baseUrl}
             */
            private String loginPath = "/login/cas";

            /**
             * CAS Service logout path that will be append to
             * {@link Service#baseUrl}
             */
            private String logoutPath = "/logout";

            /**
             * CAS Service proxy callback url
             */
            private String proxyReceptorUrl;

            /**
             * CAS Service failure handler url
             */
            private String failureHandlerUrl;
            /**
             * CAS Service proxy failure handler url
             */
            private String proxyFailureHandlerUrl;

            public String getBaseUrl() {
                return baseUrl;
            }

            public void setBaseUrl(String baseUrl) {
                this.baseUrl = baseUrl;
            }

            public String getLoginPath() {
                return loginPath;
            }

            public void setLoginPath(String loginPath) {
                this.loginPath = loginPath;
            }

            public String getLogoutPath() {
                return logoutPath;
            }

            public void setLogoutPath(String logoutPath) {
                this.logoutPath = logoutPath;
            }

            public String getProxyReceptorUrl() {
                return proxyReceptorUrl;
            }

            public void setProxyReceptorUrl(String proxyReceptorUrl) {
                this.proxyReceptorUrl = proxyReceptorUrl;
            }

            public String getFailureHandlerUrl() {
                return failureHandlerUrl;
            }

            public void setFailureHandlerUrl(String failureHandlerUrl) {
                this.failureHandlerUrl = failureHandlerUrl;
            }

            public String getProxyFailureHandlerUrl() {
                return proxyFailureHandlerUrl;
            }

            public void setProxyFailureHandlerUrl(String proxyFailureHandlerUrl) {
                this.proxyFailureHandlerUrl = proxyFailureHandlerUrl;
            }
        }

        public static class Provider {
            /**
             * Authentication provider key
             */
            private String key;

            public String getKey() {
                return key;
            }

            public void setKey(String key) {
                this.key = key;
            }
        }

        public static class Saml {

            private String entityId;

            private String passphrase;

            private String privateKey;

            private String certificate;

            public String getPassphrase() {
                return passphrase;
            }

            public void setPassphrase(String passphrase) {
                this.passphrase = passphrase;
            }

            public String getPrivateKey() {
                return privateKey;
            }

            public void setPrivateKey(String privateKey) {
                this.privateKey = privateKey;
            }

            public String getCertificate() {
                return certificate;
            }

            public void setCertificate(String certificate) {
                this.certificate = certificate;
            }

            public String getEntityId() {
                return entityId;
            }

            public void setEntityId(String entityId) {
                this.entityId = entityId;
            }
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Service getService() {
            return service;
        }

        public void setService(Service service) {
            this.service = service;
        }

        public Provider getProvider() {
            return provider;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }

        public Saml getSaml() {
            return saml;
        }

        public void setSaml(Saml saml) {
            this.saml = saml;
        }

        public String getDisableSsoParameter() {
            return disableSsoParameter;
        }

        public void setDisableSsoParameter(String disableSsoParameter) {
            this.disableSsoParameter = disableSsoParameter;
        }
    }

    public static class Login {
        /**
         * 登录页面
         */
        private String page = "/login";
        /**
         * 登录成功地址
         */
        private String successUrl = "/";
        /**
         * #网关是否启用https
         */
        private boolean enableHttps;
        /**
         * 默认的客户端
         */
        private String defaultClientId = "client";

        public String getPage() {
            return page;
        }

        public void setPage(String page) {
            this.page = page;
        }

        public String getSuccessUrl() {
            return successUrl;
        }

        public void setSuccessUrl(String successUrl) {
            this.successUrl = successUrl;
        }

        public boolean isEnableHttps() {
            return enableHttps;
        }

        public void setEnableHttps(boolean enableHttps) {
            this.enableHttps = enableHttps;
        }

        public String getDefaultClientId() {
            return defaultClientId;
        }

        public void setDefaultClientId(String defaultClientId) {
            this.defaultClientId = defaultClientId;
        }
    }

    public Sso getSso() {
        return sso;
    }

    public void setSso(Sso sso) {
        this.sso = sso;
    }

    public Login getLogin() {
        return login;
    }

    public void setLogin(Login login) {
        this.login = login;
    }
}
