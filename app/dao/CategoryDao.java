package dao;

import akka.dispatch.Futures;
import db.MyConnectionPool;
import model.Category;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by yakov_000 on 19.02.2015.
 */
public class CategoryDao {

    public static Future<Category> getByName(String name) {

        final Promise<Category> promise = Futures.promise();

        MyConnectionPool.db.query("select * from category where name=$1", Arrays.asList(name),
                result -> {
                    if (result.size() > 0) {
                        final Category category = new Category(result.row(0).getLong("id"), result.row(0).getString("name"),
                                result.row(0).getString("displayName"),result.row(0).getLong("parent_id"));

                        promise.success(category);
                    } else {
                        promise.failure(new Exception("Category with specified name doesn't exist"));
                    }
                },
                promise::failure
        );

        return promise.future();
    }

    public static Future<List<Category>> listByParentId(Long parentId) {

        final Promise<List<Category>> promise = Futures.promise();

        final String query = (parentId == 0) ? "select * from category where parent_id is null" :
                "select * from category where parent_id = ".concat(parentId.toString());

        MyConnectionPool.db.query(query,
                result-> {
                    final List<Category> categories = new ArrayList<>();


                    result.forEach(row -> {
                        final Category category = new Category(row.getLong("id"), row.getString("name"),
                                row.getString("displayname"),row.getLong("parent_id"));
                        categories.add(category);
                    });

                    promise.success(categories);
                },
                promise::failure
        );

        return promise.future();
    }
}
