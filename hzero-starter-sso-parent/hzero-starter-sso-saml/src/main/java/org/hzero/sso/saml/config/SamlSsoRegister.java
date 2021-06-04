package org.hzero.sso.saml.config;

import java.util.Set;

import com.google.common.collect.Sets;

import org.hzero.sso.core.constant.SsoConstant;
import org.hzero.sso.core.type.SsoRegister;

/**
 *
 * @author bojiangzhou 2020/04/15
 */
public class SamlSsoRegister implements SsoRegister {

    @Override
    public Set<String> ids() {
        return Sets.newHashSet(SsoConstant.SAML);
    }
}
