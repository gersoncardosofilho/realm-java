/*
 * Copyright 2015 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.realm.internal;

import android.util.JsonReader;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmModel;
import io.realm.RealmObject;
import io.realm.RealmObjectSchema;
import io.realm.RealmSchema;
import io.realm.exceptions.RealmException;


/**
 * Superclass for the RealmProxyMediator class. This class contains all static methods introduced by the annotation
 * processor as part of the RealmProxy classes.
 * <p>
 * Classes extending this class act as binders between the static methods inside each RealmProxy and the code at
 * runtime. We cannot rely on using reflection as the RealmProxies are generated by the annotation processor before
 * ProGuard is run. So after ProGuard has run there is a mismatch between the name of the RealmProxy and the original
 * RealmObject class.
 */
public abstract class RealmProxyMediator {

    /**
     * Creates a object schema for the given RealmObject class.
     *
     * @param clazz the {@link RealmObject} model class to create object schema for.
     * @param realmSchema the {@link RealmSchema} to associate the object schema with.
     * @return the object schema.
     */
    public abstract RealmObjectSchema createRealmObjectSchema(Class<? extends RealmModel> clazz, RealmSchema realmSchema);

    public abstract List<OsObjectSchemaInfo> getExpectedObjectSchemaInfoList();

    /**
     * Validates the backing table in Realm for the given RealmObject class.
     *
     * @param clazz the {@link RealmObject} model class to validate.
     * @param sharedRealm the wrapper object of underlying native database to validate against.
     * @param allowExtraColumns if {@code} false, {@link io.realm.exceptions.RealmMigrationNeededException}
     * is thrown when the column count it more than expected.
     * @return the field indices map.
     */
    public abstract ColumnInfo validateTable(Class<? extends RealmModel> clazz,
            SharedRealm sharedRealm,
            boolean allowExtraColumns);

    /**
     * Returns a map of non-obfuscated object field names to their internal Realm name.
     *
     * @param clazz the {@link RealmObject} class reference.
     * @return The simple name of an RealmObject class (before it has been obfuscated).
     */
    public abstract List<String> getFieldNames(Class<? extends RealmModel> clazz);

    /**
     * Returns the name that Realm should use for all its internal tables. This is the un-obfuscated name of the
     * class.
     *
     * @param clazz the {@link RealmObject} class reference.
     * @return the simple name of an RealmObject class (before it has been obfuscated).
     * @throws java.lang.NullPointerException if null is given as argument.
     */
    public abstract String getTableName(Class<? extends RealmModel> clazz);

    /**
     * Creates a new instance of an {@link RealmObjectProxy} for the given RealmObject class.
     *
     * @param clazz the {@link RealmObject} to create {@link RealmObjectProxy} for.
     * @param acceptDefaultValue {@code true} to accept the values set in the constructor, {@code false} otherwise.
     * @param excludeFields the column names whose default value will be ignored if the {@code acceptDefaultValue}
     * is {@code true}. Only {@link io.realm.RealmModel} and {@link io.realm.RealmList}
     * column will respect this.
     * No effects if the {@code acceptDefaultValue} is {@code false}.
     * @return created {@link RealmObjectProxy} object.
     */
    public abstract <E extends RealmModel> E newInstance(Class<E> clazz,
            Object baseRealm,
            Row row,
            ColumnInfo columnInfo,
            boolean acceptDefaultValue,
            List<String> excludeFields);

    /**
     * Returns the list of RealmObject classes that can be saved in this Realm.
     *
     * @return list of class references to RealmObject classes. Empty list if no RealmObjects are supported.
     */
    public abstract Set<Class<? extends RealmModel>> getModelClasses();

    /**
     * Copies an unmanaged {@link RealmObject} or a RealmObject from another Realm to this Realm. After being copied
     * any changes to the original object will not be persisted.
     *
     * @param realm the reference to the {@link Realm} where the object will be copied.
     * @param object the object to copy properties from.
     * @param update {@code true} if object has a primary key and should try to update already existing data,
     * {@code false} otherwise.
     * @param cache the cache for mapping between unmanaged objects and their {@link RealmObjectProxy} representation.
     * @return the managed Realm object.
     */
    public abstract <E extends RealmModel> E copyOrUpdate(Realm realm, E object, boolean update, Map<RealmModel, RealmObjectProxy> cache);

    /**
     * Inserts an unmanaged RealmObject. This is generally faster than {@link #copyOrUpdate(Realm, RealmModel, boolean, Map)}
     * since it doesn't return the inserted elements, and performs minimum allocations and checks.
     * After being inserted any changes to the original object will not be persisted.
     *
     * @param realm reference to the {@link Realm} where the object will be inserted.
     * @param object {@link RealmObject} to insert.
     * @param cache the cache for mapping between unmanaged objects and their table row index for eventual reuse.
     */
    public abstract void insert(Realm realm, RealmModel object, Map<RealmModel, Long> cache);

    /**
     * Inserts or updates a RealmObject. This is generally faster than {@link #copyOrUpdate(Realm, RealmModel, boolean, Map)}
     * since it doesn't return the inserted elements, and performs minimum allocations and checks.
     * After being inserted any changes to the original object will not be persisted.
     *
     * @param realm reference to the {@link Realm} where the objecs will be inserted.
     * @param object {@link RealmObject} to insert.
     * @param cache the cache for mapping between unmanaged objects and their table row index for eventual reuse.
     */
    public abstract void insertOrUpdate(Realm realm, RealmModel object, Map<RealmModel, Long> cache);

    /**
     * Inserts or updates a RealmObject. This is generally faster than {@link #copyOrUpdate(Realm, RealmModel, boolean, Map)}
     * since it doesn't return the inserted elements, and performs minimum allocations and checks.
     * After being inserted any changes to the original objects will not be persisted.
     *
     * @param realm reference to the {@link Realm} where the objects will be inserted.
     * @param objects Collection of {@link RealmObject} to insert or update. This must not be empty.
     */
    public abstract void insertOrUpdate(Realm realm, Collection<? extends RealmModel> objects);

    /**
     * Inserts a RealmObject. This is generally faster than {@link #copyOrUpdate(Realm, RealmModel, boolean, Map)} since
     * it doesn't return the inserted elements, and performs minimum allocations and checks.
     * After being inserted any changes to the original objects will not be persisted.
     *
     * @param realm reference to the {@link Realm} where the objects will be inserted.
     * @param objects Collection of {@link RealmObject} to insert or update. This must not be empty.
     */
    public abstract void insert(Realm realm, Collection<? extends RealmModel> objects);

    /**
     * Creates or updates a {@link RealmObject} using the provided JSON data.
     *
     * @param clazz the type of {@link RealmObject}
     * @param realm the reference to {@link Realm} where to create the object.
     * @param json the JSON data
     * @param update {@code true} if Realm should try to update a existing object. This requires that the RealmObject
     * class has a @PrimaryKey.
     * @return RealmObject that has been created or updated.
     * @throws JSONException if the JSON mapping doesn't match the expected class.
     */
    public abstract <E extends RealmModel> E createOrUpdateUsingJsonObject(Class<E> clazz, Realm realm, JSONObject json, boolean update) throws JSONException;

    /**
     * Creates new {@link RealmObject} based on a JSON input stream.
     *
     * @param clazz the type of {@link RealmObject}
     * @param realm the reference to Realm where to create the object.
     * @param reader the reference to the InputStream containing the JSON data.
     * @return the created {@link RealmObject}
     * @throws IOException if an error occurs with the input stream.
     */
    public abstract <E extends RealmModel> E createUsingJsonStream(Class<E> clazz, Realm realm, JsonReader reader) throws java.io.IOException;

    /**
     * Creates a deep unmanaged copy of a RealmObject. This is a deep copy so all links will be copied as well.
     * The depth can be restricted to a maximum depth after which all links will be turned into null values instead.
     *
     * @param realmObject RealmObject to copy. It must be a valid object.
     * @param maxDepth restrict the depth of the copy to this level. The root object is depth {@code 0}.
     * @param cache cache used to make sure unmanaged objects are reused correctly.
     * @return an unmanaged copy of the given object.
     */
    public abstract <E extends RealmModel> E createDetachedCopy(E realmObject, int maxDepth, Map<RealmModel, RealmObjectProxy.CacheData<RealmModel>> cache);

    /**
     * Returns whether Realm transformer has been applied or not. Subclasses of this class are
     * created by the annotation processor and the Realm transformer will add an override of
     * this method that always return {@code true} if the transform was successful.
     *
     * @return {@code true} if Realm transformer was applied, {@code false} otherwise.
     */
    public boolean transformerApplied() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RealmProxyMediator)) {
            return false;
        }
        RealmProxyMediator other = (RealmProxyMediator) o;
        return getModelClasses().equals(other.getModelClasses());
    }

    @Override
    public int hashCode() {
        return getModelClasses().hashCode();
    }

    protected static void checkClass(Class<? extends RealmModel> clazz) {
        if (clazz == null) {
            throw new NullPointerException("A class extending RealmObject must be provided");
        }
    }

    protected static RealmException getMissingProxyClassException(Class<? extends RealmModel> clazz) {
        return new RealmException(clazz + " is not part of the schema for this Realm.");
    }
}
