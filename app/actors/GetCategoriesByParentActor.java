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
import dao.CategoryDao;
import dto.CategoryDto;
import scala.concurrent.Future;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yakov_000 on 29.01.2015.
 */
public class GetCategoriesByParentActor extends AbstractActor {

    final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public GetCategoriesByParentActor() {

        receive(ReceiveBuilder.match(Long.class, parentId -> {
            log.info("Recieved: ".concat(parentId.toString()));

            final ActorRef sender = sender();
            final ActorRef self = self();
            final ActorSystem system = getContext().system();

            CategoryDao.listByParentId(parentId).onComplete(new OnComplete<List<CategoryDto>>() {

                @Override
                public void onComplete(Throwable failure, List<CategoryDto> categories) throws Throwable {

                    if (failure != null) sender.tell(new Status.Failure(failure), self);

                    List<Future<List<CategoryDto>>> futures = new ArrayList<>();
                    categories.forEach(category -> {
                                final Future<List<CategoryDto>> childCategoriesFuture = CategoryDao.listByParentId(category.getId());
                                childCategoriesFuture.onComplete(new OnComplete<List<CategoryDto>>() {

                                    @Override
                                    public void onComplete(Throwable failure, List<CategoryDto> categories) throws Throwable {
                                        category.getChildren().addAll(categories);
                                    }
                                }, system.dispatcher());
                                futures.add(childCategoriesFuture);
                            }
                    );
                    Futures.sequence(futures, system.dispatcher()).onComplete(new OnComplete<Iterable<List<CategoryDto>>>() {

                        @Override
                        public void onComplete(Throwable failure, Iterable<List<CategoryDto>> success) throws Throwable {
                            sender.tell(categories, self);
                        }
                    }, system.dispatcher());
                }
            }, system.dispatcher());

        }).build());
    }
}
