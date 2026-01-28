/*
 * Copyright (c) 2026. Dick Lieber, WA9NNN
 *
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or    
 * (at your option) any later version.                                  
 *                                                                      
 * This program is distributed in the hope that it will be useful,      
 * but WITHOUT ANY WARRANTY; without even the implied warranty of       
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        
 * GNU General Public License for more details.                         
 *                                                                      
 * You should have received a copy of the GNU General Public License    
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package fdswarm

import _root_.io.github.classgraph.{ClassGraph, ClassInfo}
import com.google.inject.multibindings.Multibinder
import com.google.inject.name.Names
import com.google.inject.{Binder, Scopes, TypeLiteral}

import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag

object AutoBind {

  def bindAllImplementationsOf[T: ClassTag](
                                             binder: Binder,
                                             packagesOnly: Seq[String],
                                             named: Option[String] = None,
                                             asSingleton: Boolean = false
                                           ): Unit = {
    val traitClass = summon[ClassTag[T]].runtimeClass
    val typeLiteral =
      TypeLiteral.get(traitClass).asInstanceOf[TypeLiteral[T]]

    val scan = new ClassGraph()
      .enableClassInfo()
      .ignoreClassVisibility()
      .acceptPackages(packagesOnly*)
      .scan()

    try {
      val implInfos =
        scan.getClassesImplementing(traitClass.getName).asScala ++
          scan.getSubclasses(traitClass.getName).asScala

      val concrete = implInfos.filter(isConcrete)

      val mb: Multibinder[T] =
        named match {
          case Some(n) => Multibinder.newSetBinder(binder, typeLiteral, Names.named(n))
          case None    => Multibinder.newSetBinder(binder, typeLiteral)
        }

      concrete.foreach { ci =>
        val impl: Class[? <: T] = Class.forName(ci.getName).asInstanceOf[Class[? <: T]]
        val b = mb.addBinding().to(impl)
        if (asSingleton) b.asEagerSingleton()
      }
    } finally {
      scan.close()
    }
  }

  private def isConcrete(ci: ClassInfo): Boolean =
    !ci.isAbstract && !ci.isInterface && !ci.isAnonymousInnerClass && !ci.isSynthetic
}