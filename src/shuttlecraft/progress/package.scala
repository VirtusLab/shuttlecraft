package shuttlecraft

import com.typesafe.scalalogging.Logger

package object progress {

  implicit class ProgressLogger(val artifact: Artifact) extends AnyVal{
    def progress(msg: String) = {
      Logger(artifact.toString).info(msg)
    }
  }

  implicit class ProgressLoggerSeq(val artifacts: Seq[Artifact]) extends AnyVal{
    def progress(msg: String) = {
      artifacts.map(_.progress(msg))
    }
  }

}
