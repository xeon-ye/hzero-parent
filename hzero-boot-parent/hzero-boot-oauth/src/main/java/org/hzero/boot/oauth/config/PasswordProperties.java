package org.hzero.boot.oauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * @author bojiangzhou 2019/08/07
 */
@ConfigurationProperties(prefix = PasswordProperties.PREFIX)
public class PasswordProperties {

    public static final String PREFIX = "hzero";

    private Captcha captcha = new Captcha();

    public Captcha getCaptcha() {
        return captcha;
    }

    public void setCaptcha(Captcha captcha) {
        this.captcha = captcha;
    }

    public static class Captcha {
        /**
         * enable image captcha first
         */
        private boolean alwaysEnable = false;

        public boolean isAlwaysEnable() {
            return alwaysEnable;
        }

        public void setAlwaysEnable(boolean alwaysEnable) {
            this.alwaysEnable = alwaysEnable;
        }
    }



}
