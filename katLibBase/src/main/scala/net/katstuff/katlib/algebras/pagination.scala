package net.katstuff.katlib.algebras

import net.katsstuff.minejson.text.Text

/**
  * Provides a way to create and send a page to a command source.
  */
trait Pagination[F[_], CommandSource] {
  type Page

  /**
    * The different operations that can be done on a page.
    */
  val pageOperations: PageOperations[Page]

  /**
    * Sends a fully defined page.
    * @param page The page to send.
    * @param source The source to send to.
    */
  def sendPage(page: Page, source: CommandSource): F[Unit]
}
object Pagination {
  type Aux[F[_], CommandSource, Page0] = Pagination[F, CommandSource] { type Page = Page0 }
}

/**
  * Provdes access to the operations you can do on a page.
  * @tparam A The page type.
  */
trait PageOperations[A] {

  /**
    * Sets the title of the page.
    */
  def setTitle(title: Text): A

  /**
    * Sets the header of the page.
    */
  def setHeader(header: Text): A

  /**
    * Sets the footer of the page.
    */
  def setFooter(footer: Text): A

  /**
    * Sets the padding of the page.
    */
  def setPadding(padding: Text): A

  /**
    * Sets the amount of lines per page.
    */
  def setLinesPerPage(linesPerPage: Int): A

  /**
    * Sets the content of the page.
    */
  def setContent(content: Seq[Text]): A
}
