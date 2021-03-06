package example1;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.japi.JavaPartialFunction;
import akka.util.Timeout;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import scala.util.Try;

import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

public class App {

    private static final FiniteDuration duration = Duration.create(10, TimeUnit.SECONDS);

    public static void main(String[] args) throws Exception {

        // starting the AKKA system
        ActorSystem system = ActorSystem.create("some_system");

        // creating 1st actor
        ActorRef master = system.actorOf(Props.create(Master.class), "master");

        // sending initial msg - using tell to start jobs
        master.tell(new Master.JobMessage(1, "hi"), null);
        master.tell(new Master.JobMessage(100, "hello"), null);
        master.tell(new Master.JobMessage(100, "world"), null);
        master.tell(new Master.JobMessage(1, "there"), null);

        // using ASK instead of TELL - as a way to get the result out of the actor system
        // ----- finishing 1st round
        Future<Object> future1 = ask(master, new Master.FinishAll(), new Timeout(duration));
        future1.onSuccess(printResultsFromMaster("FINAL RESULT : future1 - "), system.dispatcher());
        future1.andThen(handleSysShutdown(system), system.dispatcher());

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
                System.out.println(round + " all msgs received from master: " + result);
                return null;
            }
        };
    }

}
