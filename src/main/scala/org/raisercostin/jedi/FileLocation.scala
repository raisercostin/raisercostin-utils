package org.raisercostin.jedi

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import scala.util.Try
import org.apache.commons.io.monitor.FileAlterationObserver
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor
import org.slf4j.LoggerFactory
import rx.lang.scala.Observable
import rx.lang.scala.Subscription
import scala.util.control.NonFatal

trait FileLocation extends NavigableFileInOutLocation with FileInputLocation with FileOutputLocation { self =>
  override type Repr = self.type
  def fileFullPath: String
  def append: Boolean
  override def parentName: String = toFile.getParentFile.getAbsolutePath
  def raw = fileFullPath
  def asInput: NavigableFileInputLocation = self
  lazy val toFile: File = new File(fileFullPath)
  override def toPath: Path = Paths.get(fileFullPath)
  override def unsafeToInputStream: InputStream = new FileInputStream(toFile)
  //should not throw exception but return Try?
  def checkedChild(child: String): String = { require(!child.endsWith(" "), "Child [" + child + "] has trailing spaces"); child }
  //import org.raisercostin.util.MimeTypesUtils2
  def asFile: Repr = self
  def renamed(renamer: String => String): Try[Repr] = Try {
    val newName = renamer(baseName)
    if (newName == baseName) {
      //p rintln(s"ignore [${absolute}] to [${absolute}]")
      this
    } else {
      val dest = parent.child(withExtension2(newName, extension))
      //p rintln(s"move [${absolute}] to [${dest.absolute}]")
      FileUtils.moveFile(toFile, dest.toFile)
      dest
    }
  }

  def watchFileCreated(pollingIntervalInMillis: Long = 1000): Observable[FileAlterated] = {
    Observable.apply { obs =>
      val observer = new FileAlterationObserver(toFile);
      val monitor = new FileAlterationMonitor(pollingIntervalInMillis);
      val fileListener = new FileAlterationListenerAdaptor() {
//        override def onFileCreate(file: File) = {
//          val location = Locations.file(file)
//          try {
//            obs.onNext(FileCreated(file))
//          } catch {
//            case NonFatal(e) =>
//              obs.onError(new RuntimeException(s"Processing of [${Locations.file(file)}] failed.", e))
//          }
//        }
        /**File system observer started checking event.*/
        //override def onStart(file:FileAlterationObserver) = obs.onNext(FileChanged(file))
        override def onDirectoryCreate(file:File) = obs.onNext(DirectoryCreated(file))
        override def onDirectoryChange(file:File) = obs.onNext(DirectoryChanged(file))
        override def onDirectoryDelete(file:File) = obs.onNext(DirectoryDeleted(file))
        override def onFileCreate(file:File) = obs.onNext(FileCreated(file))
        override def onFileChange(file:File) = obs.onNext(FileChanged(file))
        override def onFileDelete(file:File) = obs.onNext(FileDeleted(file))
        /**File system observer finished checking event.*/
        //override def onStop(file:FileAlterationObserver) = obs.onNext(FileChanged(file))
    }
      observer.addListener(fileListener)
      monitor.addObserver(observer)
      monitor.start()
      Subscription { monitor.stop() }
    }
  }
  @deprecated("Use watch with observable","0.31")
  def watch(pollingIntervalInMillis: Long = 1000, listener: FileLocation => Unit): FileMonitor = {
    FileMonitor(watchFileCreated(pollingIntervalInMillis).subscribe(file => listener.apply(file.location), error => LoggerFactory.getLogger(classOf[FileLocation]).error("Watch failed.", error)))
  }

  def copyFromFolder(src:FileLocation):Repr={
    src.descendants.map { x =>
      val rel = x.extractPrefix(src).get
      val y = child(rel).mkdirOnParentIfNecessary.copyFrom(x)
      println(f"""copy ${rel.raw}%-40s $x -> $y""")
    }
    this
  }
  override def childName(child:String):String = toPath.resolve(checkedChild(child)).toFile.getAbsolutePath
  def build(path:String): Repr = FileLocation(path)

  /**This one if folder otherwise the parent*/
  def folder: Repr = {
    if(isFile)
      parent
    else
      this
  }
}

@deprecated("Use watch with observable", "0.31")
case class FileMonitor(private val subscription: Subscription) {
  def stop() = subscription.unsubscribe()
}
sealed abstract class FileAlterated {
  lazy val location: FileLocation = Locations.file(file)
  protected def file: File
}
case class FileCreated(file: File) extends FileAlterated
case class FileChanged(file: File) extends FileAlterated
case class FileDeleted(file: File) extends FileAlterated
case class DirectoryCreated(file: File) extends FileAlterated
case class DirectoryChanged(file: File) extends FileAlterated
case class DirectoryDeleted(file: File) extends FileAlterated
object FileLocation{
  def apply(fileFullPath: String, append: Boolean = false):FileLocation = FileLocationImpl(fileFullPath,append)
  def apply(path: Path):FileLocation = apply(path,false)
  def apply(path: Path, append: Boolean):FileLocation = FileLocationImpl(path.toFile.getAbsolutePath,append)
}
case class FileLocationImpl(fileFullPath: String, append: Boolean = false) extends FileLocation { self =>
  override type Repr = self.type
  override def withAppend: Repr = self.copy(append = true)
}