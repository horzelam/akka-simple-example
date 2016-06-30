package example3;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.japi.JavaPartialFunction;
import akka.util.Timeout;
import example1.Master;
import org.apache.commons.lang3.RandomUtils;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import scala.util.Try;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

public class AppWithMultipleMsgs {

    private static final FiniteDuration duration = Duration.create(10, TimeUnit.SECONDS);

    public static void main(String[] args) throws Exception {

        // starting the AKKA system
        ActorSystem system = ActorSystem.create("some_system");

        // creating 1st actor
        ActorRef master = system.actorOf(Props.create(Master.class), "master");

        // ----- 1st round
        // sending initial msg - using tell to start jobs
        master.tell(new Master.JobMessage(1, "hi"), null);
        master.tell(new Master.JobMessage(1, "there"), null);
        master.tell(new Master.JobMessage(100, "hello"), null);
        master.tell(new Master.JobMessage(100, "world"), null);

        // ask for results
        // using ASK instead of TELL - as a way to get the result out of the actor system
        Future<Object> future1 = ask(master, new Master.FinishAll(), new Timeout(duration));
        future1.onSuccess(printResultsFromMaster("RESULT : future1 - "), system.dispatcher());

        // ----- 2nd round
        for (int i = 0; i < 1000; i++) {
            master.tell(new Master.JobMessage(RandomUtils.nextInt(1, 100), "msg_" + i), null);
        }

        // ask for results
        Future<Object> future2 = ask(master, new Master.FinishAll(), new Timeout(duration));
        future2.onSuccess(printResultsFromMaster("FINAL RESULT : future2 - "), system.dispatcher());

        // lets wait for both and then SHUTDOWN...
        final List<Future<Object>> futures = Arrays.asList(future1, future2);
        Futures.sequence(futures, system.dispatcher()).andThen(handleSysShutdown(system), system.dispatcher());
    }

    private static <T> JavaPartialFunction<Try<T>, Object> handleSysShutdown(final ActorSystem system) {
        return new JavaPartialFunction<Try<T>, Object>() {

            @Override
            public Object apply(Try<T> x, boolean isCheck) throws Exception {
                System.out.println("Shutdown ...");
                system.shutdown();
                return null;
            }
        };
    }

    private static JavaPartialFunction<Object, Object> printResultsFromMaster(String round) {

        return new JavaPartialFunction<Object, Object>() {

            @Override
            public Object apply(Object result, boolean isCheck) throws Exception {
                System.out.println(round + " - all msgs received from master: " + result);
                return null;
            }
        };
    }

}
