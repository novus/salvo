package salvo.dist

import salvo.util._
import salvo.tree._

import org.eclipse.jetty.server.{ Handler, Server }
import org.eclipse.jetty.server.handler.{ DefaultHandler, HandlerList, ResourceHandler }
import org.eclipse.jetty.servlet.{ ServletHandler, ServletHolder }
import javax.servlet.http.{ HttpServlet, HttpServletRequest, HttpServletResponse }
import java.net.InetSocketAddress

trait TorrentsOps {
  dist: Dist =>

  val DefaultPort = 44663

  def server(listen: InetSocketAddress = socketAddress(DefaultPort)) = {
    val server = new Server(listen)

    val resource_handler = new ResourceHandler()
    resource_handler.setDirectoriesListed(true)
    resource_handler.setResourceBase(dir.toAbsolutePath().toString)

    val servlets = new ServletHandler()
    servlets.addServletWithMapping(new ServletHolder(new LatestVersion), "/latest-version.do")

    val handlers = new HandlerList()
    handlers.setHandlers(Array(servlets, resource_handler))

    server.setHandler(handlers)

    server
  }

  class LatestVersion extends HttpServlet {
    override def doGet(req: HttpServletRequest, res: HttpServletResponse) {
      dist.tree.history.latest() match {
        case Some(Dir(version, _)) =>
          res.setStatus(HttpServletResponse.SC_OK)
          res.getWriter.println(version.toString)
        case _ =>
          res.setStatus(HttpServletResponse.SC_NOT_FOUND)
      }
    }
  }
}
