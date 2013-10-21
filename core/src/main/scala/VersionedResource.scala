package salvo.core

import salvo.util._
import salvo.tree._
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

abstract class VersionedResource[A](tree: Tree, create: Tree => Resource[A], destroy: Resource[A] => Unit = (x: Resource[A]) => ()) {
  val id = "VersionedResource("+tree.root+")"
  val ref = new AtomicReference[Resource[A]](create(tree))
  def map[B](f: A => B): Resource[B] = ref.get().right.map(f)
  val continue = new AtomicReference(true)
  class Tailer(timeout: Long, unit: TimeUnit) extends Runnable {
    val tail = tree.history.tail()
    tail.start()
    def activate(version: Version) {
      tree.activate(version)
      destroy(ref.getAndSet(create(tree)))
    }
    def run() {
      while (continue.get()) {
        try {
          val versions = tail.poll(timeout, unit).sorted(Version.ordering.reverse)
          (versions.headOption, tree.current()) match {
            case (Some(newVersion), Some(Dir(existingVersion, _))) if Version.ordering.gt(newVersion, existingVersion) => activate(newVersion)
            case (Some(newVersion), None) => activate(newVersion)
            case _ => {}
          }
        }
        catch {
          case ie: InterruptedException => {}
        }
      }
      tail.stop()
    }
  }
  val tailer = new Thread(new Tailer(1000L, TimeUnit.SECONDS), id+"/Tailer")
  def start() {
    tailer.start()
  }
  def stop() {
    continue.set(false)
    tailer.interrupt()
  }
}
