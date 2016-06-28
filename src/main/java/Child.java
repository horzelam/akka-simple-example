import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Child extends UntypedActor {
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private String stringBuffer = "";

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof Master.JobMessage) {
            Master.JobMessage jobMsg = (Master.JobMessage) msg;
            logger.info("Child got job with : " + jobMsg.getWord());

            // execute the job - concatenate the words
            if (stringBuffer.length() > 0) {
                stringBuffer = stringBuffer.concat(" ");
            }
            stringBuffer = stringBuffer.concat(jobMsg.getWord());

            //simulate a bit longer handling
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //
            }


        } else if (msg instanceof Master.FinishAll) {
            logger.info("Child finishing its work with result: " + stringBuffer);
            sender().tell(new Master.JobDoneMsg(stringBuffer), self());
        } else {
            unhandled(msg);
        }
    }
}
