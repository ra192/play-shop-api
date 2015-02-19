package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Status;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import akka.japi.pf.ReceiveBuilder;
import dao.CategoryDao;
import dao.ProductDao;
import dao.PropertyDao;
import dto.CategoryDto;
import dto.ListResponseWithCountDto;
import dto.ProductDto;
import dto.PropertyValueDto;
import scala.Tuple2;
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

            final Future<CategoryDto> categoryByNameFuture = CategoryDao.getByName(inputParams.getCategoryName());

            final List<Future<PropertyValueDto>> propertyValueFutures = inputParams.getPropertyValueNames().stream()
                    .map(PropertyDao::getPropertyValueByName).collect(Collectors.toList());

            final Future<Iterable<PropertyValueDto>> propertyValueFuturesSeq = Futures.sequence(propertyValueFutures, context.dispatcher());

            categoryByNameFuture.onComplete(new OnComplete<CategoryDto>() {

                @Override
                public void onComplete(Throwable failure, CategoryDto category) throws Throwable {

                    if (failure != null) {
                        sender.tell(new Status.Failure(failure), self);
                    } else {

                        propertyValueFuturesSeq.onComplete(new OnComplete<Iterable<PropertyValueDto>>() {

                            @Override
                            public void onComplete(Throwable failure, Iterable<PropertyValueDto> success) throws Throwable {

                                if (failure != null) {
                                    sender.tell(new Status.Failure(failure), self);
                                } else {
                                    final Map<Long, List<Long>> propertyValueIds = groupPropertyValueIds(success);
                                    final Future<ListResponseWithCountDto> resultFuture =
                                            ProductDao.listByCategoryIdAndPropertyValues(category.getId(), propertyValueIds, inputParams.getFirst(),
                                                    inputParams.getMax(), inputParams.getOrderProperty(), inputParams.getIsAsc())
                                                    .zip(ProductDao.countByCategoryIdAndPropertyValues(category.getId(), propertyValueIds))
                                                    .map(new Mapper<Tuple2<List<ProductDto>, Long>, ListResponseWithCountDto>() {
                                                        @Override
                                                        public ListResponseWithCountDto apply(Tuple2<List<ProductDto>, Long> parameter) {
                                                            return new ListResponseWithCountDto(parameter._1(),parameter._2());
                                                        }
                                                    }, context.dispatcher());
                                    resultFuture.onComplete(new OnComplete<ListResponseWithCountDto>() {

                                        @Override
                                        public void onComplete(Throwable failure, ListResponseWithCountDto success) throws Throwable {
                                            if(failure!=null)
                                                sender.tell(new Status.Failure(failure), self);
                                            else
                                                sender.tell(success,self);
                                        }
                                    },context.dispatcher());
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
