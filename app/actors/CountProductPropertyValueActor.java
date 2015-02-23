package actors;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Status;
import akka.dispatch.OnComplete;
import dao.ProductDao;
import dto.CategoryDto;
import dto.PropertyValueWithCountDto;
import scala.concurrent.Future;

import java.util.List;
import java.util.Map;

/**
 * Created by yakov_000 on 20.02.2015.
 */
public class CountProductPropertyValueActor extends CategoryAndPropertyValuesBaseActor {

    public CountProductPropertyValueActor() {

        super(Message.class);
    }

    @Override
    protected void onReceive(Object message, CategoryDto category, Map<Long, List<Long>> propertyValueIds, ActorRef self, ActorRef sender, ActorContext context) {
        final Future<Map<String, List<PropertyValueWithCountDto>>> future = ProductDao.countPropertyValuesByCategoryIdAndFilter(category.getId(), null, propertyValueIds);

        future.onComplete(new OnComplete<Map<String, List<PropertyValueWithCountDto>>>() {
            @Override
            public void onComplete(Throwable failure, Map<String, List<PropertyValueWithCountDto>> success) throws Throwable {
                if (failure != null)
                    sender.tell(new Status.Failure(failure), self);
                else
                    sender.tell(success, self);
            }
        }, context.dispatcher());

    }

    public static class Message extends BaseMessage {


        public Message(String categoryName, List<String> propertyValueNames) {
            super(categoryName, propertyValueNames);
        }
    }
}
