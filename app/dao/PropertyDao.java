package dao;

import akka.dispatch.Futures;
import db.MyConnectionPool;
import model.Property;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import java.util.Arrays;

/**
 * Created by yakov_000 on 19.02.2015.
 */
public class PropertyDao {

    public static Future<Property> get(Long id) {

        final Promise<Property> promise = Futures.promise();

        String query = "select * from property where id=$1";

        MyConnectionPool.db.query(query, Arrays.asList(id), result -> {
            if (result.size() > 0)
                promise.success(new Property(result.row(0).getLong("id"), result.row(0).getString("name"), result.row(0).getString("displayName")));
            else
                promise.failure(new Exception("Property with specified name doesn't exist"));
        }, promise::failure);

        return promise.future();
    }

    public static Future<Property> getByName(String name) {

        final Promise<Property> promise = Futures.promise();

        String query = "select * from property where name=$1";

        MyConnectionPool.db.query(query, Arrays.asList(name), result -> {
            if (result.size() > 0)
                promise.success(new Property(result.row(0).getLong("id"), result.row(0).getString("name"), result.row(0).getString("displayName")));
            else
                promise.failure(new Exception("Property with specified name doesn't exist"));
        }, promise::failure);

        return promise.future();
    }

    public static Future<Long> create(Property property) {

        final Promise<Long> promise = Futures.promise();

        MyConnectionPool.db.query("select nextval('hibernate_sequence')", idRes -> {
            final Long id = idRes.row(0).getLong(0);
            String query = "INSERT INTO property(id, displayname, name) VALUES ($1, $2, $3)";
            MyConnectionPool.db.query(query, Arrays.asList(id, property.getDisplayName(), property.getName()), res -> promise.success(id), promise::failure);
        }, promise::failure);

        return promise.future();
    }

    public static Future<Long> update(Property property) {

        final Promise<Long> promise = Futures.promise();

        String query = "UPDATE property SET displayname=$1, name=$2 WHERE id=$1";
        MyConnectionPool.db.query(query, Arrays.asList(property.getId(), property.getDisplayName(), property.getName()), res -> promise.success(1L), promise::failure);

        return promise.future();
    }
}
