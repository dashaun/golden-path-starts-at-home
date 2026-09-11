package com.dashaun.golden.binding;

import java.util.List;

import org.springframework.core.env.Environment;

/**
 * A place a platform might leave a binding.
 */
public interface BindingSource {

    /**
     * @param environment the environment being prepared
     * @return every binding this source can see, in no particular order
     */
    List<ServiceBinding> bindings(Environment environment);
}
