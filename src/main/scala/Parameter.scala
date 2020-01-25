
import com.amazonaws.services.simplesystemsmanagement.{AWSSimpleSystemsManagement, AWSSimpleSystemsManagementClientBuilder}
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest
import org.apache.logging.log4j.scala.Logging


class Parameter(
  val client: AWSSimpleSystemsManagement,
  val name: String,
  val isEncrypted: Boolean
) extends Logging {

  def getParameter(): String = {
    val request = new GetParameterRequest().withWithDecryption(isEncrypted)
    logger.info(s"getting parameter: $name")
    client.getParameter(request.withName(name)).getParameter.getValue
  }

}

object Parameter {
  def apply(name: String, isEncrypted: Boolean): Parameter = {
    val client = AWSSimpleSystemsManagementClientBuilder.standard().withRegion("us-east-1").build()
    new Parameter(client, name, isEncrypted)
  }
}
