package de.raidcraft.achievements.entities;

import io.ebean.annotation.DbJson;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import net.silthus.ebean.BaseEntity;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static de.raidcraft.achievements.Constants.TABLE_PREFIX;

@Getter
@Setter
@Accessors(fluent = true)
@Entity
@Table(name = TABLE_PREFIX + "datastore")
@Log(topic = "RCSkills")
public class DataStore extends BaseEntity {

    @DbJson
    private Map<String, Object> data = new HashMap<>();

    /**
     * @return gets the raw data of this data store.
     */
    public Map<String, Object> data() {

        if (data == null) {
            data = new HashMap<>();
        }
        return data;
    }

    /**
     * Sets the given key to the given value overwriting any existing value with the same key.
     * <p>Make sure you call {@link #save()} on the entity that holds this data store or the store itself.
     *
     * @param key the key to store the data under. must not be null.
     * @param value the value to store. can be null.
     * @param <TData> the type of the data that is stored.
     *               This needs to be the same type when retrieving the data with {@link #get(String, Class)}.
     * @return this data store for fluent method chaining
     */
    public <TData> DataStore set(@NonNull String key, TData value) {

        data().put(key, value);
        return this;
    }

    /**
     * Gets a stored value under the given key with the given type.
     * Will return the default value if the key does not exist or the types do not match.
     * <p>The default value will not be persisted to the store.
     *
     * @param key the key the data is stored under
     * @param type the type of the data
     * @param defaultValue the value to use as a fallback when no data is found
     * @param <TData> the type of the data. This needs to be the same type the data was stored with.
     * @return the retrieved data or default value
     */
    public <TData> TData get(@NonNull String key, @NonNull Class<TData> type, TData defaultValue) {

        return get(key, type).orElse(defaultValue);
    }

    /**
     * Gets the stored value for the given key.
     * Will return an empty optional if the value is not found
     * or if it cannot be cast to the given type.
     *
     * @param key the key the data is stored under
     * @param type the type of the data
     * @param <TData> the type of the data
     * @return the data or an empty optional
     */
    @SuppressWarnings("unchecked")
    public <TData> Optional<TData> get(@NonNull String key, @NonNull Class<TData> type) {

        try {
            return Optional.ofNullable((TData) data().get(key));
        } catch (Exception e) {
            log.warning("cannot convert data entry " + key + " to "
                    + type.getCanonicalName() + ": " + e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Clears the value at the given key.
     * <p>Make sure you call {@link #save()} on the entity that holds this data store or the store itself.
     *
     * @param key the key to clear
     * @return this data store for fluent method chaining.
     */
    public DataStore clear(@NonNull String key) {

        data().remove(key);
        return this;
    }
}
