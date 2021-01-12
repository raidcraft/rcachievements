package de.raidcraft.achievements;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.function.Function;

/**
 * A type registration exception is thrown when an error occurs while registering
 * the provided achievement type in {@link AchievementManager#register(String, Class, Function)}.
 */
@Value
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public class TypeRegistrationException extends RCAchievementException {

    AchievementType.Registration<?> registration;

    public TypeRegistrationException(String message, AchievementType.Registration<?> registration) {

        super(message);
        this.registration = registration;
    }

    public TypeRegistrationException(String message, Throwable cause, AchievementType.Registration<?> registration) {

        super(message, cause);
        this.registration = registration;
    }

    public Class<?> typeClass() {

        return registration.typeClass();
    }

    public String identifier() {

        return registration.identifier();
    }
}
