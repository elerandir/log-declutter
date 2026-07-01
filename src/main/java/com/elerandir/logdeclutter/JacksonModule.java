package com.elerandir.logdeclutter;

import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/** Provides the shared, thread-safe Jackson {@link ObjectMapper} to the object graph. */
@Module
public class JacksonModule {

    @Provides
    @Singleton
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
