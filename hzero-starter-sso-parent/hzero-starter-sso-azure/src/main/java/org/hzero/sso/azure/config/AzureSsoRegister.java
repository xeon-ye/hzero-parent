package org.hzero.sso.azure.config;

import java.util.Set;

import com.google.common.collect.Sets;

import org.hzero.sso.core.constant.SsoConstant;
import org.hzero.sso.core.type.SsoRegister;

/**
 *
 * @author bojiangzhou 2020/04/15
 */
public class AzureSsoRegister implements SsoRegister {

    @Override
    public Set<String> ids() {
        return Sets.newHashSet(SsoConstant.AZURE);
    }
}
