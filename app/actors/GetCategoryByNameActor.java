package actors;

import akka.actor.ActorRef;
import akka.actor.Status;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import db.MyConnectionPool;
import dto.CategoryDto;

import java.util.Arrays;

/**
 * Created by yakov_000 on 09.02.2015.
 */
public class GetCategoryByNameActor extends UntypedActor {

    final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    @Override
    public void onReceive(Object message) throws Exception {

        if (message instanceof String) {
            String categoryName = (String) message;

            final ActorRef self = getSelf();
            final ActorRef sender = getSender();

            MyConnectionPool.db.query("select * from category where name=$1", Arrays.asList(categoryName),
                    result -> {
                        if(result.size()>0) {
                            final CategoryDto category = new CategoryDto(result.row(0).getLong("id"),
                                    result.row(0).getString("name"), result.row(0).getString("displayName"));

                            sender.tell(category, self);
                        } else {
                            sender.tell(new Status.Failure(new Exception("Category with specified name doesn't exist")),self);
                        }
                    },
                    error -> sender.tell(new Status.Failure(error),self)
            );
        } else {
            unhandled(message);
        }
    }
}
