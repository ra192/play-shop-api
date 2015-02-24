package actors;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Status;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import dao.ProductDao;
import dto.CategoryDto;
import dto.PropertyDto;
import scala.Tuple2;
import scala.concurrent.Future;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by yakov_000 on 20.02.2015.
 */
public class CountProductPropertyValueActor extends CategoryAndPropertyValuesBaseActor {

    public CountProductPropertyValueActor() {

        super(Message.class);
    }

    @Override
    protected void onReceive(Object message, CategoryDto category, Map<Long, List<Long>> propertyValueIds,
                             ActorRef self, ActorRef sender, ActorContext context) {

        final Future<List<PropertyDto>> filterFuture =
                ProductDao.countPropertyValuesByCategoryIdAndFilter(category.getId(), null, propertyValueIds);

        final Future<Iterable<List<PropertyDto>>> additionalFiltersFuture = Futures.sequence(propertyValueIds.keySet().stream()
                .map(propertyId -> ProductDao.countPropertyValuesByCategoryIdAndFilter(category.getId(), propertyId, propertyValueIds))
                .collect(Collectors.toList()), context.dispatcher());

        filterFuture.zip(additionalFiltersFuture).onComplete(new OnComplete<Tuple2<List<PropertyDto>,
                Iterable<List<PropertyDto>>>>() {

            @Override
            public void onComplete(Throwable failure, Tuple2<List<PropertyDto>, Iterable<List<PropertyDto>>> success) throws Throwable {
                if (failure != null)
                    sender.tell(new Status.Failure(failure), self);
                else {
                    sender.tell(createResponse(success._1(), success._2()), self);
                }
            }
        }, context.dispatcher());
    }

    private Object createResponse(List<PropertyDto> properties, Iterable<List<PropertyDto>> additionalProperties) {

        return properties;
    }

    public static class Message extends BaseMessage {

        public Message(String categoryName, List<String> propertyValueNames) {
            super(categoryName, propertyValueNames);
        }
    }

    public static class Response {

    }
}
