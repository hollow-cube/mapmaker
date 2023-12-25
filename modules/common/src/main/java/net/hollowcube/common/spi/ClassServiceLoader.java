package net.hollowcube.common.spi;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Like {@link java.util.ServiceLoader}, but for classes rather than trying to instantiate them.
 *
 * <p>The general use case is to later instantiate the classes with Guava.</p>
 */
public class ClassServiceLoader {

    public static @NotNull List<Class<?>> load(@NotNull Class<?> service) {
        List<Class<?>> serviceClasses = new ArrayList<>();
        String serviceFile = "META-INF/services/" + service.getName();

        // Use the class loader to find all service files
        try {
            Enumeration<URL> serviceFiles = Thread.currentThread().getContextClassLoader().getResources(serviceFile);
            while (serviceFiles.hasMoreElements()) {
                URL serviceURL = serviceFiles.nextElement();

                // Read each service file and load the class names
                try (InputStream input = serviceURL.openStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Class<?> serviceClass = Class.forName(line);
                        serviceClasses.add(serviceClass);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return serviceClasses;
    }
}
