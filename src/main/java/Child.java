import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.apache.commons.lang3.RandomUtils;

import java.util.concurrent.TimeUnit;

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


        } else if (msg instanceof Master.FinishAll) {
            logger.info("Child finishing its work with result: " + stringBuffer);
            sender().tell(new Master.JobDoneMsg(stringBuffer), self());
            // we could stop current child here as well
            // getContext().stop(self());
        } else {
            unhandled(msg);
        }
    }

    //    private void sleepRandomly() {
    //        try {
    //            TimeUnit.MILLISECONDS.sleep(RandomUtils.nextInt(100,300));
    //        } catch (InterruptedException e) {
    //            //
    //        }
    //    }
}
