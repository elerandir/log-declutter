package com.elerandir.logdeclutter;

import com.elerandir.logdeclutter.model.DeclutterConfig;
import dagger.BindsInstance;
import dagger.Component;
import javax.inject.Singleton;

/**
 * Dagger composition root for the application object graph.
 *
 * <p>The runtime {@link DeclutterConfig} is supplied at build time via the factory's
 * {@code @BindsInstance} parameter, making it injectable into the singleton services.
 */
@Singleton
@Component
public interface DeclutterComponent {

    /** The fully wired declutter service. */
    LogDeclutterer declutterer();

    @Component.Factory
    interface Factory {
        DeclutterComponent create(@BindsInstance DeclutterConfig config);
    }
}
