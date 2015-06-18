
import actors.{ValidatorActor, MasterActor}
import play.api.{Application, Logger, GlobalSettings}
/**
 * Created by SB on 23/03/15.
 */
object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Logger.info("Application has started")

    //MasterActor.start
    //ValidatorActor.start //30 saniyede bir çalışacak
  }


}
