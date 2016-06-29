package example2;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.util.Timeout;
import scala.PartialFunction;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import scala.runtime.BoxedUnit;

import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

public class ActorWhichSchedulesMsgToSelf extends AbstractActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    private ActorRef parent;

    private int childAmount = 0;
    private int allChildCreatedAmount;

    public static void main(String[] args) throws Exception {


        // starting the AKKA system
        ActorSystem system = ActorSystem.create("some_system");


        // creating actor
        ActorRef tickActor = system.actorOf(Props.create(ActorWhichSchedulesMsgToSelf.class), "tickActor");

        // now for 2 seconds lets do the Ticks....
        TimeUnit.SECONDS.sleep(1);


        // now just finishing
        Timeout timeout = new Timeout(Duration.create(1, TimeUnit.SECONDS));
        Future<Object> future = ask(tickActor, new Finish(), timeout);
        Object result = Await.result(future, timeout.duration());

        System.out.println("[example1.App] Final result : " + result);

        system.shutdown();
    }

    @Override
    public void preStart() throws Exception {
        // lets schedule Ticks - every 1 sec
        getContext().system().scheduler().schedule(
                new FiniteDuration(1, TimeUnit.MILLISECONDS),
                new FiniteDuration(1, TimeUnit.MILLISECONDS),
                () -> self().tell(new Tick(), self()),
                getContext().system().dispatcher());
    }

    public ActorWhichSchedulesMsgToSelf() {
        receive(ReceiveBuilder
                .match(Tick.class, msg -> {
                    logger.info("Tick received - lets create a child");
                    ActorRef child = getContext().actorOf(Props.create(MyChild.class), "child_" + ++childAmount);
                    getContext().watch(child);

                })
                .match(Finish.class, msg -> {
                    allChildCreatedAmount = childAmount;

                    logger.info("Finish - now we send Hi to all : " + childAmount + " children we have");
                    getContext().actorSelection("/user/tickActor/child*").tell("hi", self());

                    logger.info(" and immediately we stop them");
                    getContext().actorSelection("/user/tickActor/child*").tell(PoisonPill.getInstance(), self());

                    parent = sender();
                    getContext().become(finishing());
                })
                .build());
    }

    // ------------------ finishing STATE ----------------------
    private PartialFunction<Object, BoxedUnit> finishing() {

        return ReceiveBuilder
                .match(Terminated.class, msg -> {
                    logger.info("example1.Child finished - we have " + --childAmount + " children.");
                    if (childAmount == 0) {
                        parent.tell("finished all " + allChildCreatedAmount +" children", self());
                        // getContext().unbecome();
                        getContext().stop(self());
                    }
                })
                .matchAny(msg -> System.out.println("in finishing STATE we ignore : " + msg))
                .build();

    }

    public static class Tick {
    }

    public static class Finish {
    }

    static class MyChild extends AbstractActor {

        private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

        public MyChild() {
            receive(ReceiveBuilder
                    .matchEquals("hi", msg -> {
                        logger.info("Hi received in " + self().path().name());
                    })
                    .build());
        }
    }
}
