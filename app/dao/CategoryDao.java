package dao;

import akka.actor.Status;
import akka.dispatch.Futures;
import db.MyConnectionPool;
import dto.CategoryDto;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import java.util.Arrays;

/**
 * Created by yakov_000 on 19.02.2015.
 */
public class CategoryDao {

    public static Future<CategoryDto> getCategoryByName(String name) {

        final Promise<CategoryDto> promise = Futures.promise();

        MyConnectionPool.db.query("select * from category where name=$1", Arrays.asList(name),
                result -> {
                    if (result.size() > 0) {
                        final CategoryDto category = new CategoryDto(result.row(0).getLong("id"),
                                result.row(0).getString("name"), result.row(0).getString("displayName"));

                        promise.success(category);
                    } else {
                        promise.failure(new Exception("Category with specified name doesn't exist"));
                    }
                },
                error -> promise.failure(error)
        );

        return promise.future();
    }
}
