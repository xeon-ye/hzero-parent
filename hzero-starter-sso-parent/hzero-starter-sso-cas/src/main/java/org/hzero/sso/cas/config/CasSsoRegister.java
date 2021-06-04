package org.hzero.sso.cas.config;

import java.util.Set;

import com.google.common.collect.Sets;

import org.hzero.sso.core.constant.SsoConstant;
import org.hzero.sso.core.type.SsoRegister;

/**
 *
 * @author bojiangzhou 2020/04/15
 */
public class CasSsoRegister implements SsoRegister {

    @Override
    public Set<String> ids() {
        return Sets.newHashSet(SsoConstant.CAS, SsoConstant.CAS2, SsoConstant.CAS3);
    }
}
