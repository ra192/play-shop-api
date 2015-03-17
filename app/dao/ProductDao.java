package dao;

import akka.dispatch.Futures;
import com.github.pgasync.ResultSet;
import com.github.pgasync.Row;
import db.MyConnectionPool;
import dto.PropertyDto;
import dto.PropertyValueWithCountDto;
import model.Product;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by yakov_000 on 19.02.2015.
 */
public class ProductDao {

    public static Future<Product> getByCode(String code) {

        final Promise<Product> promise = Futures.promise();

        String query = "select * from product where code = $1";

        MyConnectionPool.db.query(query, Arrays.asList(code), result -> {
            if (result.size() > 0)
                promise.success(new Product(result.row(0).getLong("id"), result.row(0).getString("code"), result.row(0).getString("displayName"),
                        result.row(0).getBigDecimal("price").doubleValue(), result.row(0).getString("description"),
                        result.row(0).getString("imageUrl"), result.row(0).getLong("category_id")));
            else
                promise.failure(new Exception("Product with specified code doesn't exist"));
        }, promise::failure);

        return promise.future();
    }

    public static Future<List<Product>> listByCategoryIdAndPropertyValues(Long categoryId, Map<Long, List<Long>> propertyValueIds,
                                                                          Integer first, Integer max, String orderProperty, Boolean isAsc) {

        final Promise<List<Product>> promise = Futures.promise();

        final StringBuilder queryBuilder = new StringBuilder("select * from product as prod where category_id=").append(categoryId);

        buildPropertyValuesSubqueries(propertyValueIds, queryBuilder);
        queryBuilder.append(" order by ").append(orderProperty);
        if (!isAsc)
            queryBuilder.append(" desc");
        queryBuilder.append(" limit ").append(max).append(" offset ").append(first);

        MyConnectionPool.db.query(queryBuilder.toString(),
                queryRes -> {
                    List<Product> products = new ArrayList<>();
                    queryRes.forEach(row -> products.add(new Product(row.getLong("id"),
                            row.getString("code"), row.getString("displayName"),
                            row.getBigDecimal("price").doubleValue(), row.getString("description"),
                            row.getString("imageUrl"), categoryId)));
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

    public static Future<List<PropertyDto>> countPropertyValuesByCategoryIdAndFilter(Long categoryId, Long propertyId, Map<Long, List<Long>> propertyValueIds) {

        final Promise<List<PropertyDto>> promise = Futures.promise();

        final StringBuilder queryBuilder = new StringBuilder("select prop.name as prop_name, prop.displayname as prop_displayname,")
                .append(" propval.name as propval_name, propval.displayname as propval_displayname, count(*) from product as prod")
                .append(" inner join product_property_value as ppv on product_id=prod.id")
                .append(" inner join property_value as propval on propval.id=ppv.propertyvalues_id")
                .append(" inner join property as prop on prop.id=propval.property_id")
                .append(" where prod.category_id=").append(categoryId);

        if (propertyId != null) {
            queryBuilder.append(" and prop.id = ").append(propertyId);

            final Map<Long, List<Long>> propertyValueIdsFiltered = propertyValueIds.entrySet().stream()
                    .filter(ent -> !ent.getKey().equals(propertyId)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            buildPropertyValuesSubqueries(propertyValueIdsFiltered, queryBuilder);
        } else
            buildPropertyValuesSubqueries(propertyValueIds, queryBuilder);

        final List<Long> flatPropertyValueIds = propertyValueIds.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        if (!flatPropertyValueIds.isEmpty()) {
            queryBuilder.append(" and propval.id not in (");
            for (int i = 0; i < flatPropertyValueIds.size(); i++) {
                queryBuilder.append(flatPropertyValueIds.get(i));
                if (i != flatPropertyValueIds.size() - 1)
                    queryBuilder.append(",");
            }
            queryBuilder.append(")");
        }

        queryBuilder.append(" group by prop.name, prop.displayname, propval.name, propval.displayname, ppv.propertyvalues_id order by prop.displayname, propval.displayname");

        MyConnectionPool.db.query(queryBuilder.toString(),
                queryRes -> {
                    final List<PropertyDto> result = new ArrayList<>();
                    PropertyDto resultItem = null;
                    for (Row row : queryRes) {
                        if (resultItem == null || !resultItem.getName().equals(row.getString("prop_name"))) {
                            resultItem = new PropertyDto(row.getString("prop_name"), row.getString("prop_displayname"));
                            result.add(resultItem);
                        }

                        resultItem.getPropertyValues().add(new PropertyValueWithCountDto(row.getString("propval_name"),
                                row.getString("propval_displayname"), row.getLong("count")));
                    }

                    promise.success(result);
                },
                promise::failure);

        return promise.future();
    }

    public static Future<Long> create(Product product, Set<Long> propertyValueIds) {

        final Promise<Long> promise = Futures.promise();

        MyConnectionPool.db.query("select nextval('hibernate_sequence')",
                idResult -> {
                    Long id = idResult.row(0).getLong(0);

                    String query = "INSERT INTO product(id, code, description, displayname, imageurl, price, rating,category_id)" +
                            " VALUES ($1, $2, $3, $4, $5, $6, $7, $8)";

                    MyConnectionPool.db.query(query, Arrays.asList(id, product.getCode(), product.getDescription(),
                                    product.getDisplayName(), product.getImageUrl(), product.getPrice(), 0, product.getCategoryId()),
                            result -> updatePropertyValues(id, propertyValueIds, res -> promise.success(id), promise::failure),
                            promise::failure);

                },
                promise::failure);

        return promise.future();
    }

    public static Future<Long> update(Product product, Set<Long> propertyValueIds) {

        final Promise<Long> promise = Futures.promise();

        String query = "UPDATE product SET code=$2, description=$3, displayname=$4, imageurl=$5, price=$6, rating=$7, category_id=$8 WHERE id=$1;";

        MyConnectionPool.db.query(query, Arrays.asList(product.getId(), product.getCode(), product.getDescription(),
                        product.getDisplayName(), product.getImageUrl(), product.getPrice(), 0, product.getCategoryId()),
                result -> updatePropertyValues(product.getId(), propertyValueIds, res -> promise.success(1L), promise::failure),
                promise::failure);

        return promise.future();
    }

    private static void updatePropertyValues(Long productId, Set<Long> propertyValueIds, Consumer<ResultSet> consumer, Consumer<Throwable> consumer1) {

        final StringBuilder queryBuilder = new StringBuilder("delete from product_property_value where product_id = ").append(productId).append(";");

        if (!propertyValueIds.isEmpty()) {
            queryBuilder.append("INSERT INTO product_property_value(product_id, propertyvalues_id)");
            final Iterator<Long> iterator = propertyValueIds.iterator();

            while (iterator.hasNext()) {
                Long propertyValueId = iterator.next();
                queryBuilder.append(" VALUES (").append(productId).append(",").append(propertyValueId).append(")");
                if (iterator.hasNext())
                    queryBuilder.append(",");
                else
                    queryBuilder.append(";");
            }
        }

        MyConnectionPool.db.query(queryBuilder.toString(), consumer, consumer1);
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
