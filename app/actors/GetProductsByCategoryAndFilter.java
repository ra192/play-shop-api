package actors;

import akka.actor.*;
import akka.dispatch.Futures;
import akka.dispatch.OnSuccess;
import akka.pattern.Patterns;
import db.MyConnectionPool;
import dto.CategoryDto;
import dto.ProductDto;
import dto.PropertyValueDto;
import scala.concurrent.Future;

import java.util.*;

/**
 * Created by yakov_000 on 09.02.2015.
 */
public class GetProductsByCategoryAndFilter extends UntypedActor {

    private final ActorRef categoryByNameActorRef;
    private final ActorRef propertyValueByNameActorRef;

    public GetProductsByCategoryAndFilter() {

        categoryByNameActorRef = getContext().actorOf(Props.create(GetCategoryByNameActor.class));
        propertyValueByNameActorRef=getContext().actorOf(Props.create(GetPropertyValueByNameActor.class));
    }

    @Override
    public void onReceive(Object message) throws Exception {

        if(message instanceof Message) {
            Message inputParams= (Message) message;

            final ActorRef self = getSelf();
            final ActorRef sender = getSender();
            final UntypedActorContext context = getContext();

            final Future<Object> categoryByNameFuture = Patterns.ask(categoryByNameActorRef, inputParams.getCategoryName(), 5000);

            final List<Future<Object>>propertyValueFutures=new ArrayList<>();
            for(String propertyValueName: inputParams.getPropertyValueNames()) {
                propertyValueFutures.add(Patterns.ask(propertyValueByNameActorRef,propertyValueName,5000));
            }

            final Future<Iterable<Object>> propertyValuesSeq = Futures.sequence(propertyValueFutures, context.dispatcher());

            categoryByNameFuture.onSuccess(new OnSuccess<Object>() {

                @Override
                public void onSuccess(Object result) throws Throwable {

                    final CategoryDto categoryDto = (CategoryDto) result;

                    propertyValuesSeq.onSuccess(new OnSuccess<Iterable<Object>>(){

                        @Override
                        public void onSuccess(Iterable<Object> result) throws Throwable {

                            getProducts(categoryDto.getId(), groupPropertyValueIds(result), sender, self);
                        }
                    }, context.dispatcher());
                }
            }, context.dispatcher());
        } else {
            unhandled(message);
        }
    }

    private Map<Long, List<Long>> groupPropertyValueIds(Iterable<Object> result) {

        Map<Long,List<Long>>resultMap=new HashMap<>();

        result.forEach(resultItem -> {
            PropertyValueDto propertyValue = (PropertyValueDto) resultItem;

            List<Long> resultMapValue = resultMap.get(propertyValue.getPropertyId());
            if (resultMapValue == null) {
                resultMapValue = new ArrayList<>();
                resultMap.put(propertyValue.getPropertyId(), resultMapValue);
            }
            resultMapValue.add(propertyValue.getId());
        });
        return resultMap;
    }

    private void getProducts(Long categoryId, Map<Long, List<Long>> propertyValueIds, ActorRef sender, ActorRef self) {

        final StringBuilder queryBuilder = new StringBuilder("select * from product as prod where category_id=").append(categoryId);

        propertyValueIds.values().forEach(ids->{
            queryBuilder.append(" and exists (select * from product_property_value where prod.id=product_id and propertyvalues_id in (");
            for (int i = 0; i < ids.size(); i++) {
                queryBuilder.append(ids.get(i));
                if (i!=ids.size()-1)
                    queryBuilder.append(",");
            }
            queryBuilder.append("))");
        });

        MyConnectionPool.db.query(queryBuilder.toString(),
                queryRes -> {
                    List<ProductDto> products = new ArrayList<>();
                    queryRes.forEach(row -> products.add(new ProductDto(row.getLong("id"),
                            row.getString("code"), row.getString("displayName"),
                            row.getBigDecimal("price").doubleValue(), row.getString("description"),
                            row.getString("imageUrl"))));
                    sender.tell(products, self);
                },
                error -> sender.tell(new Status.Failure(error), self));
    }

    public static class Message {
        private String categoryName;
        private List<String> propertyValueNames;

        public Message(String name, List<String> propertyValueNames) {
            this.categoryName = name;
            this.propertyValueNames = propertyValueNames;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public List<String> getPropertyValueNames() {
            return propertyValueNames;
        }
    }
}
