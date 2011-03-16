package org.bowlerframework.view.squery

import org.bowlerframework.view.scalate.ClasspathTemplateResolver
import xml.{XML, NodeSeq}
import java.io.{IOException, StringReader}
import collection.mutable.HashMap
import java.util.concurrent.ConcurrentHashMap
import org.bowlerframework.{RequestResolver, RequestScope}

/**
 * Created by IntelliJ IDEA.
 * User: wfaler
 * Date: 12/03/2011
 * Time: 18:48
 * To change this template use File | Settings | File Templates.
 */

trait Component {
  def render: NodeSeq = Component.getTemplate(this.getClass, Component.localisationPreferences)
}

object Component {
  val templateCache = new ConcurrentHashMap[Class[_], HashMap[String, Option[NodeSeq]]]
  val templateResolver = new ClasspathTemplateResolver
  val types = List(".html", ".xhtml", ".xml")

  var requestResolver : RequestResolver = new RequestResolver{
    def request = RequestScope.request
  }

  private def uri(cls: Class[_]): String = "/" + cls.getName.replace(".", "/")

  def localisationPreferences: List[String] = {
    if (requestResolver.request != null && requestResolver.request.getLocales != null)
      return requestResolver.request.getLocales
    else return Nil
  }


  private def getTemplate(cls: Class[_], locales: List[String]): NodeSeq = {
    if (templateCache.get(cls) == null)
      templateCache.put(cls, new HashMap[String, Option[NodeSeq]])
    val map = templateCache.get(cls)
    try {
      if (locales == Nil) {
        val option = map("default")
        if (option == None)
          return getTemplate(cls.getSuperclass, localisationPreferences)
        else return option.get
      } else {
        val option = map(locales.head)
        if (option == None) {
          val newLocales = locales.drop(1)
          return getTemplate(cls, newLocales)
        } else return option.get
      }
    } catch {
      case e: NoSuchElementException => {
        try {
          val nodeSeq = XML.load(new StringReader(templateResolver.resolveResource(uri(cls), Component.types, {
            if (locales != Nil) List(locales.head); else Nil
          }).template)).asInstanceOf[NodeSeq]
          map.put({
            if (locales != Nil) locales.head; else "default"
          }, Some(nodeSeq))
          return nodeSeq
        } catch {
          case e: IOException => {
            map.put({
              if (locales != Nil) locales.head; else "default"
            }, None)
            if (locales != Nil) {
              val newLocales = locales.drop(1)
              getTemplate(cls, newLocales)
            } else if (cls.getSuperclass != null && classOf[Component].isAssignableFrom(cls.getSuperclass) && locales == Nil) {
              getTemplate(cls.getSuperclass, localisationPreferences)
            }
            else
              throw new IOException("Can't find any markup for Component of type " + cls.getName)
          }
        }
      }
    }

  }
}