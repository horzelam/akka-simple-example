package example1;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import scala.PartialFunction;
import scala.runtime.AbstractFunction0;
import scala.runtime.BoxedUnit;

public class Master extends AbstractActorWithStash {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // ------- Fields holding internal Actor state ----------
    private ActorRef senderAwaitingResult;

    private String allResults = "";

    private int allResultsAmount;

    public Master() {
        // ------------------ msg handling in INITIAL actor state----------------------
        receive(ReceiveBuilder.match(JobMessage.class, msg -> handleJobMsg(msg))
                        .match(FinishAll.class, msg -> handleFinishAllMsg(msg))
                        .matchAny(msg -> System.out.println("any other msg in master:" + msg))
                        .build());
    }

    private void handleFinishAllMsg(FinishAll msg) {
        // one of the ways to select multiple actors:
        ActorSelection childrenSelection = context().actorSelection("/user/master/child_*");
        childrenSelection.tell(msg, self());

        // State change example:
        getContext().become(awaitingResultsFunction());
        this.senderAwaitingResult = sender();
    }

    private void handleJobMsg(JobMessage jobMsg) {

        logger.info(" new job msg: " + jobMsg);

        // one child per id
        String childName = "child_" + jobMsg.getId();

        // get existing Child r create new one:
        ActorRef childRef = getContext().child(childName).getOrElse(new AbstractFunction0<ActorRef>() {

            @Override
            public ActorRef apply() {
                ActorRef childRefCreated = createChild(childName);
                getContext().watch(childRefCreated);
                logger.info("new child created: " + childRefCreated.path());
                return childRefCreated;
            }
        });


        // propagate msg to child actor
        childRef.tell(jobMsg, self());
    }

    // ------------------ msg handling in "await results" actor state----------------------
    final PartialFunction<Object, BoxedUnit> awaitingResultsFunction() {
        return ReceiveBuilder.match(JobDoneMsg.class, msg -> handleJobDoneMsg(msg))
                        .match(Terminated.class, msg -> handleTerminatedMsg())
                        .matchAny(anyMsg -> {
                            logger.info("Putting aside msg:" + anyMsg);
                            stash();
                        })
                        .build();
    }

    private void handleTerminatedMsg() {
        logger.info("Got msg about child termination: " + sender().path());
        if (!getContext().children().nonEmpty()) {
            // all children terminated -> we can finish completely
            senderAwaitingResult.tell(allResults, self());
            allResults = "";
            unstashAll();
            getContext().unbecome();

        } else {
            logger.info("children still exists");
        }
    }

    private void handleJobDoneMsg(JobDoneMsg msg) {
        String result = sender().path().name() + ": <" + msg.getResult() + ">";
        allResultsAmount++;
        if (allResults.length() > 0) {
            allResults = allResults.concat(" " + result);
        } else {
            allResults = allResults.concat(result);

        }

        // Terminate the sender = child
        // there are also other ways : poisonPill , gracefulStop waiting for termination
        logger.info("RESULT from " + sender().path() + " is " + msg.getResult());
        getContext().stop(sender());
    }

    private ActorRef createChild(String childName) {
        return context().actorOf(Props.create(Child.class), childName);
    }

    // ---------------- MESSAGES -------------
    public static class JobDoneMsg {

        private String result;

        public JobDoneMsg(String result) {
            this.result = result;
        }

        public String getResult() {
            return result;
        }
    }

    public static class JobMessage {

        private final long id;

        private String word;

        public JobMessage(long id, String word) {
            this.id = id;
            this.word = word;
        }

        public long getId() {
            return id;
        }

        public String getWord() {
            return word;
        }

        @Override
        public String toString() {
            return id + " - " + word;
        }
    }

    public static class FinishAll {

    }

}
