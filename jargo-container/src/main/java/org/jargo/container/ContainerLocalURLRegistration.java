package org.jargo.container;

import org.jargo.URLRegistration;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

public class ContainerLocalURLRegistration implements URLRegistration {

    private final List<URL> urls;
    private final ClassLoader loader;
    
    public ContainerLocalURLRegistration() {
        try {
            this.urls = Arrays.asList(new URL[] { new URL("file:.")});
        } catch (MalformedURLException e) {
            throw new AssertionError();
        }

        this.loader = AccessController.doPrivileged(
                new PrivilegedAction<ClassLoader>() {
                    public ClassLoader run() {
                        // PERMISSION: java.lang.RuntimePermission getClassLoader
                        return Container.class.getClassLoader();
                    }
                });
        
    }
    
    public List<URL> getURLs() {
        return urls;
    }

    public ClassLoader getClassLoader() {
        return loader;
    }
}
