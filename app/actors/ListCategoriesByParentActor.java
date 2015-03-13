package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Status;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import dao.CategoryDao;
import dto.CategoryDto;
import model.Category;
import scala.concurrent.Future;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yakov_000 on 29.01.2015.
 */
public class ListCategoriesByParentActor extends AbstractActor {

    final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public ListCategoriesByParentActor() {

        receive(ReceiveBuilder.match(Long.class, parentId -> {
            log.info("Recieved: ".concat(parentId.toString()));

            final ActorRef sender = sender();
            final ActorRef self = self();
            final ActorSystem system = getContext().system();

            CategoryDao.listByParentId(parentId).onComplete(new OnComplete<List<Category>>() {

                @Override
                public void onComplete(Throwable failure, List<Category> categories) throws Throwable {

                    if (failure != null)
                        sender.tell(new Status.Failure(failure), self);
                    else {
                        List<Future<Object>> futures = new ArrayList<>();
                        List<CategoryDto>result=new ArrayList<>();
                        categories.forEach(category -> {
                                    final CategoryDto resultItem = new CategoryDto(category);
                                    result.add(resultItem);

                                    final Future<Object> childCategoriesFuture = Patterns.ask(self,category.getId(),5000L);
                                    childCategoriesFuture.onComplete(new OnComplete<Object>() {

                                        @Override
                                        public void onComplete(Throwable failure, Object result) throws Throwable {
                                            if (failure != null)
                                                sender.tell(new Status.Failure(failure), self);
                                            else {
                                                resultItem.getChildren().addAll((List<CategoryDto>) result);
                                            }
                                        }
                                    }, system.dispatcher());
                                    futures.add(childCategoriesFuture);
                                }
                        );
                        Futures.sequence(futures, system.dispatcher()).onComplete(new OnComplete<Iterable<Object>>() {

                            @Override
                            public void onComplete(Throwable failure, Iterable<Object> success) throws Throwable {
                                sender.tell(result, self);
                            }
                        }, system.dispatcher());
                    }
                }

            }, system.dispatcher());

        }).build());
    }
}
