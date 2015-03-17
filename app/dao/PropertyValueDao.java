package dao;

import akka.dispatch.Futures;
import db.MyConnectionPool;
import model.PropertyValue;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import java.util.Arrays;

/**
 * Created by yakov_000 on 17.03.2015.
 */
public class PropertyValueDao {

    public static Future<PropertyValue> getByName(String name) {

        final Promise<PropertyValue> promise = Futures.promise();

        MyConnectionPool.db.query("select * from property_value where name=$1", Arrays.asList(name),
                result -> {
                    if (result.size() > 0) {
                        promise.success(new PropertyValue(result.row(0).getLong("id"), result.row(0).getString("name"),
                                result.row(0).getString("displayName"), result.row(0).getLong("property_id")));
                    } else {
                        promise.failure(new Exception("Property value with specified name doesn't exist"));
                    }
                },
                promise::failure);

        return promise.future();
    }

    public static Future<Long> create(PropertyValue propertyValue) {

        final Promise<Long> promise = Futures.promise();

        MyConnectionPool.db.query("select nextval('hibernate_sequence')", idRes -> {
            final Long id = idRes.row(0).getLong(0);
            String query = "INSERT INTO property_value(id, displayname, name, property_id) VALUES ($1, $2, $3, $4)";
            MyConnectionPool.db.query(query, Arrays.asList(id, propertyValue.getDisplayName(), propertyValue.getName(), propertyValue.getPropertyId()),
                    res -> promise.success(id), promise::failure);
        }, promise::failure);

        return promise.future();
    }

    public static Future<Long> update(PropertyValue propertyValue) {

        final Promise<Long> promise = Futures.promise();

        String query = "UPDATE property_value displayname=$2, name=$3, property_id=? WHERE id = $1";
        MyConnectionPool.db.query(query, Arrays.asList(propertyValue.getId(), propertyValue.getDisplayName(), propertyValue.getName(), propertyValue.getPropertyId()),
                res -> promise.success(1L), promise::failure);

        return promise.future();
    }
}
