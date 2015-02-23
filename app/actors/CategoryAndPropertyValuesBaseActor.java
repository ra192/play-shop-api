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
import dto.CategoryDto;
import dto.PropertyValueDto;
import scala.Tuple2;
import scala.concurrent.Future;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by yakov_000 on 23.02.2015.
 */
public abstract class CategoryAndPropertyValuesBaseActor extends AbstractActor {

    public CategoryAndPropertyValuesBaseActor(Class<? extends BaseMessage> messageClass) {

        receive(ReceiveBuilder.match(messageClass, message -> {

            final ActorRef self = self();
            final ActorRef sender = sender();
            final ActorContext context = context();

            final Future<CategoryDto> categoryFuture = CategoryDao.getByName(message.getCategoryName());
            final Future<Iterable<PropertyValueDto>> propertyValuesFuture = Futures.sequence(message.getPropertyValueNames()
                    .stream().map(PropertyDao::getPropertyValueByName).collect(Collectors.toList()), context.dispatcher());

            categoryFuture.zip(propertyValuesFuture).onComplete(new OnComplete<Tuple2<CategoryDto, Iterable<PropertyValueDto>>>() {

                @Override
                public void onComplete(Throwable failure, Tuple2<CategoryDto, Iterable<PropertyValueDto>> success) throws Throwable {
                    if (failure != null)
                        sender.tell(new Status.Failure(failure), self);
                    else {
                        onReceive(message, success._1(), groupPropertyValueIds(success._2()), self, sender, context);
                    }
                }
            }, context.dispatcher());

        }).build());
    }

    protected abstract void onReceive(Object message, CategoryDto category, Map<Long, List<Long>> propertyValueIds,
                                      ActorRef self, ActorRef sender, ActorContext context);

    protected Map<Long, List<Long>> groupPropertyValueIds(Iterable<PropertyValueDto> result) {

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

    protected static class BaseMessage {
        private final String categoryName;
        private final List<String> propertyValueNames;

        public BaseMessage(String categoryName, List<String> propertyValueNames) {
            this.categoryName = categoryName;
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
