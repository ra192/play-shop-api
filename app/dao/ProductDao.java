package dao;

import akka.dispatch.Futures;
import com.github.pgasync.Row;
import db.MyConnectionPool;
import dto.ProductDto;
import dto.PropertyDto;
import dto.PropertyValueWithCountDto;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import java.util.*;
import java.util.stream.Collectors;

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

    public static Future<List<PropertyDto>> countPropertyValuesByCategoryIdAndFilter(Long categoryId, Long propertyId, Map<Long, List<Long>> propertyValueIds) {

        final Promise<List<PropertyDto>> promise = Futures.promise();

        final StringBuilder queryBuilder = new StringBuilder("select prop.id as prop_id, prop.name as prop_name, prop.displayname as prop_displayname,")
                .append(" propval.id as propval_id, propval.name as propval_name, propval.displayname as propval_displayname, count(*) from product as prod")
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

        queryBuilder.append(" group by prop.id, prop.name, prop.displayname, propval.id, propval.name, propval.displayname, ppv.propertyvalues_id order by prop.displayname, propval.displayname");

        MyConnectionPool.db.query(queryBuilder.toString(),
                queryRes -> {
                    final List<PropertyDto>result=new ArrayList<>();
                    PropertyDto resultItem=null;
                    for(Row row:queryRes) {
                        if(resultItem==null||!resultItem.getId().equals(row.getLong("prop_id"))) {
                            resultItem=new PropertyDto(row.getLong("prop_id"), row.getString("prop_name"), row.getString("prop_displayname"));
                            result.add(resultItem);
                        }

                        resultItem.getPropertyValues().add(new PropertyValueWithCountDto(row.getLong("propval_id"),row.getString("propval_name"),
                                row.getString("propval_displayname"),row.getLong("prop_id"),row.getLong("count")));
                    }

                    promise.success(result);
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
