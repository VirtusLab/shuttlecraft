package shuttlecraft.http

import scalaj.http.{BaseHttp, HttpOptions}
import scala.concurrent.duration._

object PatientHttp
  extends BaseHttp(
    options = Seq(
      HttpOptions.connTimeout(5.seconds.toMillis.toInt),
      HttpOptions.readTimeout(1.minute.toMillis.toInt),
      HttpOptions.followRedirects(false)
    )
  )
