package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Status;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import akka.japi.pf.ReceiveBuilder;
import dao.CategoryDao;
import dao.PropertyDao;
import db.MyConnectionPool;
import dto.CategoryDto;
import dto.ListResponseWithCountDto;
import dto.ProductDto;
import dto.PropertyValueDto;
import scala.concurrent.Future;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by yakov_000 on 09.02.2015.
 */
public class GetProductsByCategoryAndFilterActor extends AbstractActor {

    public GetProductsByCategoryAndFilterActor() {

        receive(ReceiveBuilder.match(Message.class, inputParams -> {
            final ActorRef self = self();
            final ActorRef sender = sender();
            final ActorContext context = context();

            final Future<CategoryDto> categoryByNameFuture = CategoryDao.getCategoryByName(inputParams.getCategoryName());

            final List<Future<PropertyValueDto>> propertyValueFutures = inputParams.getPropertyValueNames().stream()
                    .map(PropertyDao::getPropertyValueByName).collect(Collectors.toList());

            final Future<Iterable<PropertyValueDto>> propertyValueFuturesSeq = Futures.sequence(propertyValueFutures, context.dispatcher());

            categoryByNameFuture.onComplete(new OnComplete<CategoryDto>() {

                @Override
                public void onComplete(Throwable failure, CategoryDto categoryDto) throws Throwable {

                    if (failure != null) {
                        sender.tell(new Status.Failure(failure), self);
                    } else {

                        propertyValueFuturesSeq.onComplete(new OnComplete<Iterable<PropertyValueDto>>() {

                            @Override
                            public void onComplete(Throwable failure, Iterable<PropertyValueDto> success) throws Throwable {

                                if (failure != null) {
                                    sender.tell(new Status.Failure(failure), self);
                                } else {
                                    getProducts(categoryDto.getId(), groupPropertyValueIds(success), inputParams.getFirst(),
                                            inputParams.getMax(), inputParams.getOrderProperty(), inputParams.getIsAsc(), sender, self);
                                }
                            }
                        }, context.dispatcher());
                    }
                }
            }, context.dispatcher());
        }).build());
    }

    private Map<Long, List<Long>> groupPropertyValueIds(Iterable<PropertyValueDto> result) {

        Map<Long, List<Long>> resultMap = new HashMap<>();

        result.forEach(resultItem -> {

            List<Long> resultMapValue = resultMap.get(resultItem.getPropertyId());
            if (resultMapValue == null) {
                resultMapValue = new ArrayList<>();
                resultMap.put(resultItem.getPropertyId(), resultMapValue);
            }
            resultMapValue.add(resultItem.getId());
        });
        return resultMap;
    }

    private void getProducts(Long categoryId, Map<Long, List<Long>> propertyValueIds, Integer first, Integer max,
                             String orderProperty, Boolean isAsc, ActorRef sender, ActorRef self) {

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
                    getProductsCount(categoryId, propertyValueIds, products, sender, self);
                },
                error -> sender.tell(new Status.Failure(error), self));
    }

    private void getProductsCount(Long categoryId, Map<Long, List<Long>> propertyValueIds, List<ProductDto> products, ActorRef sender, ActorRef self) {

        final StringBuilder queryBuilder = new StringBuilder("select count(*) from product as prod where category_id=").append(categoryId);

        buildPropertyValuesSubqueries(propertyValueIds, queryBuilder);

        MyConnectionPool.db.query(queryBuilder.toString(),
                queryRes -> {
                    final Long count = queryRes.row(0).getLong(0);
                    sender.tell(new ListResponseWithCountDto(products, count), self);
                },
                error -> sender.tell(new Status.Failure(error), self));
    }

    private void buildPropertyValuesSubqueries(Map<Long, List<Long>> propertyValueIds, StringBuilder queryBuilder) {
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

    public static class Message {
        private String categoryName;
        private List<String> propertyValueNames;
        private Integer first;
        private Integer max;
        private String orderProperty;
        private Boolean isAsc;

        public Message(String categoryName, List<String> propertyValueNames, Integer first, Integer max, String orderProperty, Boolean isAsc) {
            this.categoryName = categoryName;
            this.propertyValueNames = propertyValueNames;
            this.first = first;
            this.max = max;
            this.orderProperty = orderProperty;
            this.isAsc = isAsc;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public List<String> getPropertyValueNames() {
            return propertyValueNames;
        }

        public Integer getFirst() {
            return first;
        }

        public Integer getMax() {
            return max;
        }

        public String getOrderProperty() {
            return orderProperty;
        }

        public Boolean getIsAsc() {
            return isAsc;
        }
    }
}
