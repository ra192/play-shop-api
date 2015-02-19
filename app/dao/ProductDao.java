package dao;

import akka.dispatch.Futures;
import db.MyConnectionPool;
import dto.ProductDto;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by yakov_000 on 19.02.2015.
 */
public class ProductDao {

    public static Future<List<ProductDto>> listByCategoryIdAndPropertyValues(Long categoryId, Map<Long, List<Long>> propertyValueIds,
                                                                             Integer first, Integer max, String orderProperty, Boolean isAsc) {

        final Promise<List<ProductDto>> promise = Futures.promise();

        final StringBuilder queryBuilder = new StringBuilder("select * from product as prod where category_id=").append(categoryId);

        buildPropertyValuesSubqueries(propertyValueIds, queryBuilder);
        queryBuilder.append(" order by ").append(orderProperty);
        if (!isAsc)
            queryBuilder.append(" desc");
        queryBuilder.append(" limit ").append(max).append(" offset ").append(first);

        MyConnectionPool.db.query(queryBuilder.toString(),
                queryRes -> {
                    List<ProductDto> products = new ArrayList<>();
                    queryRes.forEach(row -> products.add(new ProductDto(row.getLong("id"),
                            row.getString("code"), row.getString("displayName"),
                            row.getBigDecimal("price").doubleValue(), row.getString("description"),
                            row.getString("imageUrl"))));
                    promise.success(products);
                },
                promise::failure);

        return promise.future();
    }

    public static Future<Long> countByCategoryIdAndPropertyValues(Long categoryId, Map<Long, List<Long>> propertyValueIds) {

        final Promise<Long> promise = Futures.promise();

        final StringBuilder queryBuilder = new StringBuilder("select count(*) from product as prod where category_id=").append(categoryId);

        buildPropertyValuesSubqueries(propertyValueIds, queryBuilder);

        MyConnectionPool.db.query(queryBuilder.toString(),
                queryRes -> {
                    final Long count = queryRes.row(0).getLong(0);
                    promise.success(count);
                },
                promise::failure);

        return promise.future();
    }

    private static void buildPropertyValuesSubqueries(Map<Long, List<Long>> propertyValueIds, StringBuilder queryBuilder) {
        propertyValueIds.values().forEach(ids -> {
            queryBuilder.append(" and exists (select * from product_property_value where prod.id=product_id and propertyvalues_id in (");
            for (int i = 0; i < ids.size(); i++) {
                queryBuilder.append(ids.get(i));
                if (i != ids.size() - 1)
                    queryBuilder.append(",");
            }
            queryBuilder.append("))");
        });
    }
}
