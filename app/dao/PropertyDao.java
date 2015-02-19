package dao;

import akka.dispatch.Futures;
import db.MyConnectionPool;
import dto.PropertyValueDto;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import java.util.Arrays;

/**
 * Created by yakov_000 on 19.02.2015.
 */
public class PropertyDao {

    public static Future<PropertyValueDto> getPropertyValueByName(String name) {

        final Promise<PropertyValueDto> promise = Futures.promise();

        MyConnectionPool.db.query("select * from property_value where name=$1", Arrays.asList(name),
                result -> {
                    if (result.size() > 0) {
                        promise.success(new PropertyValueDto(result.row(0).getLong("id"), result.row(0).getString("name"),
                                result.row(0).getString("displayName"), result.row(0).getLong("property_id")));
                    } else {
                        promise.failure(new Exception("Property value with specified name doesn't exist"));
                    }
                },
                promise::failure);

        return promise.future();
    }
}
