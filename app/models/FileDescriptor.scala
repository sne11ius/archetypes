package models

sealed trait FileDescriptor

case object Empty extends FileDescriptor
case class Binary(href: String) extends FileDescriptor
case class Image(href: String) extends FileDescriptor
case class Text(text: String) extends FileDescriptor
case class Markdown(html: String) extends FileDescriptor
