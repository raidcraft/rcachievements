package de.raidcraft.achievements;

/**
 * The base exception for all exceptions thrown by the RCAchivements plugin.
 */
public abstract class RCAchievementException extends Throwable {

    public RCAchievementException() {

    }

    public RCAchievementException(String message) {

        super(message);
    }

    public RCAchievementException(String message, Throwable cause) {

        super(message, cause);
    }

    public RCAchievementException(Throwable cause) {

        super(cause);
    }

    public RCAchievementException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {

        super(message, cause, enableSuppression, writableStackTrace);
    }
}
