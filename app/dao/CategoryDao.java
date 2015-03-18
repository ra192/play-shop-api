package dao;

import akka.dispatch.Futures;
import com.github.pgasync.ResultSet;
import db.MyConnectionPool;
import model.Category;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import java.util.*;
import java.util.function.Consumer;

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
                                result.row(0).getString("displayName"), result.row(0).getLong("parent_id"));

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
                result -> {
                    final List<Category> categories = new ArrayList<>();


                    result.forEach(row -> {
                        final Category category = new Category(row.getLong("id"), row.getString("name"),
                                row.getString("displayname"), row.getLong("parent_id"));
                        categories.add(category);
                    });

                    promise.success(categories);
                },
                promise::failure
        );

        return promise.future();
    }

    public static Future<Long> create(Category category, Set<Long> propertyIds) {

        final Promise<Long> promise = Futures.promise();

        MyConnectionPool.db.query("select nextval('hibernate_sequence')", idResult -> {
            final Long id = idResult.row(0).getLong(0);

            String query = "INSERT INTO category(id, displayname, name, parent_id) VALUES ($1, $2, $3, $4);";

            MyConnectionPool.db.query(query, Arrays.asList(id, category.getDisplayName(), category.getName(), category.getParentId()),
                    result -> updateProperties(id, propertyIds, res -> promise.success(id), promise::failure), promise::failure);
        }, promise::failure);

        return promise.future();
    }

    public static Future<Long>update(Category category,Set<Long>propertyIds) {

        final Promise<Long> promise = Futures.promise();

        String query="UPDATE category SET displayname=$2, name=$3, parent_id=$4 WHERE id=$1";

        MyConnectionPool.db.query(query, Arrays.asList(category.getId(), category.getDisplayName(), category.getName(), category.getParentId()),
                result -> updateProperties(category.getId(), propertyIds, res -> promise.success(1L), promise::failure), promise::failure);

        return promise.future();
    }

    private static void updateProperties(Long id, Set<Long> propertyIds, Consumer<ResultSet> consumer1, Consumer<Throwable> consumer2) {

        final StringBuilder queryBuilder = new StringBuilder("delete from category_property where category_id=").append(id).append(";");

        if (!propertyIds.isEmpty()) {
            queryBuilder.append("INSERT INTO category_property(category_id, properties_id) VALUES");
            final Iterator<Long> iterator = propertyIds.iterator();
            while (iterator.hasNext()) {
                final Long propertyId = iterator.next();
                queryBuilder.append(" (").append(id).append(", ").append(propertyId).append(")");
                if (iterator.hasNext())
                    queryBuilder.append(", ");
                else
                    queryBuilder.append(";");
            }
        }

        MyConnectionPool.db.query(queryBuilder.toString(),consumer1,consumer2);
    }
}
