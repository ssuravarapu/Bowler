package org.bowlerframework.model

import collection.mutable.HashMap
import collection.TraversableLike
import com.recursivity.commons.bean.{GenericsParser, GenericTypeDefinition}

/**
 * Created by IntelliJ IDEA.
 * User: wfaler
 * Date: 22/12/2010
 * Time: 23:11
 * To change this template use File | Settings | File Templates.
 */

object AliasRegistry{
  val map = new HashMap[GenericTypeDefinition, String]

  val responseAliases = new HashMap[String, String]


  def getRequestAlias(cls: GenericTypeDefinition): String = {
    try{
      return map(cls)
    }catch{
      case e: NoSuchElementException => {
        var alias = cls.toSimpleString(true)
        alias = alias.substring(0,1).toLowerCase + alias.substring(1)
        return alias
      }
    }
  }

  def getModelAlias(param: Any): Option[String] = {
    val cls = param.asInstanceOf[AnyRef].getClass
    if(classOf[TraversableLike[_,_]].isAssignableFrom(cls)){
      val traversable = param.asInstanceOf[TraversableLike[_,_]]
      if(traversable.size > 0){
        val head = param.asInstanceOf[TraversableLike[_,_]].head
        val name = getModelAlias(head).get + "s"
        try{
          return Some(responseAliases(name))
        }catch{
          case e: NoSuchElementException => return Some(name)
        }
      }else{
        return None
      }
    }else if(classOf[java.util.Collection[_]].isAssignableFrom(cls)){
      val col = param.asInstanceOf[java.util.Collection[_]]
      if(col.size > 0){
        val head = col.iterator.next
        val name = getModelAlias(head).get + "s"
        try{
          return Some(responseAliases(name))
        }catch{
          case e: NoSuchElementException => return Some(name)
        }
      }else
        return Some("items")
    }else if(classOf[scala.collection.Map[_,_]].isAssignableFrom(cls) || classOf[java.util.Map[_,_]].isAssignableFrom(cls))
      return None
    else{
      val name = GenericsParser.parseDefinition(cls.getName).toSimpleString(true)
      try{
        return Some(responseAliases(name))
      }catch{
        case e: NoSuchElementException => return Some(name)
      }
    }
    return None
  }

  def registerModelAlias[T](alias: String)(implicit m: Manifest[T]){
    var typeString = m.toString.replace("[", "<")
    typeString = typeString.replace("]", ">")
    val typeDef = GenericsParser.parseDefinition(typeString)
    val key = getModelAliasKey(typeDef)
    if(key != None)
      responseAliases.put(key.get, alias)
  }

  def getModelAliasKey(typeDef: GenericTypeDefinition): Option[String] = {
    try{
      val cls = Class.forName(typeDef.clazz)
      if(classOf[TraversableLike[_,_]].isAssignableFrom(cls) || classOf[java.util.Collection[_]].isAssignableFrom(cls)){
        if(typeDef.genericTypes != None){
          return Some(typeDef.genericTypes.get.head.toSimpleString(true) + "s")
        }else
          return Some("items")
      }else if(typeDef.genericTypes == None){
        return Some(typeDef.toSimpleString(true))
      }else
        return None
    }catch{
      case e: ClassNotFoundException => return Some(typeDef.toSimpleString(true))
    }
  }

  def registerRequestAlias(cls: GenericTypeDefinition, alias: String) = map.put(cls, alias)

  def getTypeDefinition[T]()(implicit m: Manifest[T]): GenericTypeDefinition = {
    var typeString = m.toString.replace("[", "<")
    typeString = typeString.replace("]", ">")
    return GenericsParser.parseDefinition(typeString)
  }
}